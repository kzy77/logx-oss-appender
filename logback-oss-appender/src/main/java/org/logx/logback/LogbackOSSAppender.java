package org.logx.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import org.logx.logback.LogbackBridge;
import org.logx.storage.s3.S3StorageConfig;
import org.logx.core.AsyncEngine;
import org.logx.exception.StorageException;
import org.logx.storage.s3.S3StorageFactory;
import org.logx.storage.StorageBackend;

/**
 * S3兼容对象存储 Logback Appender： - 支持AWS S3、阿里云OSS、腾讯云COS、MinIO、Cloudflare R2等所有S3兼容存储 - 基于AWS SDK v2构建，提供统一的对象存储接口 - 继承
 * UnsynchronizedAppenderBase 避免线程同步开销 - 依赖 Encoder 将 ILoggingEvent 序列化为字符串 - 核心逻辑委托给
 * 通用适配器框架（复用log-java-producer的高性能组件）
 */
public final class LogbackOSSAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

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
    private UnifiedErrorHandler errorHandler;

    @Override
    public void start() {
        // 初始化统一错误处理器
        this.errorHandler = new UnifiedErrorHandler("Logback-OSSAppender");

        if (encoder == null) {
            handleConfigurationError("No encoder set for the appender named \"" + name + "\"",
                    ErrorCode.CONFIG_MISSING_REQUIRED_FIELD);
            return;
        }
        try {
            // 验证必需参数
            if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
                handleConfigurationError("accessKeyId must be set", ErrorCode.CONFIG_MISSING_REQUIRED_FIELD);
                return;
            }
            if (accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
                handleConfigurationError("accessKeySecret must be set", ErrorCode.CONFIG_MISSING_REQUIRED_FIELD);
                return;
            }
            if (bucket == null || bucket.trim().isEmpty()) {
                handleConfigurationError("bucket must be set", ErrorCode.CONFIG_MISSING_REQUIRED_FIELD);
                return;
            }

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
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public void setMaxBatchCount(int maxBatchCount) {
        this.maxBatchCount = maxBatchCount;
    }

    public void setMaxBatchBytes(int maxBatchBytes) {
        this.maxBatchBytes = maxBatchBytes;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public void setDropWhenQueueFull(boolean dropWhenQueueFull) {
        this.dropWhenQueueFull = dropWhenQueueFull;
    }

    public void setMultiProducer(boolean multiProducer) {
        this.multiProducer = multiProducer;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setBaseBackoffMs(long baseBackoffMs) {
        this.baseBackoffMs = baseBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        this.maxBackoffMs = maxBackoffMs;
    }
    // endregion

    /**
     * 统一错误处理方法
     */
    private void handleAppendError(String message, Throwable throwable) {
        if (errorHandler != null) {
            ErrorContext context = new ErrorContext.Builder().errorCode(ErrorCode.STORAGE_UPLOAD_FAILED)
                    .type(ErrorType.STORAGE_ERROR).severity(ErrorSeverity.MEDIUM).message(message).throwable(throwable)
                    .addContextData("component", "Logback-OSSAppender").addContextData("appender", getName()).build();

            errorHandler.handleError(context);
        } else {
            // 回退到Logback默认错误处理
            addError(message, throwable);
        }
    }

    private void handleConfigurationError(String message, ErrorCode errorCode) {
        if (errorHandler != null) {
            ErrorContext context = new ErrorContext.Builder().errorCode(errorCode).type(ErrorType.CONFIGURATION_ERROR)
                    .severity(ErrorSeverity.HIGH).message(message).addContextData("component", "Logback-OSSAppender")
                    .build();

            errorHandler.handleError(context);
        } else {
            // 回退到Logback默认错误处理
            addStatus(new ErrorStatus(message, this));
        }
    }
}
