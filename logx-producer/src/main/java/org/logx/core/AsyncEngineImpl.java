package org.logx.core;

import org.logx.fallback.FallbackManager;
import org.logx.fallback.FallbackUploaderTask;
import org.logx.reliability.ShutdownHookHandler;
import org.logx.storage.StorageService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步处理引擎实现
 * <p>
 * 基于Disruptor高性能队列和BatchProcessor实现的异步日志处理引擎。
 * 负责接收日志数据、启动和停止处理流程，并提供优雅停机支持。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class AsyncEngineImpl implements AsyncEngine, AutoCloseable {
    
    private final StorageService storageService;
    private final ShutdownHookHandler shutdownHandler;
    private final BatchProcessor batchProcessor;
    private final AsyncEngineConfig config;
    private final FallbackManager fallbackManager;
    private ScheduledExecutorService fallbackScheduler;
    
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    
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
        
        // 创建兜底管理器
        this.fallbackManager = new FallbackManager(config.getFallbackPath(), "application");
        
        // 创建批处理器
        BatchProcessor.Config batchConfig = new BatchProcessor.Config()
            .batchSize(config.getBatchMaxMessages())
            .batchSizeBytes(config.getBatchMaxBytes())
            .flushIntervalMs(config.getFlushIntervalMs())
            .enableCompression(true)
            .enableAdaptiveSize(true)
            .enableSharding(true);
        
        this.batchProcessor = new BatchProcessor(batchConfig, this::onBatch, storageService);
        
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
            return; // 已经启动了
        }
        
        // 启动批处理器
        batchProcessor.start();
        
        // 启动兜底文件重传定时任务
        startFallbackScheduler();
        
        // 注册JVM关闭钩子
        shutdownHandler.registerShutdownHook();
        
        System.out.println("AsyncEngine started successfully");
    }
    
    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        if (!stopped.compareAndSet(false, true)) {
            return; // 已经停止了
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
            
            // 关闭批处理器
            batchProcessor.close();
            
            // 等待所有任务完成
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = timeoutMillis - elapsed;
            
            if (remaining > 0) {
                Thread.sleep(Math.min(remaining, config.getMaxShutdownWaitMs())); // 使用配置的最大等待时间
            }
            
            // 关闭存储服务
            try {
                if (storageService != null) {
                    storageService.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing storage service: " + e.getMessage());
            }
            
            System.out.println("AsyncEngine stopped successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("AsyncEngine stop interrupted");
        } catch (Exception e) {
            System.err.println("Error stopping AsyncEngine: " + e.getMessage());
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
        
        // 提交到批处理器
        batchProcessor.submit(data);
    }
    
    /**
     * 处理批处理器的批次
     */
    private boolean onBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount) {
        try {
            // 生成唯一的键名
            String key = config.getLogFilePrefix() + System.currentTimeMillis() + "-" + System.nanoTime() + ".log";
            
            // 异步上传到存储服务
            storageService.putObject(key, batchData).get(30, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to process batch: " + e.getMessage());
            e.printStackTrace();
            
            // 写入兜底文件
            if (fallbackManager.writeFallbackFile(batchData)) {
                System.out.println("Batch data written to fallback file due to upload failure");
                return true; // 认为处理成功，因为数据已保存到兜底文件
            }
            
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
            new FallbackUploaderTask(storageService, config.getFallbackPath(), "application", config.getFallbackRetentionDays()),
            1, config.getFallbackScanIntervalSeconds(), TimeUnit.SECONDS
        );
    }
}