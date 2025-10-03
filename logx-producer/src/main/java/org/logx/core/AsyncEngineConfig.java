package org.logx.core;

import org.logx.config.CommonConfig;

/**
 * 异步引擎配置类
 * <p>
 * 定义异步引擎的各种配置参数，支持通过配置文件或环境变量进行设置。
 * <p>
 * <b>重要：所有默认值从 CommonConfig.Defaults 统一读取，确保配置一致性。</b>
 * <p>
 * 配置参数分组：
 * <ul>
 * <li>队列配置：queueCapacity, batchMaxMessages, batchMaxBytes, flushIntervalMs, blockOnFull, multiProducer</li>
 * <li>线程池配置：corePoolSize, maximumPoolSize, queueCapacityThreadPool, enableCpuYield, enableMemoryProtection</li>
 * <li>关闭配置：maxShutdownWaitMs</li>
 * <li>文件配置：logFilePrefix, logFileName, fallbackRetentionDays, fallbackScanIntervalSeconds</li>
 * </ul>
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class AsyncEngineConfig {

    // ==================== Disruptor队列配置 ====================

    // Disruptor环形缓冲大小（默认：8192）
    private int queueCapacity = CommonConfig.Defaults.QUEUE_CAPACITY;

    // 批处理最大消息数（默认：8192条）
    private int batchMaxMessages = CommonConfig.Defaults.MAX_BATCH_COUNT;

    // 批处理最大字节数（默认：10MB）
    private int batchMaxBytes = CommonConfig.Defaults.MAX_BATCH_BYTES;

    // 最早消息最大年龄（默认：10分钟）
    private long flushIntervalMs = CommonConfig.Defaults.MAX_MESSAGE_AGE_MS;

    // 队列满时是否阻塞（默认：false，即阻塞等待）
    private boolean blockOnFull = CommonConfig.Defaults.DROP_WHEN_QUEUE_FULL;

    // 是否支持多生产者模式（默认：false）
    private boolean multiProducer = CommonConfig.Defaults.MULTI_PRODUCER;

    // ==================== 线程池配置 ====================

    // 线程池核心线程数（默认：2）
    private int corePoolSize = CommonConfig.Defaults.CORE_POOL_SIZE;

    // 线程池最大线程数（默认：4）
    private int maximumPoolSize = CommonConfig.Defaults.MAXIMUM_POOL_SIZE;

    // 线程池队列容量（默认：500）
    private int queueCapacityThreadPool = CommonConfig.Defaults.QUEUE_CAPACITY_THREAD_POOL;

    // 是否启用CPU让出机制（默认：true）
    private boolean enableCpuYield = CommonConfig.Defaults.ENABLE_CPU_YIELD;

    // 是否启用内存保护机制（默认：true）
    private boolean enableMemoryProtection = CommonConfig.Defaults.ENABLE_MEMORY_PROTECTION;

    // ==================== 关闭配置 ====================

    // 最大关闭等待时间（默认：1秒）
    private long maxShutdownWaitMs = CommonConfig.Defaults.MAX_SHUTDOWN_WAIT_MS;

    // ==================== 文件配置 ====================

    // 日志文件路径前缀（默认：logs/）
    private String logFilePrefix = CommonConfig.Defaults.KEY_PREFIX;

    // 日志文件名（默认：applogx）
    private String logFileName = CommonConfig.Defaults.LOG_FILE_NAME;

    // 兜底文件保留天数（默认：7天）
    private int fallbackRetentionDays = CommonConfig.Defaults.FALLBACK_RETENTION_DAYS;

    // 兜底文件扫描间隔（默认：60秒）
    private int fallbackScanIntervalSeconds = CommonConfig.Defaults.FALLBACK_SCAN_INTERVAL_SECONDS;
    
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
    
    public String getLogFileName() {
        return logFileName;
    }
    
    public AsyncEngineConfig logFileName(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }
    
    public int getFallbackRetentionDays() {
        return fallbackRetentionDays;
    }
    
    public AsyncEngineConfig fallbackRetentionDays(int fallbackRetentionDays) {
        this.fallbackRetentionDays = fallbackRetentionDays;
        return this;
    }
    
    public int getFallbackScanIntervalSeconds() {
        return fallbackScanIntervalSeconds;
    }
    
    public AsyncEngineConfig fallbackScanIntervalSeconds(int fallbackScanIntervalSeconds) {
        this.fallbackScanIntervalSeconds = fallbackScanIntervalSeconds;
        return this;
    }
}