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

    // 紧急保护阈值：512MB
    private static final long EMERGENCY_MEMORY_THRESHOLD = 512L * 1024 * 1024;

    private final StorageService storageService;
    private final ShutdownHookHandler shutdownHandler;
    private final EnhancedDisruptorBatchingQueue batchingQueue;
    private final AsyncEngineConfig config;
    private final FallbackManager fallbackManager;
    private final ObjectNameGenerator nameGenerator;
    private ScheduledExecutorService fallbackScheduler;

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
            .compressionThreshold(1024)
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
                    System.err.println("Failed to shutdown AsyncEngine: " + e.getMessage());
                    return false;
                }
            }
            
            @Override
            public String getComponentName() {
                return "AsyncEngine";
            }
        });
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

        // 注册JVM关闭钩子
        shutdownHandler.registerShutdownHook();

        logger.info("AsyncEngine started successfully");
    }
    
    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        if (!stopped.compareAndSet(false, true)) {
            // 已经停止了
            return;
        }
        
        long timeoutMillis = timeUnit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        
        try {
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

            // 关闭批处理队列
            batchingQueue.close();
            
            // 等待所有任务完成
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = timeoutMillis - elapsed;
            
            if (remaining > 0) {
                // 使用配置的最大等待时间
                Thread.sleep(Math.min(remaining, config.getMaxShutdownWaitMs()));
            }
            
            // 关闭存储服务
            try {
                if (storageService != null) {
                    storageService.close();
                }
            } catch (Exception e) {
                logger.error("Error closing storage service: {}", e.getMessage());
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

        // 紧急保护：内存超过512MB，直接写兜底文件
        if (currentMemory > EMERGENCY_MEMORY_THRESHOLD) {
            logger.warn("Emergency fallback triggered: memory usage {} MB > 512 MB, writing directly to fallback file",
                    currentMemory / 1024 / 1024);
            
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
     * 处理批处理器的批次
     */
    private boolean onBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount) {
        try {
            // 使用ObjectNameGenerator生成统一的文件名
            String key = nameGenerator.generateNormalObjectName();

            // 异步上传到存储服务
            storageService.putObject(key, batchData).get(30, TimeUnit.SECONDS);

            // 上传成功后打印日志
            logger.info("Successfully uploaded log file: {}", key);

            // 减少内存计数（使用原始大小）
            currentMemoryUsage.addAndGet(-originalSize);

            return true;
        } catch (Exception e) {
            logger.error("Failed to process batch: {}", e.getMessage(), e);

            // 写入兜底文件
            if (fallbackManager.writeFallbackFile(batchData)) {
                logger.info("Batch data written to fallback file due to upload failure");
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
}