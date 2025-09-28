package org.logx.core;

/**
 * 异步引擎配置类
 * <p>
 * 定义异步引擎的各种配置参数，支持通过配置文件或环境变量进行设置。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class AsyncEngineConfig {
    // Disruptor队列配置
    private int queueCapacity = 2048;
    private int batchMaxMessages = 50;
    private int batchMaxBytes = 1024 * 1024; // 1MB
    private long flushIntervalMs = 1; // 1毫秒
    private boolean blockOnFull = false;
    private boolean multiProducer = false;
    
    // 线程池配置
    private int corePoolSize = 2;
    private int maximumPoolSize = 4;
    private int queueCapacityThreadPool = 500;
    private boolean enableCpuYield = true;
    private boolean enableMemoryProtection = true;
    
    // 其他配置
    private long maxShutdownWaitMs = 1000; // 最大关闭等待时间(毫秒)
    private String logFilePrefix = "logs/batch-"; // 日志文件前缀
    
    // 默认配置实例
    public static AsyncEngineConfig defaultConfig() {
        return new AsyncEngineConfig();
    }
    
    // Getters and Setters
    public int getQueueCapacity() {
        return queueCapacity;
    }
    
    public AsyncEngineConfig queueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
        return this;
    }
    
    public int getBatchMaxMessages() {
        return batchMaxMessages;
    }
    
    public AsyncEngineConfig batchMaxMessages(int batchMaxMessages) {
        this.batchMaxMessages = batchMaxMessages;
        return this;
    }
    
    public int getBatchMaxBytes() {
        return batchMaxBytes;
    }
    
    public AsyncEngineConfig batchMaxBytes(int batchMaxBytes) {
        this.batchMaxBytes = batchMaxBytes;
        return this;
    }
    
    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }
    
    public AsyncEngineConfig flushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
        return this;
    }
    
    public boolean isBlockOnFull() {
        return blockOnFull;
    }
    
    public AsyncEngineConfig blockOnFull(boolean blockOnFull) {
        this.blockOnFull = blockOnFull;
        return this;
    }
    
    public boolean isMultiProducer() {
        return multiProducer;
    }
    
    public AsyncEngineConfig multiProducer(boolean multiProducer) {
        this.multiProducer = multiProducer;
        return this;
    }
    
    public int getCorePoolSize() {
        return corePoolSize;
    }
    
    public AsyncEngineConfig corePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }
    
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    
    public AsyncEngineConfig maximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        return this;
    }
    
    public int getQueueCapacityThreadPool() {
        return queueCapacityThreadPool;
    }
    
    public AsyncEngineConfig queueCapacityThreadPool(int queueCapacityThreadPool) {
        this.queueCapacityThreadPool = queueCapacityThreadPool;
        return this;
    }
    
    public boolean isEnableCpuYield() {
        return enableCpuYield;
    }
    
    public AsyncEngineConfig enableCpuYield(boolean enableCpuYield) {
        this.enableCpuYield = enableCpuYield;
        return this;
    }
    
    public boolean isEnableMemoryProtection() {
        return enableMemoryProtection;
    }
    
    public AsyncEngineConfig enableMemoryProtection(boolean enableMemoryProtection) {
        this.enableMemoryProtection = enableMemoryProtection;
        return this;
    }
    
    public long getMaxShutdownWaitMs() {
        return maxShutdownWaitMs;
    }
    
    public AsyncEngineConfig maxShutdownWaitMs(long maxShutdownWaitMs) {
        this.maxShutdownWaitMs = maxShutdownWaitMs;
        return this;
    }
    
    public String getLogFilePrefix() {
        return logFilePrefix;
    }
    
    public AsyncEngineConfig logFilePrefix(String logFilePrefix) {
        this.logFilePrefix = logFilePrefix;
        return this;
    }
}