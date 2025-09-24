package org.logx.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import org.logx.storage.StorageConfig;
import org.logx.logback.StorageConfigBuilder;

/**
 * S3兼容对象存储 Logback Appender： - 支持AWS S3、阿里云OSS、腾讯云COS、MinIO、Cloudflare R2等所有S3兼容存储 - 基于AWS SDK v2构建，提供统一的对象存储接口 - 继承
 * AppenderBase 避免线程同步开销 - 依赖 Encoder 将 ILoggingEvent 序列化为字符串 - 核心逻辑委托给
 * 通用适配器框架（复用log-java-producer的高性能组件）
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

    // 应用行为配置 - 可选参数，提供最优默认值
    private String keyPrefix = "logs/";
    private String backendType; // 新增：后端类型配置
    private int maxQueueSize = 65536; // 64K - 必须是2的幂
    private int maxBatchCount = 1000;
    private int maxBatchBytes = 4 * 1024 * 1024;
    private long flushIntervalMs = 2000L;
    private boolean dropWhenQueueFull = false;
    private boolean multiProducer = false;
    private int maxRetries = 5;
    private long baseBackoffMs = 200L;
    private long maxBackoffMs = 10000L;

    private LogbackBridge adapter;

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for the appender named \"" + name + "\"");
            return;
        }
        try {
            // 验证必需参数
            if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
                addError("accessKeyId must be set");
                return;
            }
            if (accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
                addError("accessKeySecret must be set");
                return;
            }
            if (bucket == null || bucket.trim().isEmpty()) {
                addError("bucket must be set");
                return;
            }

            // 构建存储配置
            StorageConfig config = new StorageConfigBuilder()
                .backendType(this.backendType)
                .endpoint(this.endpoint)
                .region(this.region)
                .accessKeyId(this.accessKeyId)
                .accessKeySecret(this.accessKeySecret)
                .bucket(this.bucket)
                .keyPrefix(this.keyPrefix)
                .build();

            this.adapter = new LogbackBridge(config);
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

    public String getBackendType() {
        return backendType;
    }

    public void setBackendType(String backendType) {
        this.backendType = backendType;
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

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
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
        // 使用Logback默认错误处理
        addError(message, throwable);
    }
}
