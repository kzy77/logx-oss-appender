package org.logx.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试ConfigManager的占位符解析功能
 */
class PlaceholderResolutionTest {

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
        // 清除可能存在的JVM系统属性
        System.clearProperty("TEST_VAR");
        System.clearProperty("LOGX_OSS_REGION");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("TEST_VAR");
        System.clearProperty("LOGX_OSS_REGION");
    }

    @Test
    void testResolvePlaceholderWithBashStyleDefault() {
        // 测试bash风格的默认值语法 ${VAR:-default}
        String input = "${LOGX_OSS_REGION:-us}";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("应该使用默认值ap-guangzhou")
            .isEqualTo("us");
    }

    @Test
    void testResolvePlaceholderWithSimpleStyleDefault() {
        // 测试简化风格的默认值语法 ${VAR:default}
        String input = "${LOGX_OSS_REGION:cn-beijing}";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("应该使用默认值cn-beijing")
            .isEqualTo("cn-beijing");
    }

    @Test
    void testResolvePlaceholderWithJvmSystemProperty() {
        // 设置JVM系统属性
        System.setProperty("LOGX_OSS_REGION", "us-west-1");

        String input = "${LOGX_OSS_REGION:-us}";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("应该使用JVM系统属性的值")
            .isEqualTo("us-west-1");
    }

    @Test
    void testResolvePlaceholderWithoutDefault() {
        // 测试没有默认值的占位符
        String input = "${LOGX_OSS_REGION}";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("没有默认值且变量不存在时应该返回空字符串")
            .isEqualTo("");
    }

    @Test
    void testResolveMultiplePlaceholders() {
        // 测试多个占位符
        System.setProperty("LOGX_OSS_BUCKET", "my-bucket");

        String input = "s3://${LOGX_OSS_BUCKET:-default-bucket}/${LOGX_OSS_KEY_PREFIX:-logs/}";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("应该正确解析多个占位符")
            .isEqualTo("s3://my-bucket/logs/");
    }

    @Test
    void testResolveNonPlaceholderString() {
        // 测试不包含占位符的字符串
        String input = "us";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("不包含占位符的字符串应该原样返回")
            .isEqualTo("us");
    }

    @Test
    void testResolveNullString() {
        // 测试null值
        String result = configManager.resolvePlaceholders(null);

        assertThat(result)
            .as("null应该返回null")
            .isNull();
    }

    @Test
    void testResolveEmptyString() {
        // 测试空字符串
        String input = "";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("空字符串应该返回空字符串")
            .isEmpty();
    }

    @Test
    void testResolvePlaceholderWithEmptyDefault() {
        // 测试空默认值
        String input = "${LOGX_OSS_REGION:-}";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("默认值为空时应该返回空字符串")
            .isEmpty();
    }

    @Test
    void testResolvePlaceholderWithSpecialCharactersInDefault() {
        // 测试默认值包含特殊字符
        String input = "${LOGX_OSS_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}";
        String result = configManager.resolvePlaceholders(input);

        assertThat(result)
            .as("应该正确处理包含特殊字符的默认值")
            .isEqualTo("https://oss-cn-hangzhou.aliyuncs.com");
    }
}
