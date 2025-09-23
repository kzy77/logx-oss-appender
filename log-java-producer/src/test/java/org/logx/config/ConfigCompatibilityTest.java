package org.logx.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void shouldHaveConsistentDefaultValues() {
        // 验证默认值一致性
        ConfigManager configManager = new ConfigManager();

        // 测试默认值
        assertThat(configManager.getProperty(CommonConfig.KEY_PREFIX, CommonConfig.Defaults.KEY_PREFIX))
                .isEqualTo(CommonConfig.Defaults.KEY_PREFIX);

        assertThat(configManager.getIntProperty(CommonConfig.MAX_QUEUE_SIZE, CommonConfig.Defaults.MAX_QUEUE_SIZE))
                .isEqualTo(CommonConfig.Defaults.MAX_QUEUE_SIZE);

        assertThat(configManager.getIntProperty(CommonConfig.MAX_BATCH_COUNT, CommonConfig.Defaults.MAX_BATCH_COUNT))
                .isEqualTo(CommonConfig.Defaults.MAX_BATCH_COUNT);

        assertThat(
                configManager.getLongProperty(CommonConfig.FLUSH_INTERVAL_MS, CommonConfig.Defaults.FLUSH_INTERVAL_MS))
                        .isEqualTo(CommonConfig.Defaults.FLUSH_INTERVAL_MS);
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
        String log4jKey = CommonConfig.log4jKey(field);
        return log4jConfig.containsKey(log4jKey);
    }

    // 辅助方法：检查Log4j2配置是否包含字段
    private boolean hasFieldInLog4j2Config(String field) {
        String log4j2Key = CommonConfig.log4j2Key(field);
        return log4j2Config.containsKey(log4j2Key);
    }

    // 辅助方法：检查Logback配置是否包含字段
    private boolean hasFieldInLogbackConfig(String field) {
        String logbackKey = CommonConfig.logbackKey(field);
        return logbackConfig.containsKey(logbackKey);
    }

    // 辅助方法：检查Spring Boot配置是否包含字段
    private boolean hasFieldInSpringBootConfig(String field) {
        // Spring Boot使用kebab-case
        String springBootField = convertToKebabCase(field);
        String springBootKey = CommonConfig.springBootKey(springBootField);
        return springBootConfig.containsKey(springBootKey);
    }

    // 辅助方法：转换为kebab-case
    private String convertToKebabCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
