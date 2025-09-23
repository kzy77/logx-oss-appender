package org.logx.log4j;

import com.thoughtworks.xstream.XStream;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.OnlyOnceErrorHandler;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.logx.adapter.QueueFullException;
import org.logx.core.UnifiedErrorHandler;
import org.logx.log4j.Log4j1xBridge;
import org.logx.storage.s3.S3StorageConfig;
import org.logx.storage.s3.StorageBackend;

/**
 * OSS对象存储 Log4j 1.x Appender： - 支持AWS S3、阿里云OSS、腾讯云COS、MinIO、Cloudflare R2等所有S3兼容存储 - 基于AWS SDK v2构建，提供统一的对象存储接口 - 继承
 * AppenderSkeleton 提供Log4j 1.x标准接口 - 核心逻辑委托给通用适配器框架（复用log-java-producer的高性能组件） - 支持XML和Properties两种配置方式
 */
public class Log4jOSSAppender extends AppenderSkeleton {

    private Log4j1xBridge adapter;
    private UnifiedErrorHandler errorHandler;

    // S3兼容存储配置 - 必需参数
    private String endpoint;
    private String region;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;

    // 应用行为配置 - 可选参数，提供最优默认值
    private String keyPrefix = "logs/";
    private int maxQueueSize = 262144;
    private int maxBatchCount = 4096;
    private int maxBatchBytes = 4 * 1024 * 1024;
    private long flushIntervalMs = 2000L;
    private boolean dropWhenQueueFull = false;
    private boolean multiProducer = false;
    private int maxRetries = 5;
    private long baseBackoffMs = 200L;
    private long maxBackoffMs = 10000L;

    /**
     * 初始化Appender
     */
    @Override
    public void activateOptions() {
        super.activateOptions();

        // 初始化统一错误处理器
        this.errorHandler = new UnifiedErrorHandler("Log4j-OSSAppender");

        // 验证必需参数
        if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
            handleConfigurationError("accessKeyId 不能为空", ErrorCode.CONFIG_MISSING_REQUIRED_FIELD);
            return;
        }
        if (accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
            handleConfigurationError("accessKeySecret 不能为空", ErrorCode.CONFIG_MISSING_REQUIRED_FIELD);
            return;
        }
        if (bucket == null || bucket.trim().isEmpty()) {
            handleConfigurationError("bucket 不能为空", ErrorCode.CONFIG_MISSING_REQUIRED_FIELD);
            return;
        }

        try {
            // 构建S3存储配置，根据endpoint自动选择合适的配置类
            S3StorageConfig config;
            if (this.endpoint != null && this.endpoint.contains("sf-oss")) {
                config = new org.logx.storage.s3.SfOssConfig.Builder()
                    .endpoint(this.endpoint)
                    .region(this.region)
                    .accessKeyId(this.accessKeyId)
                    .accessKeySecret(this.accessKeySecret)
                    .bucket(this.bucket)
                    .build();
            } else {
                config = new org.logx.storage.s3.AwsS3Config.Builder()
                    .endpoint(this.endpoint)
                    .region(this.region)
                    .accessKeyId(this.accessKeyId)
                    .accessKeySecret(this.accessKeySecret)
                    .bucket(this.bucket)
                    .build();
            }

            this.adapter = new Log4j1xBridge(config);
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
     * 处理配置错误的统一方法
     */
    private void handleConfigurationError(String message, ErrorCode errorCode) {
        if (errorHandler != null) {
            ErrorContext context = new ErrorContext.Builder().errorCode(errorCode).type(ErrorType.CONFIGURATION_ERROR)
                    .severity(ErrorSeverity.HIGH).message(message).addContextData("component", "Log4j-OSSAppender")
                    .build();

            errorHandler.handleError(context);
        } else {
            // 如果统一错误处理器还未初始化，使用Log4j的默认错误处理器
            getErrorHandler().error(message);
        }
    }
}
