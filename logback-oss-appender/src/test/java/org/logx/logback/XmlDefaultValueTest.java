package org.logx.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.config.ConfigManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试XML默认值语法 ${ENV:-defaultValue} 的解析优先级
 */
class XmlDefaultValueTest {

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
        System.clearProperty("logx.oss.storage.region");
        System.clearProperty("LOGX_OSS_STORAGE_REGION");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("logx.oss.storage.region");
        System.clearProperty("LOGX_OSS_STORAGE_REGION");
        System.clearProperty("logx.oss.storage.endpoint");
        System.clearProperty("BASE_URL");
    }

    @Test
    void testXmlDefaultValuePriorityOverConfigManagerDefaults() {
        String xmlDefaultValue = "cn-beijing";

        String finalValue = configManager.getProperty("logx.oss.storage.region", xmlDefaultValue);

        // 期望：XML明确配置的默认值应该优先于ConfigManager的默认值
        assertThat(finalValue)
            .as("XML明确配置的默认值应该优先于ConfigManager默认值")
            .isEqualTo("cn-beijing");
    }

    @Test
    void testEnvironmentVariableOverridesXmlDefault() {
        System.setProperty("LOGX_OSS_STORAGE_REGION", "us-west-1");

        String xmlDefaultValue = "cn-beijing";
        String finalValue = configManager.getProperty("logx.oss.storage.region", xmlDefaultValue);

        assertThat(finalValue).isEqualTo("us-west-1");
    }

    @Test
    void testJvmPropertyOverridesAll() {
        System.setProperty("logx.oss.storage.region", "eu-central-1");

        String xmlDefaultValue = "cn-beijing";
        String finalValue = configManager.getProperty("logx.oss.storage.region", xmlDefaultValue);

        assertThat(finalValue).isEqualTo("eu-central-1");
    }

    @Test
    void testResolvePlaceholderInXmlValue() {
        String xmlValueWithPlaceholder = "${LOGX_OSS_STORAGE_REGION:-cn-beijing}";

        String finalValue = configManager.resolvePlaceholders(xmlValueWithPlaceholder);

        assertThat(finalValue)
            .as("应该自动解析XML中的占位符")
            .isEqualTo("cn-beijing");
    }

    @Test
    void testResolvePlaceholderWithEnvironmentVariable() {
        System.setProperty("LOGX_OSS_STORAGE_REGION", "eu-west-1");

        String xmlValueWithPlaceholder = "${LOGX_OSS_STORAGE_REGION:-cn-beijing}";
        String finalValue = configManager.resolvePlaceholders(xmlValueWithPlaceholder);

        assertThat(finalValue)
            .as("环境变量应该优先于占位符中的默认值")
            .isEqualTo("eu-west-1");
    }

    @Test
    void testResolvePlaceholderInHighPriorityConfig() {
        System.setProperty("logx.oss.storage.endpoint", "${BASE_URL:-http://localhost:9000}");

        String xmlValue = "http://default-endpoint.com";
        String finalValue = configManager.getProperty("logx.oss.storage.endpoint", xmlValue);

        assertThat(finalValue)
            .as("高优先级配置中的占位符应该被正确解析")
            .isEqualTo("http://localhost:9000");
    }

    @Test
    void testResolvePlaceholderInHighPriorityConfigWithNestedVariable() {
        System.setProperty("BASE_URL", "https://production.example.com");
        System.setProperty("logx.oss.storage.endpoint", "${BASE_URL:-http://localhost:9000}");

        String xmlValue = "http://default-endpoint.com";
        String finalValue = configManager.getProperty("logx.oss.storage.endpoint", xmlValue);

        assertThat(finalValue)
            .as("高优先级配置中的占位符应该引用其他环境变量")
            .isEqualTo("https://production.example.com");
    }
}