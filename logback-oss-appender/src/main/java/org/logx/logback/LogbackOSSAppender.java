package org.logx.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import org.logx.storage.StorageConfig;
import org.logx.core.AsyncEngineConfig;
import org.logx.config.AppenderConfigResolver;

/**
 * S3兼容对象存储 Logback Appender： - 支持AWS S3、阿里云OSS、腾讯云COS、MinIO、Cloudflare R2等所有S3兼容存储 - 基于AWS SDK v2构建，提供统一的对象存储接口 - 继承
 * AppenderBase 避免线程同步开销 - 依赖 Encoder 将 ILoggingEvent 序列化为字符串 - 核心逻辑委托给
 * 通用适配器框架（复用logx-producer的高性能组件）
 */
public final class LogbackOSSAppender extends AppenderBase<ILoggingEvent> {

    // Logback必需
    private Encoder<ILoggingEvent> encoder;

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

    private LogbackBridge adapter;

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for the appender named \"" + name + "\"");
            return;
        }
        try {
            String finalEndpoint = AppenderConfigResolver.resolveStringConfig("logx.oss.endpoint", this.endpoint);
            String finalRegion = AppenderConfigResolver.resolveStringConfig("logx.oss.region", this.region);
            String finalAccessKeyId = AppenderConfigResolver.resolveStringConfig("logx.oss.accessKeyId", this.accessKeyId);
            String finalAccessKeySecret = AppenderConfigResolver.resolveStringConfig("logx.oss.accessKeySecret", this.accessKeySecret);
            String finalBucket = AppenderConfigResolver.resolveStringConfig("logx.oss.bucket", this.bucket);
            String finalKeyPrefix = AppenderConfigResolver.resolveStringConfig("logx.oss.keyPrefix", this.keyPrefix);
            String finalOssType = AppenderConfigResolver.resolveStringConfig("logx.oss.ossType", this.ossType);

            int finalMaxQueueSize = AppenderConfigResolver.resolveIntConfig("logx.oss.queueCapacity", this.maxQueueSize);
            int finalMaxBatchCount = AppenderConfigResolver.resolveIntConfig("logx.oss.maxBatchCount", this.maxBatchCount);
            int finalMaxBatchBytes = AppenderConfigResolver.resolveIntConfig("logx.oss.maxBatchBytes", this.maxBatchBytes);
            long finalMaxMessageAgeMs = AppenderConfigResolver.resolveLongConfig("logx.oss.maxMessageAgeMs", this.maxMessageAgeMs);
            boolean finalDropWhenQueueFull = AppenderConfigResolver.resolveBooleanConfig("logx.oss.dropWhenQueueFull", this.dropWhenQueueFull);
            boolean finalMultiProducer = AppenderConfigResolver.resolveBooleanConfig("logx.oss.multiProducer", this.multiProducer);
            int finalMaxRetries = AppenderConfigResolver.resolveIntConfig("logx.oss.maxRetries", this.maxRetries);
            long finalBaseBackoffMs = AppenderConfigResolver.resolveLongConfig("logx.oss.baseBackoffMs", this.baseBackoffMs);
            long finalMaxBackoffMs = AppenderConfigResolver.resolveLongConfig("logx.oss.maxBackoffMs", this.maxBackoffMs);

            // 验证必需参数
            if (finalAccessKeyId == null || finalAccessKeyId.trim().isEmpty()) {
                addError("accessKeyId must be set");
                return;
            }
            if (finalAccessKeySecret == null || finalAccessKeySecret.trim().isEmpty()) {
                addError("accessKeySecret must be set");
                return;
            }
            if (finalBucket == null || finalBucket.trim().isEmpty()) {
                addError("bucket must be set");
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

            this.adapter = new LogbackBridge(storageConfig, engineConfig);
            this.adapter.setEncoder(encoder);
            this.adapter.start();

            super.start();
        } catch (Exception e) {
            addError("Failed to start LogbackOSSAppender", e);
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted() || adapter == null) {
            return;
        }
        try {
            adapter.append(eventObject);
        } catch (Exception e) {
            handleAppendError("Failed to encode and send log event", e);
        }
    }

    @Override
    public void stop() {
        if (adapter != null) {
            try {
                adapter.stop();
            } catch (Exception e) {
                addError("Failed to gracefully close adapter", e);
            }
        }
        super.stop();
    }

    // region setters for logback config
    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

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
     * 统一错误处理方法
     */
    private void handleAppendError(String message, Throwable throwable) {
        addError(message, throwable);
    }
}