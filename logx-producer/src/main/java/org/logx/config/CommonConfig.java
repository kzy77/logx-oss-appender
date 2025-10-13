package org.logx.config;

/**
 * 通用配置常量定义类
 * <p>
 * 定义LogX OSS Appender的统一配置管理中心，包括：
 * <ul>
 * <li>存储服务配置（OSS、S3等）</li>
 * <li>批处理触发条件配置</li>
 * <li>紧急保护配置</li>
 * <li>兜底机制配置</li>
 * <li>内部技术参数配置</li>
 * </ul>
 * <p>
 * <b>配置分类：</b>
 * <ul>
 * <li>用户可配置参数（14个）：存储凭证、触发条件、兜底配置等</li>
 * <li>内部技术参数（20个）：线程池、压缩、分片、重试等优化参数</li>
 * <li>所有框架适配器（Log4j、Log4j2、Logback）共享这些配置</li>
 * </ul>
 * <p>
 * 使用统一的LOGX_OSS_前缀环境变量配置。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public final class CommonConfig {

    // ==================== 存储服务配置（7个）====================

    /**
     * 对象存储服务端点地址
     * <p>
     * 示例：https://oss-cn-hangzhou.aliyuncs.com
     * <p>
     * 必需参数
     */
    public static final String ENDPOINT = "endpoint";

    /**
     * 存储区域
     * <p>
     * 示例：cn-hangzhou
     * <p>
     * 可选参数
     */
    public static final String REGION = "region";

    /**
     * 访问密钥ID
     * <p>
     * 必需参数
     */
    public static final String ACCESS_KEY_ID = "accessKeyId";

    /**
     * 访问密钥Secret
     * <p>
     * 必需参数
     */
    public static final String ACCESS_KEY_SECRET = "accessKeySecret";

    /**
     * 存储桶名称
     * <p>
     * 必需参数
     */
    public static final String BUCKET = "bucket";

    /**
     * 文件路径前缀（同时用于OSS对象路径和本地兜底路径）
     * <p>
     * 用途：
     * 1. OSS对象存储：logs/2024-10-05/applogx-xxx.log
     * 2. 本地兜底文件：logs/applogx-fallback-xxx.log
     * <p>
     * 示例：logs/
     * <p>
     * 可选参数，默认：logs/
     */
    public static final String KEY_PREFIX = "keyPrefix";

    /**
     * 存储后端类型
     * <p>
     * 可选值：SF_OSS、S3
     * <p>
     * 可选参数，默认：SF_OSS
     */
    public static final String OSS_TYPE = "ossType";

    // ==================== 批处理触发配置（3个）====================

    /**
     * 触发条件1：消息数量阈值
     * <p>
     * 当队列中消息数量达到此值时触发上传
     * <p>
     * 可选参数，默认：8192条
     */
    public static final String MAX_BATCH_COUNT = "maxBatchCount";

    /**
     * 触发条件2：内存占用阈值
     * <p>
     * 当队列中消息内存占用达到此字节数时触发上传
     * <p>
     * 可选参数，默认：10MB
     */
    public static final String MAX_BATCH_BYTES = "maxBatchBytes";

    /**
     * 触发条件3：最早消息年龄阈值（maxMessageAgeMs）
     * <p>
     * 当队列中最早的消息超过此毫秒数时触发上传
     * <p>
     * <b>重要说明：</b>
     * <ul>
     * <li>maxMessageAgeMs是三个批处理触发条件之一（另外两个是maxBatchCount和maxBatchBytes）</li>
     * <li>触发方式：事件驱动，每次新消息到达时检查是否满足任一触发条件，无固定检查间隔</li>
     * <li>只有当触发条件满足时（消息数量、总字节数或消息年龄），才会触发OSS上传</li>
     * <li>基于LMAX Disruptor的无锁队列，低延迟触发</li>
     * </ul>
     * <p>
     * 可选参数，默认：1分钟（60000毫秒）
     */
    public static final String MAX_MESSAGE_AGE_MS = "maxMessageAgeMs";

    // ==================== 紧急保护配置（1个）====================

    /**
     * 紧急保护阈值：内存占用上限
     * <p>
     * 当队列内存占用超过此MB数时，直接将新消息写入兜底文件
     * <p>
     * 可选参数，默认：512MB
     */
    public static final String EMERGENCY_MEMORY_THRESHOLD_MB = "emergencyMemoryThresholdMb";

    // ==================== 兜底机制配置（2个）====================
    // 注：兜底文件路径使用KEY_PREFIX配置（同时用于OSS和兜底）

    /**
     * 兜底文件保留天数
     * <p>
     * 超过此天数的兜底文件会被自动清理
     * <p>
     * 可选参数，默认：7天
     */
    public static final String FALLBACK_RETENTION_DAYS = "fallbackRetentionDays";

    // ==================== 文件配置（1个）====================

    /**
     * 日志文件名前缀
     * <p>
     * 用于生成对象存储中的文件名
     * <p>
     * 可选参数，默认：applogx
     */
    public static final String LOG_FILE_NAME = "logFileName";

    // ==================== 队列配置（3个）====================

    /** Disruptor环形缓冲大小 */
    public static final String QUEUE_CAPACITY = "queueCapacity";

    /** 队列满时是否丢弃新消息 */
    public static final String DROP_WHEN_QUEUE_FULL = "dropWhenQueueFull";

    /** 是否支持多生产者模式 */
    public static final String MULTI_PRODUCER = "multiProducer";

    // ==================== 线程池配置（5个）====================

    /** 线程池核心线程数 */
    public static final String CORE_POOL_SIZE = "corePoolSize";

    /** 线程池最大线程数 */
    public static final String MAXIMUM_POOL_SIZE = "maximumPoolSize";

    /** 线程池队列容量 */
    public static final String QUEUE_CAPACITY_THREAD_POOL = "queueCapacityThreadPool";

    /** 是否启用CPU让出机制 */
    public static final String ENABLE_CPU_YIELD = "enableCpuYield";

    /** 是否启用内存保护机制 */
    public static final String ENABLE_MEMORY_PROTECTION = "enableMemoryProtection";

    // ==================== 压缩配置（2个）====================

    /** 是否启用数据压缩 */
    public static final String ENABLE_COMPRESSION = "enableCompression";

    

    // ==================== 分片配置（2个）====================

    /** 是否启用数据分片 */
    public static final String ENABLE_SHARDING = "enableSharding";

    /**
     * 单个上传文件最大大小（MB）
     * <p>
     * 用户友好的配置参数，以MB为单位。
     * <p>
     * 此参数同时控制：
     * <ul>
     * <li>分片阈值：数据超过此大小时自动分片</li>
     * <li>分片大小：每个分片的大小</li>
     * </ul>
     * <p>
     * 可选参数，默认：10MB
     */
    public static final String MAX_UPLOAD_SIZE_MB = "maxUploadSizeMb";

    // ==================== 重试配置（3个）====================

    /** 最大重试次数 */
    public static final String MAX_RETRIES = "maxRetries";

    /** 基础退避时间（毫秒） */
    public static final String BASE_BACKOFF_MS = "baseBackoffMs";

    /** 最大退避时间（毫秒） */
    public static final String MAX_BACKOFF_MS = "maxBackoffMs";

    // ==================== 其他配置（2个）====================

    /** 最大关闭等待时间（毫秒） */
    public static final String MAX_SHUTDOWN_WAIT_MS = "maxShutdownWaitMs";

    /** 日志级别 */
    public static final String LEVEL = "level";



    /**
     * 默认配置值
     * <p>
     * 所有配置参数的默认值集中管理，按功能分组。
     * 这些默认值经过性能测试和生产环境验证，适用于大多数场景。
     */
    public static final class Defaults {

        // ==================== 基础配置 ====================

        /**
         * 默认字符编码
         * <p>
         * 使用UTF-8确保中文等多字节字符正确处理
         */
        public static final String DEFAULT_CHARSET = "UTF-8";

        /**
         * 对象存储中的文件路径前缀
         * <p>
         * 默认为 "logs/"，所有上传的日志文件都会存储在此目录下
         */
        public static final String KEY_PREFIX = "logs/";

        /**
         * 日志文件名前缀
         * <p>
         * 用于生成对象名，默认为 "applogx"
         */
        public static final String LOG_FILE_NAME = "applogx";

        /**
         * 默认OSS存储类型
         * <p>
         * 可选值：SF_OSS、S3
         */
        public static final String OSS_TYPE = "SF_OSS";

        /**
         * 默认存储区域
         * <p>
         * 默认为 "us"（广州区域）
         */
        public static final String REGION = "us";

        // ==================== 队列配置 ====================

        /**
         * Disruptor环形缓冲大小
         * <p>
         * 默认524288，必须是2的幂。此值决定队列的最大容量。
         * 建议范围：8192-1048576
         */
        public static final int QUEUE_CAPACITY = 524288;

        /**
         * 队列满时是否丢弃新消息
         * <p>
         * false表示阻塞等待，true表示直接丢弃
         * 推荐false以保证数据不丢失
         */
        public static final boolean DROP_WHEN_QUEUE_FULL = false;

        /**
         * 是否支持多生产者模式
         * <p>
         * false表示单生产者（性能更好），true表示多生产者（支持并发写入）
         * 大多数场景使用单生产者即可
         */
        public static final boolean MULTI_PRODUCER = false;

        // ==================== 批处理触发配置 ====================

        /**
         * 触发条件1：消息数量阈值
         * <p>
         * 当队列中消息数量达到8192条时触发上传
         * 建议范围：100-50000
         */
        public static final int MAX_BATCH_COUNT = 8192;

        /**
         * 触发条件2：内存占用阈值
         * <p>
         * 当队列中消息内存占用达到10MB时触发上传
         * 默认：10MB (10 * 1024 * 1024 字节)
         * 建议范围：1MB-100MB
         */
        public static final int MAX_BATCH_BYTES = 10 * 1024 * 1024;

        /**
         * 触发条件3：最早消息年龄阈值（maxMessageAgeMs）
         * <p>
         * 当队列中最早的消息超过10分钟时触发上传
         * <p>
         * <b>触发机制：</b>
         * <ul>
         * <li>maxMessageAgeMs是三个批处理触发条件之一（另外两个是MAX_BATCH_COUNT和MAX_BATCH_BYTES）</li>
         * <li>事件驱动：每次新消息到达时检查是否满足任一触发条件，无固定检查间隔</li>
         * <li>基于LMAX Disruptor的无锁队列，低延迟触发</li>
         * </ul>
         * <p>
         * 默认：60000毫秒 (1分钟)
         * 建议范围：1秒-30分钟
         */
        public static final long MAX_MESSAGE_AGE_MS = 60000L;

        // ==================== 紧急保护配置 ====================

        /**
         * 紧急保护阈值：内存占用上限
         * <p>
         * 当队列内存占用超过512MB时，直接将新消息写入兜底文件
         * 这是最后一道防线，防止OOM
         * 默认：512MB
         * 建议范围：256MB-2048MB
         */
        public static final int EMERGENCY_MEMORY_THRESHOLD_MB = 512;

        // ==================== 线程池配置 ====================

        /**
         * 线程池核心线程数
         * <p>
         * 用于异步处理日志上传任务
         * 默认：1
         * 建议范围：1-8
         */
        public static final int CORE_POOL_SIZE = 1;

        /**
         * 线程池最大线程数
         * <p>
         * 当核心线程忙碌时，可以创建的最大线程数
         * 默认：1
         * 建议范围：2-16
         */
        public static final int MAXIMUM_POOL_SIZE = 1;

        /**
         * 线程池队列容量
         * <p>
         * 用于存储等待执行的任务
         * 默认：500
         */
        public static final int QUEUE_CAPACITY_THREAD_POOL = 500;

        /**
         * 是否启用CPU让出机制
         * <p>
         * 在密集操作中主动让出CPU，避免长时间占用
         * 默认：true
         */
        public static final boolean ENABLE_CPU_YIELD = true;

        /**
         * 是否启用内存保护机制
         * <p>
         * 启用后会监控内存使用，防止OOM
         * 默认：true
         */
        public static final boolean ENABLE_MEMORY_PROTECTION = true;

        /**
         * 消费者线程数
         * <p>
         * 用于并行处理日志批次的线程数
         * 默认：1
         * 建议范围：1-16
         */
        public static final int CONSUMER_THREAD_COUNT = 1;

        /**
         * 批处理队列容量
         * <p>
         * 用于存储批处理事件的队列容量
         * 默认：1024
         * 建议范围：128-65536
         */
        public static final int BATCH_QUEUE_CAPACITY = 1024;

        // ==================== 压缩配置 ====================

        /**
         * 是否启用数据压缩
         * <p>
         * 使用GZIP压缩可以节省90%+的存储空间和网络带宽
         * 默认：true
         */
        public static final boolean ENABLE_COMPRESSION = true;

        

        // ==================== 分片配置 ====================

        /**
         * 是否启用数据分片
         * <p>
         * 大文件自动分片可以提高上传成功率
         * 默认：true
         */
        public static final boolean ENABLE_SHARDING = true;

        /**
         * 单个上传文件最大大小（MB）
         * <p>
         * 用户友好的配置参数，以MB为单位。
         * <p>
         * 此参数同时控制：
         * <ul>
         * <li>分片阈值：数据超过 maxUploadSizeMb * 1024 * 1024 字节时自动分片</li>
         * <li>分片大小：每个分片大小为 maxUploadSizeMb * 1024 * 1024 字节</li>
         * </ul>
         * <p>
         * 默认：10MB
         * 建议范围：5MB-100MB
         */
        public static final int MAX_UPLOAD_SIZE_MB = 10;

        // ==================== 重试配置 ====================

        /**
         * 最大重试次数
         * <p>
         * 上传失败后最多重试多少次
         * 默认：3次
         * 建议范围：0-20
         */
        public static final int MAX_RETRIES = 3;

        /**
         * 基础退避时间
         * <p>
         * 第一次重试的等待时间，后续重试时间会指数增长
         * 默认：200毫秒
         * 建议范围：50ms-5000ms
         */
        public static final long BASE_BACKOFF_MS = 200L;

        /**
         * 最大退避时间
         * <p>
         * 重试等待时间的上限
         * 默认：10000毫秒 (10秒)
         * 建议范围：1秒-10分钟
         */
        public static final long MAX_BACKOFF_MS = 10000L;

        // ==================== 关闭配置 ====================

        /**
         * 最大关闭等待时间
         * <p>
         * JVM关闭时等待队列处理完成的最长时间
         * 默认：30000毫秒 (30秒)
         * 建议范围：500ms-30000ms
         */
        public static final long MAX_SHUTDOWN_WAIT_MS = 30000L;

        // ==================== 日志格式配置 ====================

        /**
         * 日志格式模板
         * <p>
         * 使用UTF-8编码避免乱码
         */
        public static final String PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n";

        /**
         * 日志级别
         * <p>
         * 默认INFO级别
         */
        public static final String LEVEL = "INFO";

        // ==================== 兜底机制配置 ====================

        /**
         * 兜底文件保留天数
         * <p>
         * 超过此天数的兜底文件会被自动清理
         * 默认：7天
         * 建议范围：1-30天
         */
        public static final int FALLBACK_RETENTION_DAYS = 7;

        /**
         * 兜底文件扫描间隔
         * <p>
         * 定时扫描兜底文件并尝试重传的间隔
         * 默认：60秒
         * 建议范围：10秒-300秒
         */
        public static final int FALLBACK_SCAN_INTERVAL_SECONDS = 60;

        // 私有构造函数，防止实例化
        private Defaults() {
            throw new UnsupportedOperationException("Defaults class cannot be instantiated");
        }
    }

    /**
     * 配置验证规则
     * <p>
     * 定义各配置参数的有效范围，与PRD建议范围保持一致
     */
    public static final class Validation {
        // 必需字段
        static final String[] REQUIRED_FIELDS = { ACCESS_KEY_ID, ACCESS_KEY_SECRET, BUCKET };

        // 队列容量验证（建议范围：8192-1048576）
        public static final int MIN_QUEUE_SIZE = 8192;
        public static final int MAX_QUEUE_SIZE_LIMIT = 1_048_576;

        // 批处理大小验证（建议范围：100-50000）
        public static final int MIN_BATCH_COUNT = 100;
        public static final int MAX_BATCH_COUNT_LIMIT = 50_000;

        // 批处理字节数验证（建议范围：1MB-100MB）
        public static final int MIN_BATCH_BYTES = 1024 * 1024;
        public static final int MAX_BATCH_BYTES_LIMIT = 100 * 1024 * 1024;

        // 重试次数验证（建议范围：0-20）
        public static final int MIN_RETRIES = 0;
        public static final int MAX_RETRIES_LIMIT = 20;

        // 基础退避时间验证（建议范围：50ms-5000ms）
        public static final long MIN_BASE_BACKOFF = 50L;
        public static final long MAX_BASE_BACKOFF_LIMIT = 5_000L;

        // 最大退避时间验证（建议范围：1秒-10分钟）
        public static final long MIN_MAX_BACKOFF = 1_000L;
        public static final long MAX_MAX_BACKOFF_LIMIT = 600_000L;

        // 兼容性：保留旧的MIN_BACKOFF和MAX_BACKOFF_LIMIT
        @Deprecated
        public static final long MIN_BACKOFF = MIN_BASE_BACKOFF;
        @Deprecated
        public static final long MAX_BACKOFF_LIMIT = MAX_MAX_BACKOFF_LIMIT;
    }

    /**
     * 环境变量名映射
     * <p>
     * 所有配置参数都支持通过环境变量设置，使用统一的LOGX_OSS_前缀
     */
    public static final class EnvVars {
        // 存储服务配置
        public static final String ENDPOINT = "LOGX_OSS_ENDPOINT";
        public static final String REGION = "LOGX_OSS_REGION";
        public static final String ACCESS_KEY_ID = "LOGX_OSS_ACCESS_KEY_ID";
        public static final String ACCESS_KEY_SECRET = "LOGX_OSS_ACCESS_KEY_SECRET";
        public static final String BUCKET = "LOGX_OSS_BUCKET";
        public static final String KEY_PREFIX = "LOGX_OSS_KEY_PREFIX";
        public static final String OSS_TYPE = "LOGX_OSS_TYPE";

        // 批处理触发配置
        public static final String MAX_BATCH_COUNT = "LOGX_OSS_MAX_BATCH_COUNT";
        public static final String MAX_BATCH_BYTES = "LOGX_OSS_MAX_BATCH_BYTES";
        public static final String MAX_MESSAGE_AGE_MS = "LOGX_OSS_MAX_MESSAGE_AGE_MS";

        // 紧急保护配置
        public static final String EMERGENCY_MEMORY_THRESHOLD_MB = "LOGX_OSS_EMERGENCY_MEMORY_THRESHOLD_MB";

        // 兜底机制配置
        public static final String FALLBACK_RETENTION_DAYS = "LOGX_OSS_FALLBACK_RETENTION_DAYS";
        public static final String FALLBACK_SCAN_INTERVAL_SECONDS = "LOGX_OSS_FALLBACK_SCAN_INTERVAL_SECONDS";

        // 文件配置
        public static final String LOG_FILE_NAME = "LOGX_OSS_LOG_FILE_NAME";

        // 队列配置
        public static final String QUEUE_CAPACITY = "LOGX_OSS_QUEUE_CAPACITY";
        public static final String DROP_WHEN_QUEUE_FULL = "LOGX_OSS_DROP_WHEN_QUEUE_FULL";
        public static final String MULTI_PRODUCER = "LOGX_OSS_MULTI_PRODUCER";

        // 线程池配置
        public static final String CORE_POOL_SIZE = "LOGX_OSS_CORE_POOL_SIZE";
        public static final String MAXIMUM_POOL_SIZE = "LOGX_OSS_MAXIMUM_POOL_SIZE";
        public static final String QUEUE_CAPACITY_THREAD_POOL = "LOGX_OSS_QUEUE_CAPACITY_THREAD_POOL";
        public static final String ENABLE_CPU_YIELD = "LOGX_OSS_ENABLE_CPU_YIELD";
        public static final String ENABLE_MEMORY_PROTECTION = "LOGX_OSS_ENABLE_MEMORY_PROTECTION";

        // 压缩配置
        public static final String ENABLE_COMPRESSION = "LOGX_OSS_ENABLE_COMPRESSION";
        

        // 分片配置
        public static final String ENABLE_SHARDING = "LOGX_OSS_ENABLE_SHARDING";
        public static final String MAX_UPLOAD_SIZE_MB = "LOGX_OSS_MAX_UPLOAD_SIZE_MB";

        // 重试配置
        public static final String MAX_RETRIES = "LOGX_OSS_MAX_RETRIES";
        public static final String BASE_BACKOFF_MS = "LOGX_OSS_BASE_BACKOFF_MS";
        public static final String MAX_BACKOFF_MS = "LOGX_OSS_MAX_BACKOFF_MS";

        // 其他配置
        public static final String MAX_SHUTDOWN_WAIT_MS = "LOGX_OSS_MAX_SHUTDOWN_WAIT_MS";
        public static final String LEVEL = "LOGX_OSS_LEVEL";
    }

    // 配置迁移功能已移除，仅保留核心配置参数

    // 私有构造函数防止实例化
    private CommonConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
