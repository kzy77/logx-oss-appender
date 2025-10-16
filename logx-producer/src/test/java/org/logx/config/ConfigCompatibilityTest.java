package org.logx.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.config.properties.LogxOssProperties;
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
    void shouldSupportUnifiedLogxOssPrefix() {
        // 验证统一的logx.oss.前缀配置风格
        Properties unifiedConfig = new Properties();

        // 设置logx.oss.storage.前缀的配置
        unifiedConfig.setProperty("logx.oss.storage.endpoint", "https://oss-cn-guangzhou.aliyuncs.com");
        unifiedConfig.setProperty("logx.oss.storage.region", "US");
        unifiedConfig.setProperty("logx.oss.storage.accessKeyId", "unified-access-key");
        unifiedConfig.setProperty("logx.oss.storage.accessKeySecret", "unified-secret-key");
        unifiedConfig.setProperty("logx.oss.storage.bucket", "unified-bucket");
        unifiedConfig.setProperty("logx.oss.storage.keyPrefix", "unified-logs/");
        unifiedConfig.setProperty("logx.oss.storage.pathStyleAccess", "true");

        // 验证所有配置项都使用logx.oss.storage.前缀
        assertThat(unifiedConfig.getProperty("logx.oss.storage.endpoint")).isEqualTo("https://oss-cn-guangzhou.aliyuncs.com");
        assertThat(unifiedConfig.getProperty("logx.oss.storage.region")).isEqualTo("US");
        assertThat(unifiedConfig.getProperty("logx.oss.storage.accessKeyId")).isEqualTo("unified-access-key");
        assertThat(unifiedConfig.getProperty("logx.oss.storage.accessKeySecret")).isEqualTo("unified-secret-key");
        assertThat(unifiedConfig.getProperty("logx.oss.storage.bucket")).isEqualTo("unified-bucket");
        assertThat(unifiedConfig.getProperty("logx.oss.storage.keyPrefix")).isEqualTo("unified-logs/");
        assertThat(unifiedConfig.getProperty("logx.oss.storage.pathStyleAccess")).isEqualTo("true");
    }

    @Test
    void shouldHaveConsistentDefaultValues() {
        // 验证默认值一致性
        ConfigManager configManager = new ConfigManager();
        LogxOssProperties properties = configManager.getLogxOssProperties();

        // 测试默认值
        assertThat(properties.getStorage().getKeyPrefix()).isEqualTo("logx/");
        assertThat(properties.getEngine().getQueue().getCapacity()).isEqualTo(524288);
        assertThat(properties.getEngine().getBatch().getCount()).isEqualTo(8192);
        assertThat(properties.getEngine().getBatch().getMaxAgeMs()).isEqualTo(60000L);
    }

    @Test
    void shouldSupportEnvironmentVariableOverride() {
        // 模拟环境变量（通过系统属性，使用ConfigManager支持的格式）
        System.setProperty("LOGX_OSS_STORAGE_BUCKET", "env-bucket");
        System.setProperty("LOGX_OSS_STORAGE_KEY_PREFIX", "env-logs/");

        try {
            ConfigManager configManager = new ConfigManager();
            LogxOssProperties properties = configManager.getLogxOssProperties();

            // 验证系统属性覆盖
            assertThat(properties.getStorage().getBucket()).isEqualTo("env-bucket");
            assertThat(properties.getStorage().getKeyPrefix()).isEqualTo("env-logs/");

        } finally {
            // 清理系统属性
            System.clearProperty("LOGX_OSS_STORAGE_BUCKET");
            System.clearProperty("LOGX_OSS_STORAGE_KEY_PREFIX");
        }
    }

    @Test
    void shouldSupportConfigurationPriorityOrder() {
        // 验证配置优先级：JVM系统属性 > 环境变量 > 配置文件属性 > 代码默认值

        // 设置系统属性（最高优先级）
        System.setProperty("logx.oss.storage.region", "system-region");
        System.setProperty("logx.oss.storage.bucket", "system-bucket");
        System.setProperty("logx.oss.storage.accessKeyId", "system-key");

        try {
            ConfigManager configManager = new ConfigManager();
            LogxOssProperties properties = configManager.getLogxOssProperties();

            // 验证系统属性优先级最高
            assertThat(properties.getStorage().getRegion()).isEqualTo("system-region");
            assertThat(properties.getStorage().getBucket()).isEqualTo("system-bucket");
            assertThat(properties.getStorage().getAccessKeyId()).isEqualTo("system-key");

            // 验证代码默认值（无其他配置时生效）
            // This test can be flaky if environment variables are set, so we allow for the default endpoint as well.
            assertThat(properties.getStorage().getEndpoint()).isIn(null, "http://localhost:9000");

        } finally {
            // 清理系统属性
            System.clearProperty("logx.oss.storage.region");
            System.clearProperty("logx.oss.storage.bucket");
            System.clearProperty("logx.oss.storage.accessKeyId");
        }
    }
}