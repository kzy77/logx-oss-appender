package org.logx.config;

/**
 * 通用配置常量定义类
 * 定义所有框架适配器（Log4j、Log4j2、Logback）共享的配置参数名称，确保配置一致性
 * 使用统一的LOGX_OSS_前缀环境变量配置
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public final class CommonConfig {

    // S3兼容存储基础配置
    public static final String ENDPOINT = "endpoint";
    public static final String REGION = "region";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String ACCESS_KEY_SECRET = "accessKeySecret";
    public static final String BUCKET = "bucket";
    public static final String KEY_PREFIX = "keyPrefix";
    public static final String OSS_TYPE = "ossType";

    // 队列配置
    public static final String MAX_QUEUE_SIZE = "maxQueueSize";
    public static final String MAX_BATCH_COUNT = "maxBatchCount";
    public static final String MAX_BATCH_BYTES = "maxBatchBytes";
    public static final String FLUSH_INTERVAL_MS = "flushIntervalMs";
    public static final String DROP_WHEN_QUEUE_FULL = "dropWhenQueueFull";
    public static final String MULTI_PRODUCER = "multiProducer";

    // 上传配置
    public static final String MAX_RETRIES = "maxRetries";
    public static final String BASE_BACKOFF_MS = "baseBackoffMs";
    public static final String MAX_BACKOFF_MS = "maxBackoffMs";
    public static final String MAX_UPLOAD_SIZE_MB = "maxUploadSizeMb";

    // 日志格式配置
    public static final String PATTERN = "pattern";
    public static final String LEVEL = "level";

    

    /**
     * 默认配置值
     */
    public static final class Defaults {
        public static final String KEY_PREFIX = "logs/";
        public static final int MAX_QUEUE_SIZE = 200_000;
        public static final int MAX_BATCH_COUNT = 5_000;
        public static final int MAX_BATCH_BYTES = 4 * 1024 * 1024;
        public static final long FLUSH_INTERVAL_MS = 2000L;
        public static final boolean DROP_WHEN_QUEUE_FULL = false;
        public static final boolean MULTI_PRODUCER = false;
        public static final int MAX_RETRIES = 5;
        public static final long BASE_BACKOFF_MS = 200L;
        public static final long MAX_BACKOFF_MS = 10000L;
        public static final int MAX_UPLOAD_SIZE_MB = 100; // 默认最大上传文件大小100MB
        public static final String OSS_TYPE = "SF_OSS"; // 默认OSS类型为SF_OSS
        public static final String PATTERN = "%d{ISO8601} [%t] %-5level %logger{36} - %msg%n";
        public static final String LEVEL = "INFO";
    }

    /**
     * 配置验证规则
     */
    public static final class Validation {
        // 必需字段
        public static final String[] REQUIRED_FIELDS = { ACCESS_KEY_ID, ACCESS_KEY_SECRET, BUCKET };

        // 数值范围
        public static final int MIN_QUEUE_SIZE = 1024;
        public static final int MAX_QUEUE_SIZE_LIMIT = 1_000_000;
        public static final int MIN_BATCH_COUNT = 1;
        public static final int MAX_BATCH_COUNT_LIMIT = 50_000;
        public static final int MIN_BATCH_BYTES = 1024;
        public static final int MAX_BATCH_BYTES_LIMIT = 100 * 1024 * 1024; // 100MB
        public static final long MIN_FLUSH_INTERVAL = 100L;
        public static final long MAX_FLUSH_INTERVAL = 300_000L; // 5分钟
        public static final int MIN_RETRIES = 0;
        public static final int MAX_RETRIES_LIMIT = 20;
        public static final long MIN_BACKOFF = 50L;
        public static final long MAX_BACKOFF_LIMIT = 600_000L; // 10分钟
        public static final int MIN_UPLOAD_SIZE_MB = 1; // 最小1MB
        public static final int MAX_UPLOAD_SIZE_MB_LIMIT = 1024; // 最大1024MB (1GB)
    }

    /**
     * 环境变量名映射
     */
    public static final class EnvVars {
        public static final String ENDPOINT = "LOGX_OSS_ENDPOINT";
        public static final String REGION = "LOGX_OSS_REGION";
        public static final String ACCESS_KEY_ID = "LOGX_OSS_ACCESS_KEY_ID";
        public static final String ACCESS_KEY_SECRET = "LOGX_OSS_ACCESS_KEY_SECRET";
        public static final String BUCKET = "LOGX_OSS_BUCKET";
        public static final String KEY_PREFIX = "LOGX_OSS_KEY_PREFIX";
        public static final String OSS_TYPE = "LOGX_OSS_TYPE";
        public static final String MAX_UPLOAD_SIZE_MB = "LOGX_OSS_MAX_UPLOAD_SIZE_MB";
    }

    // 配置迁移功能已移除，仅保留核心配置参数

    // 私有构造函数防止实例化
    private CommonConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
