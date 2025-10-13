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
        // 清除可能存在的JVM系统属性
        System.clearProperty("logx.oss.region");
        System.clearProperty("LOGX_OSS_REGION");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("logx.oss.region");
        System.clearProperty("LOGX_OSS_REGION");
        System.clearProperty("logx.oss.endpoint");
        System.clearProperty("BASE_URL");
    }

    @Test
    void testXmlDefaultValuePriorityOverConfigManagerDefaults() {
        // 场景：XML中使用了 ${LOGX_OSS_REGION:-cn-beijing}，但环境变量未设置
        // 模拟Logback解析后传给setter的值是 "cn-beijing"（XML中的默认值）
        String xmlDefaultValue = "cn-beijing";

        // 模拟resolveStringConfig的逻辑
        String finalValue = resolveStringConfig(configManager, "logx.oss.region", xmlDefaultValue);

        // 期望：XML明确配置的默认值应该优先于ConfigManager的默认值
        assertThat(finalValue)
            .as("XML明确配置的默认值应该优先于ConfigManager默认值")
            .isEqualTo("cn-beijing");
    }

    @Test
    void testEnvironmentVariableOverridesXmlDefault() {
        // 设置环境变量（模拟：实际环境变量无法在单元测试中设置，用JVM系统属性代替）
        System.setProperty("LOGX_OSS_REGION", "us-west-1");

        String xmlDefaultValue = "cn-beijing";
        String finalValue = resolveStringConfig(configManager, "logx.oss.region", xmlDefaultValue);

        // 期望：环境变量应该优先于XML默认值
        assertThat(finalValue).isEqualTo("us-west-1");
    }

    @Test
    void testJvmPropertyOverridesAll() {
        // 设置JVM系统属性
        System.setProperty("logx.oss.region", "eu-central-1");

        String xmlDefaultValue = "cn-beijing";
        String finalValue = resolveStringConfig(configManager, "logx.oss.region", xmlDefaultValue);

        // 期望：JVM系统属性应该是最高优先级
        assertThat(finalValue).isEqualTo("eu-central-1");
    }

    @Test
    void testFallbackToConfigManagerDefaultsWhenXmlValueIsEmpty() {
        // 场景：XML中没有设置默认值（传入null或空字符串）
        // 应该回退到ConfigManager的默认值
        String xmlDefaultValue = null;

        String finalValue = resolveStringConfig(configManager, "logx.oss.region", xmlDefaultValue);

        // 期望：应该使用ConfigManager默认值
        assertThat(finalValue).isEqualTo("ap-guangzhou");
    }

    @Test
    void testResolvePlaceholderInXmlValue() {
        // 场景：XML中使用占位符语法 ${LOGX_OSS_REGION:-cn-beijing}
        // 模拟Logback未替换占位符，由代码手动解析
        String xmlValueWithPlaceholder = "${LOGX_OSS_REGION:-cn-beijing}";

        String finalValue = resolveStringConfig(configManager, "logx.oss.region", xmlValueWithPlaceholder);

        // 期望：应该解析占位符并使用默认值cn-beijing
        assertThat(finalValue)
            .as("应该自动解析XML中的占位符")
            .isEqualTo("cn-beijing");
    }

    @Test
    void testResolvePlaceholderWithEnvironmentVariable() {
        // 场景：XML中使用占位符，且环境变量存在（用JVM系统属性模拟）
        System.setProperty("LOGX_OSS_REGION", "eu-west-1");

        String xmlValueWithPlaceholder = "${LOGX_OSS_REGION:-cn-beijing}";
        String finalValue = resolveStringConfig(configManager, "logx.oss.region", xmlValueWithPlaceholder);

        // 期望：应该使用环境变量的值而不是默认值
        assertThat(finalValue)
            .as("环境变量应该优先于占位符中的默认值")
            .isEqualTo("eu-west-1");
    }

    @Test
    void testResolvePlaceholderInHighPriorityConfig() {
        // 场景：高优先级配置（JVM系统属性）本身包含占位符
        // 模拟: -Dlogx.oss.endpoint='${BASE_URL:-http://localhost:9000}'
        System.setProperty("logx.oss.endpoint", "${BASE_URL:-http://localhost:9000}");

        String xmlValue = "http://default-endpoint.com";
        String finalValue = resolveStringConfig(configManager, "logx.oss.endpoint", xmlValue);

        // 期望：高优先级配置中的占位符应该被解析，使用默认值http://localhost:9000
        assertThat(finalValue)
            .as("高优先级配置中的占位符应该被正确解析")
            .isEqualTo("http://localhost:9000");
    }

    @Test
    void testResolvePlaceholderInHighPriorityConfigWithNestedVariable() {
        // 场景：高优先级配置包含占位符，且占位符引用的变量也存在
        System.setProperty("BASE_URL", "https://production.example.com");
        System.setProperty("logx.oss.endpoint", "${BASE_URL:-http://localhost:9000}");

        String xmlValue = "http://default-endpoint.com";
        String finalValue = resolveStringConfig(configManager, "logx.oss.endpoint", xmlValue);

        // 期望：应该使用BASE_URL环境变量的值
        assertThat(finalValue)
            .as("高优先级配置中的占位符应该引用其他环境变量")
            .isEqualTo("https://production.example.com");
    }

    /**
     * 模拟LogbackOSSAppender中的resolveStringConfig方法
     * <p>
     * 优先级顺序：JVM系统属性 > 环境变量 > 配置文件 > XML明确配置的值 > ConfigManager默认值
     * <p>
     * 支持所有配置源中的${ENV:-default}占位符语法
     */
    private String resolveStringConfig(ConfigManager configManager, String configKey, String xmlValue) {
        // 优先级1: 从高优先级配置源获取值（JVM系统属性、环境变量、配置文件）
        String value = configManager.getPropertyWithoutDefaults(configKey);
        if (value != null && !value.trim().isEmpty()) {
            // 高优先级配置也可能包含占位符，需要解析
            String resolvedValue = configManager.resolvePlaceholders(value);
            if (resolvedValue != null && !resolvedValue.trim().isEmpty()) {
                return resolvedValue;
            }
        }

        // 优先级2: 解析XML配置的值（支持${ENV:-default}语法）
        if (xmlValue != null && !xmlValue.trim().isEmpty()) {
            String resolvedXmlValue = configManager.resolvePlaceholders(xmlValue);
            if (resolvedXmlValue != null && !resolvedXmlValue.trim().isEmpty()) {
                return resolvedXmlValue;
            }
        }

        // 优先级3: 回退到ConfigManager默认值
        return configManager.getProperty(configKey);
    }
}
