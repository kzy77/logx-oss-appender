package org.logx.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * ConfigManager测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class ConfigManagerTest {

    private ConfigManager configManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // 清除系统属性，避免测试间相互影响
        System.clearProperty("test.property");
        System.clearProperty("s3.region");
        configManager = new ConfigManager();
    }

    @Test
    void shouldReturnNullForNonExistentProperty() {
        assertThat(configManager.getProperty("non.existent.property")).isNull();
    }

    @Test
    void shouldReturnDefaultValueForNonExistentProperty() {
        String defaultValue = "default-value";
        assertThat(configManager.getProperty("non.existent.property", defaultValue)).isEqualTo(defaultValue);
    }

    @Test
    void shouldReturnSystemPropertyWithHighestPriority() {
        // 设置系统属性
        System.setProperty("test.property", "system-value");

        // 设置默认值
        System.setProperty("test.property", "default-value");

        assertThat(configManager.getProperty("test.property")).isEqualTo("system-value");
    }

    @Test
    void shouldReturnDefaultValueWhenNoOtherSourcesAvailable() {
        String defaultValue = "default-value";
        System.setProperty("test.property", defaultValue);

        assertThat(configManager.getProperty("test.property")).isEqualTo(defaultValue);
    }

    @Test
    void shouldReturnIntPropertyCorrectly() {
        System.setProperty("int.property", "42");

        assertThat(configManager.getIntProperty("int.property", 0)).isEqualTo(42);
    }

    @Test
    void shouldReturnDefaultIntValueForNonExistentProperty() {
        assertThat(configManager.getIntProperty("non.existent.int", 99)).isEqualTo(99);
    }

    @Test
    void shouldThrowExceptionForInvalidIntProperty() {
        System.setProperty("invalid.int", "not-a-number");

        assertThatThrownBy(() -> configManager.getIntProperty("invalid.int", 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid integer value");
    }

    @Test
    void shouldReturnLongPropertyCorrectly() {
        System.setProperty("long.property", "123456789");

        assertThat(configManager.getLongProperty("long.property", 0L)).isEqualTo(123456789L);
    }

    @Test
    void shouldReturnDefaultLongValueForNonExistentProperty() {
        assertThat(configManager.getLongProperty("non.existent.long", 999L)).isEqualTo(999L);
    }

    @Test
    void shouldThrowExceptionForInvalidLongProperty() {
        System.setProperty("invalid.long", "not-a-number");

        assertThatThrownBy(() -> configManager.getLongProperty("invalid.long", 0L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid long value");
    }

    @Test
    void shouldReturnBooleanPropertyCorrectly() {
        System.setProperty("bool.true", "true");
        System.setProperty("bool.false", "false");
        System.setProperty("bool.yes", "yes");
        System.setProperty("bool.no", "no");
        System.setProperty("bool.one", "1");
        System.setProperty("bool.zero", "0");

        assertThat(configManager.getBooleanProperty("bool.true", false)).isTrue();
        assertThat(configManager.getBooleanProperty("bool.false", true)).isFalse();
        assertThat(configManager.getBooleanProperty("bool.yes", false)).isTrue();
        assertThat(configManager.getBooleanProperty("bool.no", true)).isFalse();
        assertThat(configManager.getBooleanProperty("bool.one", false)).isTrue();
        assertThat(configManager.getBooleanProperty("bool.zero", true)).isFalse();
    }

    @Test
    void shouldReturnDefaultBooleanValueForNonExistentProperty() {
        assertThat(configManager.getBooleanProperty("non.existent.bool", true)).isTrue();
        assertThat(configManager.getBooleanProperty("non.existent.bool", false)).isFalse();
    }

    @Test
    void shouldLoadPropertiesFromFile() throws IOException {
        // 创建临时配置文件
        Path configFile = tempDir.resolve("test.properties");
        Files.write(configFile, "file.property=file-value\nother.property=other-value".getBytes());

        // 使用自定义配置文件创建ConfigManager
        ConfigManager fileConfigManager = new ConfigManager(configFile.toString());

        assertThat(fileConfigManager.getProperty("file.property")).isEqualTo("file-value");
        assertThat(fileConfigManager.getProperty("other.property")).isEqualTo("other-value");
    }

    @Test
    void shouldHandleNonExistentConfigFile() {
        // 使用不存在的配置文件路径
        ConfigManager fileConfigManager = new ConfigManager("/non/existent/config.properties");

        // 应该能正常工作，只是没有从文件读取到属性
        assertThat(fileConfigManager.getProperty("non.existent")).isNull();
    }

    @Test
    void shouldClearCacheCorrectly() {
        System.setProperty("cached.property", "default-value");

        // 首次访问，值应该被缓存
        assertThat(configManager.getProperty("cached.property")).isEqualTo("default-value");

        // 设置系统属性
        System.setProperty("cached.property", "system-value");

        // 清除缓存前，仍然返回缓存的值
        assertThat(configManager.getProperty("cached.property")).isEqualTo("default-value");

        // 清除缓存后，应该返回系统属性的值
        configManager.clearCache();
        assertThat(configManager.getProperty("cached.property")).isEqualTo("system-value");
    }

    @Test
    void shouldReloadConfigurationCorrectly() throws IOException {
        // 创建临时配置文件
        Path configFile = tempDir.resolve("reload-test.properties");
        Files.write(configFile, "reload.property=initial-value".getBytes());

        ConfigManager fileConfigManager = new ConfigManager(configFile.toString());
        assertThat(fileConfigManager.getProperty("reload.property")).isEqualTo("initial-value");

        // 修改配置文件
        Files.write(configFile, "reload.property=updated-value".getBytes());

        // 重新加载
        fileConfigManager.reload();
        assertThat(fileConfigManager.getProperty("reload.property")).isEqualTo("updated-value");
    }

    @Test
    void shouldGetAllPropertiesCorrectly() {
        System.setProperty("default.prop", "default-value");
        System.setProperty("system.prop", "system-value");

        Map<String, String> allProperties = configManager.getAllProperties();

        assertThat(allProperties).containsEntry("default.prop", "default-value");
        assertThat(allProperties).containsEntry("system.prop", "system-value");
    }

    @Test
    void shouldHandleNullAndEmptyKeys() {
        assertThat(configManager.getProperty(null)).isNull();
        assertThat(configManager.getProperty("")).isNull();
        assertThat(configManager.getProperty("   ")).isNull();
    }

    @Test
    void shouldSupportUppercaseUnderscoreStyleForSystemProperties() {
        // 设置大写下划线风格的系统属性
        System.setProperty("LOGX_OSS_ENDPOINT", "http://uppercase-endpoint.example.com");

        // 使用标准点号格式查询应该能获取到大写下划线格式的值
        assertThat(configManager.getProperty("logx.oss.endpoint")).isEqualTo("http://uppercase-endpoint.example.com");

        // 清理
        System.clearProperty("LOGX_OSS_ENDPOINT");
    }

    @Test
    void shouldPreferDotStyleOverUppercaseStyleForSystemProperties() {
        // 同时设置点号格式和大写下划线格式
        System.setProperty("logx.oss.endpoint", "http://dot-style.example.com");
        System.setProperty("LOGX_OSS_ENDPOINT", "http://uppercase-style.example.com");

        // 应该优先使用点号格式的值
        assertThat(configManager.getProperty("logx.oss.endpoint")).isEqualTo("http://dot-style.example.com");

        // 清理
        System.clearProperty("logx.oss.endpoint");
        System.clearProperty("LOGX_OSS_ENDPOINT");
    }


    @Test
    void shouldSupportBothStylesInMixedScenarios() throws IOException {
        // 创建临时配置文件
        Path configFile = tempDir.resolve("mixed-style.properties");
        Files.write(configFile, "logx.oss.endpoint=http://file-endpoint.example.com".getBytes());

        ConfigManager fileConfigManager = new ConfigManager(configFile.toString());

        // 默认情况：从文件读取
        assertThat(fileConfigManager.getProperty("logx.oss.endpoint")).isEqualTo("http://file-endpoint.example.com");

        // 设置系统属性（大写下划线格式），应该覆盖文件值
        System.setProperty("LOGX_OSS_ENDPOINT", "http://system-uppercase.example.com");
        fileConfigManager.clearCache();
        assertThat(fileConfigManager.getProperty("logx.oss.endpoint"))
                .isEqualTo("http://system-uppercase.example.com");

        // 设置系统属性（点号格式），应该覆盖大写下划线格式
        System.setProperty("logx.oss.endpoint", "http://system-dot.example.com");
        fileConfigManager.clearCache();
        assertThat(fileConfigManager.getProperty("logx.oss.endpoint")).isEqualTo("http://system-dot.example.com");

        // 清理
        System.clearProperty("LOGX_OSS_ENDPOINT");
        System.clearProperty("logx.oss.endpoint");
    }

    @Test
    void shouldRespectPriorityChainWithBothStyles() throws IOException {
        // 创建临时配置文件
        Path configFile = tempDir.resolve("priority-styles.properties");
        Files.write(configFile, "logx.oss.region=file-region".getBytes());

        ConfigManager fileConfigManager = new ConfigManager(configFile.toString());

        // 1. 默认情况：从文件读取
        assertThat(fileConfigManager.getProperty("logx.oss.region")).isEqualTo("file-region");

        // 2. 设置系统属性（大写下划线格式），应该覆盖文件值
        System.setProperty("LOGX_OSS_REGION", "system-uppercase-region");
        fileConfigManager.clearCache();
        assertThat(fileConfigManager.getProperty("logx.oss.region")).isEqualTo("system-uppercase-region");

        // 3. 设置系统属性（点号格式），应该优先于大写下划线格式
        System.setProperty("logx.oss.region", "system-dot-region");
        fileConfigManager.clearCache();
        assertThat(fileConfigManager.getProperty("logx.oss.region")).isEqualTo("system-dot-region");

        // 清理
        System.clearProperty("LOGX_OSS_REGION");
        System.clearProperty("logx.oss.region");
    }

    @Test
    void shouldHandleCamelCaseInSystemProperties() {
        // 设置驼峰命名的配置键，使用大写下划线格式的系统属性
        System.setProperty("LOGX_OSS_ACCESS_KEY_ID", "test-access-key");

        // 使用驼峰格式查询应该能获取到值
        assertThat(configManager.getProperty("logx.oss.accessKeyId")).isEqualTo("test-access-key");

        // 清理
        System.clearProperty("LOGX_OSS_ACCESS_KEY_ID");
    }

    @Test
    void shouldHandleCamelCaseWithDotStylePriority() {
        // 同时设置驼峰格式和大写下划线格式
        System.setProperty("logx.oss.accessKeyId", "camel-case-value");
        System.setProperty("LOGX_OSS_ACCESS_KEY_ID", "uppercase-value");

        // 应该优先使用驼峰格式的值
        assertThat(configManager.getProperty("logx.oss.accessKeyId")).isEqualTo("camel-case-value");

        // 清理
        System.clearProperty("logx.oss.accessKeyId");
        System.clearProperty("LOGX_OSS_ACCESS_KEY_ID");
    }

    @Test
    void shouldConvertCamelCaseCorrectly() {
        // 测试多个驼峰命名的转换
        System.setProperty("LOGX_OSS_MAX_BATCH_COUNT", "5000");
        System.setProperty("LOGX_OSS_ACCESS_KEY_SECRET", "secret-value");
        System.setProperty("LOGX_OSS_ENABLE_SSL", "false");

        // 验证所有驼峰命名都能正确转换和查询
        assertThat(configManager.getProperty("logx.oss.maxBatchCount")).isEqualTo("5000");
        assertThat(configManager.getProperty("logx.oss.accessKeySecret")).isEqualTo("secret-value");
        assertThat(configManager.getProperty("logx.oss.enableSsl")).isEqualTo("false");

        // 清理
        System.clearProperty("LOGX_OSS_MAX_BATCH_COUNT");
        System.clearProperty("LOGX_OSS_ACCESS_KEY_SECRET");
        System.clearProperty("LOGX_OSS_ENABLE_SSL");
    }
}
