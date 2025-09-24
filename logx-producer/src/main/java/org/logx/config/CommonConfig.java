package org.logx.config;

/**
 * 通用配置常量定义类 定义所有框架适配器（Log4j、Log4j2、Logback）共享的配置参数名称，确保配置一致性
 *
 * @author OSS Appender Team
 *
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

    // 日志格式配置
    public static final String PATTERN = "pattern";
    public static final String LEVEL = "level";

    // 配置前缀（用于不同配置源）
    public static final String LOG4J_PREFIX = "log4j.appender.oss.";
    public static final String LOG4J2_PREFIX = "log4j2.oss.";
    public static final String LOGBACK_PREFIX = "logback.oss.";
    public static final String SPRING_BOOT_PREFIX = "logging.logback.oss.";

    /**
     * 获取Log4j配置键
     */
    public static String log4jKey(String key) {
        return LOG4J_PREFIX + key;
    }

    /**
     * 获取Log4j2配置键
     */
    public static String log4j2Key(String key) {
        return LOG4J2_PREFIX + key;
    }

    /**
     * 获取Logback配置键
     */
    public static String logbackKey(String key) {
        return LOGBACK_PREFIX + key;
    }

    /**
     * 获取Spring Boot配置键
     */
    public static String springBootKey(String key) {
        return SPRING_BOOT_PREFIX + key;
    }

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
    }

    /**
     * 环境变量名映射
     */
    public static final class EnvVars {
        public static final String ENDPOINT = "OSS_ENDPOINT";
        public static final String REGION = "OSS_REGION";
        public static final String ACCESS_KEY_ID = "OSS_ACCESS_KEY_ID";
        public static final String ACCESS_KEY_SECRET = "OSS_ACCESS_KEY_SECRET";
        public static final String BUCKET = "OSS_BUCKET";
        public static final String KEY_PREFIX = "OSS_KEY_PREFIX";
    }

    // 配置迁移功能已移除，仅保留核心配置参数

    // 私有构造函数防止实例化
    private CommonConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
