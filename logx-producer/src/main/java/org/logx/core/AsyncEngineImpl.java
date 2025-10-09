package org.logx.core;

import org.logx.core.EnhancedDisruptorBatchingQueue.LogEvent;
import org.logx.fallback.FallbackManager;
import org.logx.fallback.FallbackUploaderTask;
import org.logx.fallback.ObjectNameGenerator;
import org.logx.reliability.ShutdownHookHandler;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步处理引擎实现
 * <p>
 * 基于增强的Disruptor批处理队列实现的异步日志处理引擎。
 * 负责接收日志数据、启动和停止处理流程，并提供优雅停机支持。
 * <p>
 * 支持紧急兜底机制：当内存占用超过512MB时，直接将日志写入兜底文件，避免OOM。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class AsyncEngineImpl implements AsyncEngine, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AsyncEngineImpl.class);

    // 紧急保护阈值（字节）
    private final long emergencyMemoryThreshold;

    private final StorageService storageService;
    private final ShutdownHookHandler shutdownHandler;
    private final EnhancedDisruptorBatchingQueue batchingQueue;
    private final AsyncEngineConfig config;
    private final FallbackManager fallbackManager;
    private final ObjectNameGenerator nameGenerator;
    private ScheduledExecutorService fallbackScheduler;

    // 并行上传线程池
    private java.util.concurrent.ExecutorService uploadExecutor;

    // 队列压力监控器
    private ScheduledExecutorService queueMonitor;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    // 内存使用量监控（字节）
    private final AtomicLong currentMemoryUsage = new AtomicLong(0);
    
    /**
     * 构造异步引擎实现
     *
     * @param storageService 存储服务实现
     */
    public AsyncEngineImpl(StorageService storageService) {
        this(storageService, AsyncEngineConfig.defaultConfig());
    }
    
    /**
     * 构造异步引擎实现
     *
     * @param storageService 存储服务实现
     * @param config 异步引擎配置
     */
    public AsyncEngineImpl(StorageService storageService, AsyncEngineConfig config) {
        this.storageService = storageService;
        this.config = config;
        
        // 初始化紧急内存阈值（从配置中读取，转换为字节）
        this.emergencyMemoryThreshold = (long) config.getEmergencyMemoryThresholdMb() * 1024 * 1024;
        
        // 创建对象名生成器
        this.nameGenerator = new ObjectNameGenerator(config.getLogFileName());
        
        // 创建兜底管理器
        this.fallbackManager = new FallbackManager(config.getLogFilePrefix(), config.getLogFileName());

        // 创建增强批处理队列
        EnhancedDisruptorBatchingQueue.Config queueConfig = EnhancedDisruptorBatchingQueue.Config.defaultConfig()
            .queueCapacity(config.getQueueCapacity())
            .batchMaxMessages(config.getBatchMaxMessages())
            .batchMaxBytes(config.getBatchMaxBytes())
            .maxMessageAgeMs(config.getMaxMessageAgeMs())
            .blockOnFull(config.isBlockOnFull())
            .multiProducer(config.isMultiProducer())
            .enableCompression(true)
            .enableSharding(true)
            .maxUploadSizeMb(10);
            
        logger.debug("创建EnhancedDisruptorBatchingQueue，传递的配置参数: queueCapacity={}, batchMaxMessages={}, batchMaxBytes={}, maxMessageAgeMs={}", 
                    config.getQueueCapacity(), config.getBatchMaxMessages(), config.getBatchMaxBytes(), config.getMaxMessageAgeMs());

        this.batchingQueue = new EnhancedDisruptorBatchingQueue(queueConfig, this::onBatch, storageService);
        
        // 创建关闭钩子处理器
        this.shutdownHandler = new ShutdownHookHandler();
        
        // 注册关闭回调
        this.shutdownHandler.registerCallback(new ShutdownHookHandler.ShutdownCallback() {
            @Override
            public boolean shutdown(long timeoutSeconds) {
                try {
                    AsyncEngineImpl.this.stop(timeoutSeconds, TimeUnit.SECONDS);
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to shutdown AsyncEngine: {}", e.getMessage(), e);
                    return false;
                }
            }
            
            @Override
            public String getComponentName() {
                return "AsyncEngine";
            }
        });
        
        // 注册JVM关闭钩子
        this.shutdownHandler.registerShutdownHook();
    }
    
    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            // 已经启动了
            return;
        }

        // 启动批处理队列
        batchingQueue.start();

        // 启动兜底文件重传定时任务
        startFallbackScheduler();

        // 启动并行上传线程池
        startUploadExecutor();

        // 启动队列压力监控（如果启用了动态批处理）
        if (config.isEnableDynamicBatching()) {
            startQueuePressureMonitor();
        }

        // 注册JVM关闭钩子
        shutdownHandler.registerShutdownHook();

        logger.info("AsyncEngine started successfully with {} parallel upload threads, dynamic batching: {}",
                   config.getParallelUploadThreads(), config.isEnableDynamicBatching());
    }
    
    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        if (!stopped.compareAndSet(false, true)) {
            // 已经停止了
            logger.info("AsyncEngine已经停止，无需重复停止");
            return;
        }
        
        logger.info("开始执行JVM shutdown事件触发的上传，超时时间: {} {}", timeout, timeUnit);
        
        long timeoutMillis = timeUnit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        
        try {
            // 关闭队列压力监控器
            if (queueMonitor != null) {
                queueMonitor.shutdown();
                try {
                    if (!queueMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                        queueMonitor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    queueMonitor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // 关闭兜底任务调度器
            if (fallbackScheduler != null) {
                fallbackScheduler.shutdown();
                try {
                    if (!fallbackScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                        fallbackScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    fallbackScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // 主动处理队列中剩余的数据
            logger.info("处理队列中剩余的数据");
            EnhancedDisruptorBatchingQueue.BatchMetrics metricsBefore = batchingQueue.getMetrics();
            logger.info("处理前队列统计信息: {}", metricsBefore.toString());
            
            // 等待一小段时间让剩余数据处理完成
            Thread.sleep(100);
            
            EnhancedDisruptorBatchingQueue.BatchMetrics metricsAfter = batchingQueue.getMetrics();
            logger.info("处理后队列统计信息: {}", metricsAfter.toString());

            // 关闭批处理队列
            batchingQueue.close();

            // 关闭并行上传线程池，等待所有上传任务完成
            if (uploadExecutor != null) {
                uploadExecutor.shutdown();
                try {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long remaining = Math.max(5, timeoutMillis - elapsed);

                    logger.info("等待并行上传任务完成，剩余超时时间: {} 毫秒", remaining);
                    if (!uploadExecutor.awaitTermination(remaining, TimeUnit.MILLISECONDS)) {
                        logger.warn("上传任务未能在超时时间内完成，强制关闭");
                        uploadExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    uploadExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // 等待额外时间确保清理完成
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = timeoutMillis - elapsed;

            if (remaining > 0) {
                logger.info("等待最终清理完成，剩余超时时间: {} 毫秒", remaining);
                Thread.sleep(Math.min(remaining, Math.min(3000, config.getMaxShutdownWaitMs())));
            }
            
            // 关闭存储服务
            try {
                if (storageService != null) {
                    storageService.close();
                }
            } catch (Exception e) {
                logger.error("Error closing storage service: {}", e.getMessage());
            }

            // 统计兜底文件情况
            try {
                java.io.File fallbackDir = new java.io.File(config.getLogFilePrefix());
                if (fallbackDir.exists() && fallbackDir.isDirectory()) {
                    java.io.File[] fallbackFiles = fallbackDir.listFiles((dir, name) ->
                        name.startsWith("fallback_") && name.endsWith(".log"));
                    if (fallbackFiles != null && fallbackFiles.length > 0) {
                        logger.warn("发现 {} 个兜底文件等待上传:", fallbackFiles.length);
                        for (java.io.File file : fallbackFiles) {
                            logger.warn("兜底文件: {} (大小: {} bytes)", file.getName(), file.length());
                        }
                        logger.warn("兜底文件将由后台任务自动上传，或手动触发上传");
                    } else {
                        logger.info("没有发现兜底文件，所有日志已成功上传");
                    }
                }
            } catch (Exception e) {
                logger.error("检查兜底文件时出错: {}", e.getMessage());
            }

            logger.info("AsyncEngine stopped successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("AsyncEngine stop interrupted");
        } catch (Exception e) {
            logger.error("Error stopping AsyncEngine: {}", e.getMessage());
        }
    }
    
    @Override
    public void close() {
        stop(5, TimeUnit.SECONDS);
    }
    
    @Override
    public void put(byte[] data) {
        if (!started.get() || stopped.get()) {
            return;
        }

        if (data == null || data.length == 0) {
            return;
        }

        long currentMemory = currentMemoryUsage.get();

        // 紧急保护：内存超过配置的阈值，直接写兜底文件
        if (currentMemory > emergencyMemoryThreshold) {
            logger.warn("Emergency fallback triggered: memory usage {} MB > {} MB, writing directly to fallback file",
                    currentMemory / 1024 / 1024, emergencyMemoryThreshold / 1024 / 1024);
            
            // 格式化数据后再写入兜底文件，确保与正常上传的日志格式一致
            byte[] formattedData = formatLogDataForEmergencyFallback(data);
            fallbackManager.writeFallbackFile(formattedData);
            return;
        }

        // 正常流程：提交到批处理队列
        boolean success = batchingQueue.submit(data);
        if (success) {
            // 增加内存计数
            currentMemoryUsage.addAndGet(data.length);
        }
    }
    
    /**
     * 为紧急兜底情况格式化日志数据
     * 确保兜底文件中的数据格式与正常上传的日志一致
     * 
     * @param rawData 原始日志数据
     * @return 格式化后的日志数据
     */
    private byte[] formatLogDataForEmergencyFallback(byte[] rawData) {
        try {
            // 将原始数据转换为字符串
            String rawContent = new String(rawData, java.nio.charset.StandardCharsets.UTF_8);
            
            // 创建一个模拟的日志事件
            List<EnhancedDisruptorBatchingQueue.LogEvent> events = new ArrayList<>();
            long timestamp = System.currentTimeMillis();
            
            // 如果是二进制数据，以友好的方式显示
            if (isBinaryData(rawContent)) {
                String formattedLine = String.format("[%s] [INFO] EmergencyFallback - 紧急兜底写入，原始大小: %d 字节%n", 
                    java.time.LocalDateTime.now().toString(), rawData.length);
                events.add(new EnhancedDisruptorBatchingQueue.LogEvent(
                    formattedLine.getBytes(java.nio.charset.StandardCharsets.UTF_8), timestamp));
            } else {
                // 对于文本数据，直接包装成日志格式
                String formattedLine = String.format("[%s] [INFO] EmergencyFallback - %s%n", 
                    java.time.LocalDateTime.now().toString(), rawContent.trim());
                events.add(new EnhancedDisruptorBatchingQueue.LogEvent(
                    formattedLine.getBytes(java.nio.charset.StandardCharsets.UTF_8), timestamp));
            }
            
            // 使用与正常处理相同的序列化方法
            byte[] formattedData = serializeToPatternFormat(events);
            
            return formattedData;
        } catch (Exception e) {
            logger.warn("Failed to format emergency fallback data, using raw data", e);
            return rawData;
        }
    }
    
    /**
     * 判断是否为二进制数据
     * 
     * @param content 内容
     * @return 是否为二进制数据
     */
    private boolean isBinaryData(String content) {
        // 简单判断：如果包含大量不可打印字符，可能是二进制数据
        int printableCount = 0;
        int totalCount = Math.min(content.length(), 1000); // 只检查前1000个字符
        
        for (int i = 0; i < totalCount; i++) {
            char c = content.charAt(i);
            if (c >= 32 && c <= 126 || c == '\n' || c == '\r' || c == '\t') {
                printableCount++;
            }
        }
        
        // 如果可打印字符比例小于70%，认为是二进制数据
        return totalCount > 0 && ((double) printableCount / totalCount) < 0.7;
    }
    
    /**
     * 序列化为Pattern格式（与EnhancedDisruptorBatchingQueue中的方法保持一致）
     * 正确处理二进制数据，避免UTF-8转换导致的数据损坏
     * 
     * @param events 日志事件列表
     * @return 格式化后的字节数组
     */
    private byte[] serializeToPatternFormat(List<EnhancedDisruptorBatchingQueue.LogEvent> events) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            for (EnhancedDisruptorBatchingQueue.LogEvent event : events) {
                // 直接处理字节数组，避免不必要的字符编码转换
                byte[] payload = event.payload;
                
                // 检查payload是否以换行符结尾
                if (payload.length > 0 && payload[payload.length - 1] != '\n') {
                    // 创建新的字节数组，添加换行符
                    byte[] newPayload = new byte[payload.length + 1];
                    System.arraycopy(payload, 0, newPayload, 0, payload.length);
                    newPayload[newPayload.length - 1] = '\n';
                    baos.write(newPayload);
                } else {
                    baos.write(payload);
                }
            }
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            // 如果出现IO异常，回退到原来的实现
            org.slf4j.LoggerFactory.getLogger(AsyncEngineImpl.class)
                .warn("紧急兜底序列化过程中出现IO异常，使用回退实现: {}", e.getMessage());
            StringBuilder sb = new StringBuilder();
            for (EnhancedDisruptorBatchingQueue.LogEvent event : events) {
                String logLine = new String(event.payload, java.nio.charset.StandardCharsets.UTF_8);
                
                if (!logLine.endsWith("\n")) {
                    sb.append(logLine).append("\n");
                } else {
                    sb.append(logLine);
                }
            }
            return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    
    /**
     * 处理批处理器的批次 - 使用并行上传优化
     */
    private boolean onBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount) {
        // 使用ObjectNameGenerator生成统一的文件名
        String key = nameGenerator.generateNormalObjectName();

        logger.info("提交批次到并行上传队列 - 文件名: {}, 消息数: {}, 原始大小: {} bytes, 压缩: {}, 最终大小: {} bytes",
                   key, messageCount, originalSize, compressed, batchData.length);

        // 提交到并行上传线程池进行异步处理
        if (uploadExecutor != null && !uploadExecutor.isShutdown()) {
            uploadExecutor.submit(() -> {
                try {
                    // 异步上传到存储服务
                    storageService.putObject(key, batchData).get(30, TimeUnit.SECONDS);

                    // 上传成功后打印日志
                    logger.info("✅ 成功上传日志文件到OSS: {} (消息数: {}, 大小: {} bytes)", key, messageCount, batchData.length);

                    // 减少内存计数（使用原始大小）
                    currentMemoryUsage.addAndGet(-originalSize);

                } catch (Exception e) {
                    logger.error("并行上传失败: {} - {}", key, e.getMessage(), e);

                    // 写入兜底文件
                    if (fallbackManager.writeFallbackFile(batchData)) {
                        logger.info("批次数据已写入兜底文件: {} (原因: 上传失败)", key);
                        // 减少内存计数（写入兜底文件也算处理完成）
                        currentMemoryUsage.addAndGet(-originalSize);
                    } else {
                        // 失败也要减少内存计数，避免内存泄漏
                        currentMemoryUsage.addAndGet(-originalSize);
                    }
                }
            });

            // 立即返回true，表示任务已提交（但不等待完成）
            return true;
        } else {
            // 如果线程池不可用，回退到同步处理
            return onBatchSync(batchData, originalSize, compressed, messageCount, key);
        }
    }

    /**
     * 同步批处理（备用方案）
     */
    private boolean onBatchSync(byte[] batchData, int originalSize, boolean compressed, int messageCount, String key) {
        try {
            logger.info("使用同步模式上传批次到OSS - 文件名: {}, 消息数: {}, 原始大小: {} bytes, 压缩: {}, 最终大小: {} bytes",
                       key, messageCount, originalSize, compressed, batchData.length);

            // 同步上传到存储服务
            storageService.putObject(key, batchData).get(30, TimeUnit.SECONDS);

            // 上传成功后打印日志
            logger.info("✅ 同步上传成功: {} (消息数: {}, 大小: {} bytes)", key, messageCount, batchData.length);

            // 减少内存计数（使用原始大小）
            currentMemoryUsage.addAndGet(-originalSize);

            return true;
        } catch (Exception e) {
            logger.error("同步上传失败: {} - {}", key, e.getMessage(), e);

            // 写入兜底文件
            if (fallbackManager.writeFallbackFile(batchData)) {
                logger.info("批次数据已写入兜底文件: {} (原因: 同步上传失败)", key);
                // 减少内存计数（写入兜底文件也算处理完成）
                currentMemoryUsage.addAndGet(-originalSize);
                return true;
            }

            // 失败也要减少内存计数，避免内存泄漏
            currentMemoryUsage.addAndGet(-originalSize);
            return false;
        }
    }
    
    /**
     * 启动兜底文件重传定时任务
     */
    private void startFallbackScheduler() {
        fallbackScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fallback-uploader");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        fallbackScheduler.scheduleWithFixedDelay(
            new FallbackUploaderTask(storageService, config.getLogFilePrefix(), config.getLogFileName(), config.getFallbackRetentionDays()),
            1, config.getFallbackScanIntervalSeconds(), TimeUnit.SECONDS
        );
    }

    /**
     * 启动并行上传线程池
     */
    private void startUploadExecutor() {
        int threads = config.getParallelUploadThreads();
        this.uploadExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "parallel-uploader-" + System.currentTimeMillis());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY); // 使用正常优先级以提高响应性
            return t;
        });

        logger.info("启动并行上传线程池，线程数: {}", threads);
    }

    /**
     * 启动队列压力监控器
     */
    private void startQueuePressureMonitor() {
        queueMonitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "queue-pressure-monitor");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        queueMonitor.scheduleWithFixedDelay(
            this::monitorQueuePressure,
            config.getQueuePressureMonitorIntervalMs(),
            config.getQueuePressureMonitorIntervalMs(),
            TimeUnit.MILLISECONDS
        );

        logger.info("启动队列压力监控器，监控间隔: {} ms", config.getQueuePressureMonitorIntervalMs());
    }

    /**
     * 监控队列压力并动态调整批处理参数
     */
    private void monitorQueuePressure() {
        try {
            // 获取队列使用情况
            String queueInfo = batchingQueue.getQueueStatusInfo();

            // 从队列信息解析使用率（简化实现）
            double usageRatio = estimateQueueUsage(queueInfo);

            if (usageRatio > config.getHighPressureThreshold()) {
                // 高压力：缩小批处理大小，提高处理频率
                adjustBatchingForHighPressure();
                logger.debug("检测到高队列压力: {:.1f}%, 启用快速批处理模式", usageRatio * 100);
            } else if (usageRatio < config.getLowPressureThreshold()) {
                // 低压力：恢复正常批处理大小
                adjustBatchingForLowPressure();
                logger.debug("队列压力正常: {:.1f}%, 使用标准批处理模式", usageRatio * 100);
            }
        } catch (Exception e) {
            logger.warn("队列压力监控异常: {}", e.getMessage());
        }
    }

    /**
     * 估算队列使用率（基于队列信息字符串）
     */
    private double estimateQueueUsage(String queueInfo) {
        try {
            // 解析类似 "队列容量：65536，已占用：1234，剩余容量：64302" 的字符串
            if (queueInfo.contains("剩余容量：") && queueInfo.contains("队列容量：")) {
                String[] parts = queueInfo.split("，");
                long totalCapacity = 0;
                long remainingCapacity = 0;

                for (String part : parts) {
                    if (part.contains("队列容量：")) {
                        totalCapacity = Long.parseLong(part.split("：")[1]);
                    } else if (part.contains("剩余容量：")) {
                        remainingCapacity = Long.parseLong(part.split("：")[1]);
                    }
                }

                if (totalCapacity > 0) {
                    return 1.0 - ((double) remainingCapacity / totalCapacity);
                }
            }
        } catch (Exception e) {
            logger.warn("解析队列使用率失败: {}", e.getMessage());
        }

        // 默认返回低压力状态
        return 0.2;
    }

    /**
     * 高压力时的批处理调整
     */
    private void adjustBatchingForHighPressure() {
        // 可以通过修改queue的配置来调整批处理行为
        // 这里先记录日志，具体实现可以后续优化
        logger.debug("应用高压力批处理策略：减小批次大小，提高处理频率");
    }

    /**
     * 低压力时的批处理调整
     */
    private void adjustBatchingForLowPressure() {
        // 恢复正常的批处理参数
        logger.debug("应用低压力批处理策略：使用标准批次大小");
    }
}