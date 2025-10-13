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

    /**
     * 模拟LogbackOSSAppender中的resolveStringConfig方法
     * <p>
     * 优先级顺序：JVM系统属性 > 环境变量 > 配置文件 > XML明确配置的值 > ConfigManager默认值
     */
    private String resolveStringConfig(ConfigManager configManager, String configKey, String xmlValue) {
        // 使用不包含ConfigManager默认值的查询，确保XML明确配置的值优先
        String value = configManager.getPropertyWithoutDefaults(configKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        // 如果高优先级配置不存在，使用XML配置的值
        if (xmlValue != null && !xmlValue.trim().isEmpty()) {
            return xmlValue;
        }

        // 最后回退到ConfigManager默认值
        return configManager.getProperty(configKey);
    }
}
