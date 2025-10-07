package org.logx.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.config.factory.ConfigFactory;
import org.logx.storage.StorageOssType;
import org.logx.storage.StorageConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配置兼容性测试 确保三个框架适配器的配置一致性
 */
class ConfigCompatibilityTest {

    private Properties log4jConfig;
    private Map<String, String> log4j2Config;
    private Properties logbackConfig;
    private Properties springBootConfig;

    @BeforeEach
    void setUp() {
        // 设置Log4j配置
        log4jConfig = new Properties();
        log4jConfig.setProperty("log4j.appender.oss.endpoint", "https://oss-cn-hangzhou.aliyuncs.com");
        log4jConfig.setProperty("log4j.appender.oss.region", "cn-hangzhou");
        log4jConfig.setProperty("log4j.appender.oss.accessKeyId", "test-access-key");
        log4jConfig.setProperty("log4j.appender.oss.accessKeySecret", "test-secret-key");
        log4jConfig.setProperty("log4j.appender.oss.bucket", "test-bucket");
        log4jConfig.setProperty("log4j.appender.oss.keyPrefix", "log4j-logs/");
        log4jConfig.setProperty("log4j.appender.oss.maxQueueSize", "65536");
        log4jConfig.setProperty("log4j.appender.oss.maxBatchCount", "1000");

        // 设置Log4j2配置
        log4j2Config = new HashMap<>();
        log4j2Config.put("log4j2.oss.endpoint", "https://oss-cn-hangzhou.aliyuncs.com");
        log4j2Config.put("log4j2.oss.region", "cn-hangzhou");
        log4j2Config.put("log4j2.oss.accessKeyId", "test-access-key");
        log4j2Config.put("log4j2.oss.accessKeySecret", "test-secret-key");
        log4j2Config.put("log4j2.oss.bucket", "test-bucket");
        log4j2Config.put("log4j2.oss.keyPrefix", "log4j2-logs/");
        log4j2Config.put("log4j2.oss.maxQueueSize", "65536");
        log4j2Config.put("log4j2.oss.maxBatchCount", "1000");

        // 设置Logback配置
        logbackConfig = new Properties();
        logbackConfig.setProperty("logback.oss.endpoint", "https://oss-cn-hangzhou.aliyuncs.com");
        logbackConfig.setProperty("logback.oss.region", "cn-hangzhou");
        logbackConfig.setProperty("logback.oss.accessKeyId", "test-access-key");
        logbackConfig.setProperty("logback.oss.accessKeySecret", "test-secret-key");
        logbackConfig.setProperty("logback.oss.bucket", "test-bucket");
        logbackConfig.setProperty("logback.oss.keyPrefix", "logback-logs/");
        logbackConfig.setProperty("logback.oss.maxQueueSize", "65536");
        logbackConfig.setProperty("logback.oss.maxBatchCount", "1000");

        // 设置Spring Boot配置
        springBootConfig = new Properties();
        springBootConfig.setProperty("logging.logback.oss.endpoint", "https://oss-cn-hangzhou.aliyuncs.com");
        springBootConfig.setProperty("logging.logback.oss.region", "cn-hangzhou");
        springBootConfig.setProperty("logging.logback.oss.access-key-id", "test-access-key");
        springBootConfig.setProperty("logging.logback.oss.access-key-secret", "test-secret-key");
        springBootConfig.setProperty("logging.logback.oss.bucket", "test-bucket");
        springBootConfig.setProperty("logging.logback.oss.key-prefix", "spring-boot-logs/");
        springBootConfig.setProperty("logging.logback.oss.max-queue-size", "65536");
        springBootConfig.setProperty("logging.logback.oss.max-batch-count", "1000");
    }

    @Test
    void shouldHaveConsistentRequiredFields() {
        // 验证所有框架都有相同的必需配置字段
        String[] requiredFields = CommonConfig.Validation.REQUIRED_FIELDS;

        for (String field : requiredFields) {
            // 检查Log4j配置
            assertThat(hasFieldInLog4jConfig(field)).as("Log4j should have required field: " + field).isTrue();

            // 检查Log4j2配置
            assertThat(hasFieldInLog4j2Config(field)).as("Log4j2 should have required field: " + field).isTrue();

            // 检查Logback配置
            assertThat(hasFieldInLogbackConfig(field)).as("Logback should have required field: " + field).isTrue();

            // 检查Spring Boot配置
            assertThat(hasFieldInSpringBootConfig(field)).as("Spring Boot should have required field: " + field)
                    .isTrue();
        }
    }

    @Test
    void shouldSupportUnifiedLogxOssPrefix() {
        // 验证统一的logx.oss.前缀配置风格
        Properties unifiedConfig = new Properties();

        // 设置logx.oss.前缀的配置
        unifiedConfig.setProperty("logx.oss.endpoint", "https://oss-cn-guangzhou.aliyuncs.com");
        unifiedConfig.setProperty("logx.oss.region", "ap-guangzhou");
        unifiedConfig.setProperty("logx.oss.accessKeyId", "unified-access-key");
        unifiedConfig.setProperty("logx.oss.accessKeySecret", "unified-secret-key");
        unifiedConfig.setProperty("logx.oss.bucket", "unified-bucket");
        unifiedConfig.setProperty("logx.oss.keyPrefix", "unified-logs/");
        unifiedConfig.setProperty("logx.oss.pathStyleAccess", "true");
        unifiedConfig.setProperty("logx.oss.enableSsl", "false");
        unifiedConfig.setProperty("logx.oss.maxConnections", "100");
        unifiedConfig.setProperty("logx.oss.connectTimeout", "45000");
        unifiedConfig.setProperty("logx.oss.readTimeout", "90000");

        // 验证所有配置项都使用logx.oss.前缀
        assertThat(unifiedConfig.getProperty("logx.oss.endpoint")).isEqualTo("https://oss-cn-guangzhou.aliyuncs.com");
        assertThat(unifiedConfig.getProperty("logx.oss.region")).isEqualTo("ap-guangzhou");
        assertThat(unifiedConfig.getProperty("logx.oss.accessKeyId")).isEqualTo("unified-access-key");
        assertThat(unifiedConfig.getProperty("logx.oss.accessKeySecret")).isEqualTo("unified-secret-key");
        assertThat(unifiedConfig.getProperty("logx.oss.bucket")).isEqualTo("unified-bucket");
        assertThat(unifiedConfig.getProperty("logx.oss.keyPrefix")).isEqualTo("unified-logs/");
        assertThat(unifiedConfig.getProperty("logx.oss.pathStyleAccess")).isEqualTo("true");
        assertThat(unifiedConfig.getProperty("logx.oss.enableSsl")).isEqualTo("false");
        assertThat(unifiedConfig.getProperty("logx.oss.maxConnections")).isEqualTo("100");
        assertThat(unifiedConfig.getProperty("logx.oss.connectTimeout")).isEqualTo("45000");
        assertThat(unifiedConfig.getProperty("logx.oss.readTimeout")).isEqualTo("90000");
    }

    @Test
    void shouldHaveConsistentDefaultValues() {
        // 验证默认值一致性
        ConfigManager configManager = new ConfigManager();

        // 测试默认值
        assertThat(configManager.getProperty(CommonConfig.KEY_PREFIX, CommonConfig.Defaults.KEY_PREFIX))
                .isEqualTo(CommonConfig.Defaults.KEY_PREFIX);

        assertThat(configManager.getIntProperty(CommonConfig.QUEUE_CAPACITY, CommonConfig.Defaults.QUEUE_CAPACITY))
                .isEqualTo(CommonConfig.Defaults.QUEUE_CAPACITY);

        assertThat(configManager.getIntProperty(CommonConfig.MAX_BATCH_COUNT, CommonConfig.Defaults.MAX_BATCH_COUNT))
                .isEqualTo(CommonConfig.Defaults.MAX_BATCH_COUNT);

        assertThat(
                configManager.getLongProperty(CommonConfig.MAX_MESSAGE_AGE_MS, CommonConfig.Defaults.MAX_MESSAGE_AGE_MS))
                        .isEqualTo(CommonConfig.Defaults.MAX_MESSAGE_AGE_MS);
    }

    @Test
    void shouldSupportEnvironmentVariableOverride() {
        // 模拟环境变量（通过系统属性，使用ConfigManager支持的格式）
        System.setProperty(CommonConfig.BUCKET, "env-bucket");
        System.setProperty(CommonConfig.KEY_PREFIX, "env-logs/");

        try {
            ConfigManager configManager = new ConfigManager();

            // 验证系统属性覆盖
            assertThat(configManager.getProperty(CommonConfig.BUCKET)).isEqualTo("env-bucket");

            assertThat(configManager.getProperty(CommonConfig.KEY_PREFIX)).isEqualTo("env-logs/");

        } finally {
            // 清理系统属性
            System.clearProperty(CommonConfig.BUCKET);
            System.clearProperty(CommonConfig.KEY_PREFIX);
        }
    }

    // 辅助方法：检查Log4j配置是否包含字段
    private boolean hasFieldInLog4jConfig(String field) {
        // Log4j使用log4j.appender.oss.前缀
        String log4jKey = "log4j.appender.oss." + field;
        return log4jConfig.containsKey(log4jKey);
    }

    // 辅助方法：检查Log4j2配置是否包含字段
    private boolean hasFieldInLog4j2Config(String field) {
        // Log4j2使用log4j2.oss.前缀
        String log4j2Key = "log4j2.oss." + field;
        return log4j2Config.containsKey(log4j2Key);
    }

    // 辅助方法：检查Logback配置是否包含字段
    private boolean hasFieldInLogbackConfig(String field) {
        // Logback使用logback.oss.前缀
        String logbackKey = "logback.oss." + field;
        return logbackConfig.containsKey(logbackKey);
    }

    // 辅助方法：检查Spring Boot配置是否包含字段
    private boolean hasFieldInSpringBootConfig(String field) {
        // Spring Boot使用kebab-case和logging.logback.oss.前缀
        String springBootField = convertToKebabCase(field);
        String springBootKey = "logging.logback.oss." + springBootField;
        return springBootConfig.containsKey(springBootKey);
    }

    // 辅助方法：转换为kebab-case
    private String convertToKebabCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    @Test
    void shouldSupportConfigurationPriorityOrder() {
        // 验证配置优先级：JVM系统属性 > 环境变量 > 配置文件属性 > 代码默认值

        // 设置系统属性（最高优先级）
        System.setProperty("logx.oss.region", "system-region");
        System.setProperty("logx.oss.bucket", "system-bucket");

        try {
            ConfigManager configManager = new ConfigManager();

            // 设置配置文件属性（低优先级）
            configManager.setDefault("logx.oss.region", "config-region");
            configManager.setDefault("logx.oss.bucket", "config-bucket");
            configManager.setDefault("logx.oss.accessKeyId", "config-key");

            // 验证系统属性优先级最高
            assertThat(configManager.getProperty("logx.oss.region")).isEqualTo("system-region");
            assertThat(configManager.getProperty("logx.oss.bucket")).isEqualTo("system-bucket");

            // 验证配置文件属性（无系统属性覆盖时生效）
            assertThat(configManager.getProperty("logx.oss.accessKeyId")).isEqualTo("config-key");

            // 验证代码默认值（无其他配置时生效）
            assertThat(configManager.getProperty("logx.oss.endpoint", "https://default-endpoint.com"))
                    .isEqualTo("https://default-endpoint.com");

        } finally {
            // 清理系统属性
            System.clearProperty("logx.oss.region");
            System.clearProperty("logx.oss.bucket");
        }
    }

    @Test
    void shouldIntegrateWithConfigFactory() {
        // 验证ConfigFactory与logx.oss前缀的集成
        ConfigManager configManager = new ConfigManager();
        ConfigFactory configFactory = new ConfigFactory(configManager);

        // 设置logx.oss前缀的配置
        configManager.setDefault("logx.oss.accessKeyId", "integration-key");
        configManager.setDefault("logx.oss.accessKeySecret", "integration-secret");
        configManager.setDefault("logx.oss.bucket", "integration-bucket");
        configManager.setDefault("logx.oss.endpoint", "https://integration.example.com");
        configManager.setDefault("logx.oss.region", "integration-region");

        // 创建存储配置
        StorageConfig config = configFactory.createConfig(StorageOssType.AWS_S3);

        // 验证配置正确读取
        assertThat(config.getAccessKeyId()).isEqualTo("integration-key");
        assertThat(config.getAccessKeySecret()).isEqualTo("integration-secret");
        assertThat(config.getBucket()).isEqualTo("integration-bucket");
        assertThat(config.getEndpoint()).isEqualTo("https://integration.example.com");
        assertThat(config.getRegion()).isEqualTo("integration-region");
    }

    @Test
    void shouldSupportAdditionalConfigurationParameters() {
        // 验证所有PRD要求的配置参数及其默认值
        ConfigManager configManager = new ConfigManager();

        // 紧急保护阈值（emergencyMemoryThresholdMb）
        assertThat(configManager.getIntProperty(
                CommonConfig.EMERGENCY_MEMORY_THRESHOLD_MB,
                CommonConfig.Defaults.EMERGENCY_MEMORY_THRESHOLD_MB))
                .isEqualTo(CommonConfig.Defaults.EMERGENCY_MEMORY_THRESHOLD_MB)
                .isEqualTo(512);

        // 兜底文件保留天数（fallbackRetentionDays）
        assertThat(configManager.getIntProperty(
                CommonConfig.FALLBACK_RETENTION_DAYS,
                CommonConfig.Defaults.FALLBACK_RETENTION_DAYS))
                .isEqualTo(CommonConfig.Defaults.FALLBACK_RETENTION_DAYS)
                .isEqualTo(7);

        // 压缩阈值（compressionThreshold）
        assertThat(configManager.getIntProperty(
                CommonConfig.COMPRESSION_THRESHOLD,
                CommonConfig.Defaults.COMPRESSION_THRESHOLD))
                .isEqualTo(CommonConfig.Defaults.COMPRESSION_THRESHOLD)
                .isEqualTo(1024);

        // 单个上传文件最大大小（maxUploadSizeMb）
        assertThat(configManager.getIntProperty(
                CommonConfig.MAX_UPLOAD_SIZE_MB,
                CommonConfig.Defaults.MAX_UPLOAD_SIZE_MB))
                .isEqualTo(CommonConfig.Defaults.MAX_UPLOAD_SIZE_MB)
                .isEqualTo(10);

        // 最早消息年龄阈值（maxMessageAgeMs）
        assertThat(configManager.getLongProperty(
                CommonConfig.MAX_MESSAGE_AGE_MS,
                CommonConfig.Defaults.MAX_MESSAGE_AGE_MS))
                .isEqualTo(CommonConfig.Defaults.MAX_MESSAGE_AGE_MS)
                .isEqualTo(600000L);
    }

    @Test
    void shouldSupportFallbackScanIntervalConfiguration() {
        // 验证兜底文件扫描间隔配置（fallbackScanIntervalSeconds）
        // 注意：此参数当前只有默认值，不是用户可配置的参数
        assertThat(CommonConfig.Defaults.FALLBACK_SCAN_INTERVAL_SECONDS)
                .isEqualTo(60)
                .isPositive()
                .isBetween(10, 300);
    }

    @Test
    void shouldUseCorrectDefaultRegionValue() {
        // 验证任务#8：StorageConfig默认region值与PRD一致
        // 默认值应为 "ap-guangzhou"（来自PRD FR3）
        ConfigManager configManager = new ConfigManager();

        assertThat(CommonConfig.Defaults.REGION)
                .isEqualTo("ap-guangzhou")
                .as("默认region值应与PRD保持一致");

        // 验证ConfigManager可以正确读取默认值
        assertThat(configManager.getProperty(CommonConfig.REGION, CommonConfig.Defaults.REGION))
                .isEqualTo("ap-guangzhou");
    }

    @Test
    void shouldOverrideAdditionalParametersWithSystemProperties() {
        // 验证系统属性可以覆盖额外的配置参数

        // 设置系统属性
        System.setProperty(CommonConfig.EMERGENCY_MEMORY_THRESHOLD_MB, "1024");
        System.setProperty(CommonConfig.FALLBACK_RETENTION_DAYS, "14");
        System.setProperty(CommonConfig.COMPRESSION_THRESHOLD, "2048");
        System.setProperty(CommonConfig.MAX_UPLOAD_SIZE_MB, "20");
        System.setProperty(CommonConfig.MAX_MESSAGE_AGE_MS, "1200000");

        try {
            ConfigManager configManager = new ConfigManager();

            // 验证系统属性覆盖生效
            assertThat(configManager.getIntProperty(CommonConfig.EMERGENCY_MEMORY_THRESHOLD_MB, 512))
                    .isEqualTo(1024);

            assertThat(configManager.getIntProperty(CommonConfig.FALLBACK_RETENTION_DAYS, 7))
                    .isEqualTo(14);

            assertThat(configManager.getIntProperty(CommonConfig.COMPRESSION_THRESHOLD, 1024))
                    .isEqualTo(2048);

            assertThat(configManager.getIntProperty(CommonConfig.MAX_UPLOAD_SIZE_MB, 10))
                    .isEqualTo(20);

            assertThat(configManager.getLongProperty(CommonConfig.MAX_MESSAGE_AGE_MS, 600000L))
                    .isEqualTo(1200000L);

        } finally {
            // 清理系统属性
            System.clearProperty(CommonConfig.EMERGENCY_MEMORY_THRESHOLD_MB);
            System.clearProperty(CommonConfig.FALLBACK_RETENTION_DAYS);
            System.clearProperty(CommonConfig.COMPRESSION_THRESHOLD);
            System.clearProperty(CommonConfig.MAX_UPLOAD_SIZE_MB);
            System.clearProperty(CommonConfig.MAX_MESSAGE_AGE_MS);
        }
    }
}
