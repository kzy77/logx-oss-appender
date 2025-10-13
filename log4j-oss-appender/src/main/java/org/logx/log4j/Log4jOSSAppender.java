package org.logx.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.logx.storage.StorageConfig;
import org.logx.core.AsyncEngineConfig;
import org.logx.config.ConfigManager;

/**
 * OSS对象存储 Log4j 1.x Appender： - 支持AWS S3、阿里云OSS、腾讯云COS、MinIO、Cloudflare R2等所有S3兼容存储 - 基于AWS SDK v2构建，提供统一的对象存储接口 - 继承
 * AppenderSkeleton 提供Log4j 1.x标准接口 - 核心逻辑委托给通用适配器框架（复用logx-producer的高性能组件） - 支持XML和Properties两种配置方式
 */
public class Log4jOSSAppender extends AppenderSkeleton {

    private Log4j1xBridge adapter;

    // S3兼容存储配置 - 必需参数
    private String endpoint;
    private String region;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;

    // 应用行为配置 - 可选参数，提供最优默认值（使用CommonConfig.Defaults统一管理）
    private String keyPrefix = org.logx.config.CommonConfig.Defaults.KEY_PREFIX;
    private String ossType = org.logx.config.CommonConfig.Defaults.OSS_TYPE;
    private int maxQueueSize = org.logx.config.CommonConfig.Defaults.QUEUE_CAPACITY;
    private int maxBatchCount = org.logx.config.CommonConfig.Defaults.MAX_BATCH_COUNT;
    private int maxBatchBytes = org.logx.config.CommonConfig.Defaults.MAX_BATCH_BYTES;
    private long maxMessageAgeMs = org.logx.config.CommonConfig.Defaults.MAX_MESSAGE_AGE_MS;
    private boolean dropWhenQueueFull = org.logx.config.CommonConfig.Defaults.DROP_WHEN_QUEUE_FULL;
    private boolean multiProducer = org.logx.config.CommonConfig.Defaults.MULTI_PRODUCER;
    private int maxRetries = org.logx.config.CommonConfig.Defaults.MAX_RETRIES;
    private long baseBackoffMs = org.logx.config.CommonConfig.Defaults.BASE_BACKOFF_MS;
    private long maxBackoffMs = org.logx.config.CommonConfig.Defaults.MAX_BACKOFF_MS;

    /**
     * 初始化Appender
     */
    @Override
    public void activateOptions() {
        super.activateOptions();

        try {
            // 使用ConfigManager实现完整配置优先级：
            // JVM系统属性 > 环境变量 > 配置文件 > XML字段值 > 默认值
            ConfigManager configManager = new ConfigManager();

            // 解析所有配置，应用完整的优先级链
            String finalEndpoint = resolveStringConfig(configManager, "logx.oss.endpoint", this.endpoint);
            String finalRegion = resolveStringConfig(configManager, "logx.oss.region", this.region);
            String finalAccessKeyId = resolveStringConfig(configManager, "logx.oss.accessKeyId", this.accessKeyId);
            String finalAccessKeySecret = resolveStringConfig(configManager, "logx.oss.accessKeySecret", this.accessKeySecret);
            String finalBucket = resolveStringConfig(configManager, "logx.oss.bucket", this.bucket);
            String finalKeyPrefix = resolveStringConfig(configManager, "logx.oss.keyPrefix", this.keyPrefix);
            String finalOssType = resolveStringConfig(configManager, "logx.oss.ossType", this.ossType);

            int finalMaxQueueSize = resolveIntConfig(configManager, "logx.oss.queueCapacity", this.maxQueueSize);
            int finalMaxBatchCount = resolveIntConfig(configManager, "logx.oss.maxBatchCount", this.maxBatchCount);
            int finalMaxBatchBytes = resolveIntConfig(configManager, "logx.oss.maxBatchBytes", this.maxBatchBytes);
            long finalMaxMessageAgeMs = resolveLongConfig(configManager, "logx.oss.maxMessageAgeMs", this.maxMessageAgeMs);
            boolean finalDropWhenQueueFull = resolveBooleanConfig(configManager, "logx.oss.dropWhenQueueFull", this.dropWhenQueueFull);
            boolean finalMultiProducer = resolveBooleanConfig(configManager, "logx.oss.multiProducer", this.multiProducer);
            int finalMaxRetries = resolveIntConfig(configManager, "logx.oss.maxRetries", this.maxRetries);
            long finalBaseBackoffMs = resolveLongConfig(configManager, "logx.oss.baseBackoffMs", this.baseBackoffMs);
            long finalMaxBackoffMs = resolveLongConfig(configManager, "logx.oss.maxBackoffMs", this.maxBackoffMs);

            // 验证必需参数
            if (finalAccessKeyId == null || finalAccessKeyId.trim().isEmpty()) {
                handleConfigurationError("accessKeyId 不能为空");
                return;
            }
            if (finalAccessKeySecret == null || finalAccessKeySecret.trim().isEmpty()) {
                handleConfigurationError("accessKeySecret 不能为空");
                return;
            }
            if (finalBucket == null || finalBucket.trim().isEmpty()) {
                handleConfigurationError("bucket 不能为空");
                return;
            }

            // 构建存储配置
            StorageConfig storageConfig = new StorageConfigBuilder()
                .ossType(finalOssType != null && !finalOssType.isEmpty() ? finalOssType : org.logx.config.CommonConfig.Defaults.OSS_TYPE)
                .endpoint(finalEndpoint)
                .region(finalRegion)
                .accessKeyId(finalAccessKeyId)
                .accessKeySecret(finalAccessKeySecret)
                .bucket(finalBucket)
                .keyPrefix(finalKeyPrefix)
                .build();

            // 构建异步引擎配置
            AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig()
                .queueCapacity(finalMaxQueueSize)
                .batchMaxMessages(finalMaxBatchCount)
                .batchMaxBytes(finalMaxBatchBytes)
                .maxMessageAgeMs(finalMaxMessageAgeMs)
                .blockOnFull(finalDropWhenQueueFull) // Note: inverted logic (dropWhenQueueFull vs blockOnFull)
                .multiProducer(finalMultiProducer)
                .logFilePrefix(finalKeyPrefix)
                .logFileName("applogx") // Default log file name
                .fallbackRetentionDays(org.logx.config.CommonConfig.Defaults.FALLBACK_RETENTION_DAYS)
                .fallbackScanIntervalSeconds(org.logx.config.CommonConfig.Defaults.FALLBACK_SCAN_INTERVAL_SECONDS);

            this.adapter = new Log4j1xBridge(storageConfig, engineConfig);
            this.adapter.setLayout(layout);
            this.adapter.start();

        } catch (Exception e) {
            getErrorHandler().error("Failed to initialize Log4jOSSAppender: " + e.getMessage(), e, 1);
        }
    }

    /**
     * Log4j 1.x核心方法：处理日志事件
     */
    @Override
    protected void append(LoggingEvent event) {
        if (!isAsSevereAsThreshold(event.getLevel()) || adapter == null) {
            return;
        }

        try {
            adapter.append(event);
        } catch (Exception e) {
            getErrorHandler().error("Failed to append log event", e, 1, event);
        }
    }

    /**
     * 关闭Appender
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            if (adapter != null) {
                adapter.stop();
            }
        } catch (Exception e) {
            getErrorHandler().error("Failed to close OSSAppender", e, 1);
        }

        closed = true;
    }

    /**
     * Log4j 1.x要求实现此方法
     */
    @Override
    public boolean requiresLayout() {
        return true;
    }

    // region 配置属性的getter/setter方法（用于XML和Properties配置）

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getOssType() {
        return ossType;
    }

    public void setOssType(String ossType) {
        this.ossType = ossType;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public int getMaxBatchCount() {
        return maxBatchCount;
    }

    public void setMaxBatchCount(int maxBatchCount) {
        this.maxBatchCount = maxBatchCount;
    }

    public int getMaxBatchBytes() {
        return maxBatchBytes;
    }

    public void setMaxBatchBytes(int maxBatchBytes) {
        this.maxBatchBytes = maxBatchBytes;
    }

    public long getMaxMessageAgeMs() {
        return maxMessageAgeMs;
    }

    public void setMaxMessageAgeMs(long maxMessageAgeMs) {
        this.maxMessageAgeMs = maxMessageAgeMs;
    }

    public boolean isDropWhenQueueFull() {
        return dropWhenQueueFull;
    }

    public void setDropWhenQueueFull(boolean dropWhenQueueFull) {
        this.dropWhenQueueFull = dropWhenQueueFull;
    }

    public boolean isMultiProducer() {
        return multiProducer;
    }

    public void setMultiProducer(boolean multiProducer) {
        this.multiProducer = multiProducer;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getBaseBackoffMs() {
        return baseBackoffMs;
    }

    public void setBaseBackoffMs(long baseBackoffMs) {
        this.baseBackoffMs = baseBackoffMs;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        this.maxBackoffMs = maxBackoffMs;
    }

    // endregion

    /**
     * 处理配置错误的统一方法
     */
    private void handleConfigurationError(String message) {
        // 使用Log4j的默认错误处理器
        getErrorHandler().error(message);
    }

    /**
     * 解析字符串配置，应用完整的优先级链
     */
    private String resolveStringConfig(ConfigManager configManager, String configKey, String xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return xmlValue;
    }

    /**
     * 解析整数配置，应用完整的优先级链
     */
    private int resolveIntConfig(ConfigManager configManager, String configKey, int xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                getErrorHandler().error("Invalid integer value for " + configKey + ": " + value + ", using XML value: " + xmlValue);
                return xmlValue;
            }
        }
        return xmlValue;
    }

    /**
     * 解析长整数配置，应用完整的优先级链
     */
    private long resolveLongConfig(ConfigManager configManager, String configKey, long xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                getErrorHandler().error("Invalid long value for " + configKey + ": " + value + ", using XML value: " + xmlValue);
                return xmlValue;
            }
        }
        return xmlValue;
    }

    /**
     * 解析布尔配置，应用完整的优先级链
     */
    private boolean resolveBooleanConfig(ConfigManager configManager, String configKey, boolean xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            String trimmedValue = value.trim().toLowerCase(java.util.Locale.ENGLISH);
            return "true".equals(trimmedValue) || "yes".equals(trimmedValue) || "1".equals(trimmedValue);
        }
        return xmlValue;
    }
}