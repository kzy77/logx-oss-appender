package org.logx.core;

import org.logx.fallback.FallbackManager;
import org.logx.fallback.FallbackUploaderTask;
import org.logx.fallback.ObjectNameGenerator;
import org.logx.reliability.ShutdownHookHandler;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            fallbackManager.writeFallbackFile(data);
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