package org.logx.config.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;
import org.logx.storage.StorageConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConfigFactory测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class ConfigFactoryTest {

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
        // 清除系统属性以避免测试间干扰
        System.clearProperty("logx.oss.storage.accessKeyId");
        System.clearProperty("logx.oss.storage.accessKeySecret");
        System.clearProperty("logx.oss.storage.bucket");
        System.clearProperty("logx.oss.storage.endpoint");
        System.clearProperty("logx.oss.storage.region");
        System.clearProperty("logx.oss.storage.pathStyleAccess");
    }

    @Test
    void shouldCreateAwsS3ConfigWithDefaults() {
        // 设置最少必需的配置
        System.setProperty("logx.oss.storage.accessKeyId", "AKIAIOSFODNN7EXAMPLE");
        System.setProperty("logx.oss.storage.accessKeySecret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        System.setProperty("logx.oss.storage.bucket", "my-test-bucket");
        System.setProperty("logx.oss.storage.ossType", "aws_s3");

        LogxOssProperties properties = configManager.getLogxOssProperties();
        StorageConfig config = new StorageConfig(properties);

        assertThat(config.getEndpoint()).isEqualTo("s3.amazonaws.com");
        assertThat(config.getRegion()).isNull();
        assertThat(config.getAccessKeyId()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
        assertThat(config.getAccessKeySecret()).isEqualTo("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        assertThat(config.getBucket()).isEqualTo("my-test-bucket");
        assertThat(config.isPathStyleAccess()).isFalse();
    }

    @Test
    void shouldCreateAwsS3ConfigWithCustomValues() {
        // 设置自定义配置
        System.setProperty("logx.oss.storage.endpoint", "https://s3.eu-west-1.amazonaws.com");
        System.setProperty("logx.oss.storage.region", "eu-west-1");
        System.setProperty("logx.oss.storage.accessKeyId", "CUSTOM_ACCESS_KEY");
        System.setProperty("logx.oss.storage.accessKeySecret", "CUSTOM_SECRET_KEY");
        System.setProperty("logx.oss.storage.bucket", "custom-bucket");
        System.setProperty("logx.oss.storage.pathStyleAccess", "true");

        LogxOssProperties properties = configManager.getLogxOssProperties();
        StorageConfig config = new StorageConfig(properties);

        assertThat(config.getEndpoint()).isEqualTo("https://s3.eu-west-1.amazonaws.com");
        assertThat(config.getRegion()).isEqualTo("eu-west-1");
        assertThat(config.getAccessKeyId()).isEqualTo("CUSTOM_ACCESS_KEY");
        assertThat(config.getAccessKeySecret()).isEqualTo("CUSTOM_SECRET_KEY");
        assertThat(config.getBucket()).isEqualTo("custom-bucket");
        assertThat(config.isPathStyleAccess()).isTrue();
    }

    @Test
    void shouldCreateMinioConfigWithDefaults() {
        // 设置必需的MinIO配置
        System.setProperty("logx.oss.storage.endpoint", "http://localhost:9000");
        System.setProperty("logx.oss.storage.accessKeyId", "minioadmin");
        System.setProperty("logx.oss.storage.accessKeySecret", "minioadmin");
        System.setProperty("logx.oss.storage.bucket", "logs");
        System.setProperty("logx.oss.storage.ossType", "minio");

        LogxOssProperties properties = configManager.getLogxOssProperties();
        StorageConfig config = new StorageConfig(properties);

        assertThat(config.getEndpoint()).isEqualTo("http://localhost:9000");
        assertThat(config.getRegion()).isNull();
        assertThat(config.getAccessKeyId()).isEqualTo("minioadmin");
        assertThat(config.getAccessKeySecret()).isEqualTo("minioadmin");
        assertThat(config.getBucket()).isEqualTo("logs");
        // MinIO默认使用路径风格
        assertThat(config.isPathStyleAccess()).isTrue();
    }
}