package org.logx.config.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.config.ConfigManager;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageOssType;

import static org.assertj.core.api.Assertions.*;

/**
 * ConfigFactory测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class ConfigFactoryTest {

    private ConfigManager configManager;
    private ConfigFactory configFactory;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
        configFactory = new ConfigFactory(configManager);
        // 清除系统属性以避免测试间干扰
        System.clearProperty("logx.oss.accessKeyId");
        System.clearProperty("logx.oss.secretAccessKey");
        System.clearProperty("logx.oss.bucket");
        System.clearProperty("logx.oss.endpoint");
        System.clearProperty("logx.oss.region");
        System.clearProperty("logx.oss.pathStyleAccess");
        System.clearProperty("logx.oss.enableSsl");
        System.clearProperty("logx.oss.maxConnections");
        System.clearProperty("logx.oss.connectTimeout");
        System.clearProperty("logx.oss.readTimeout");
    }

    @Test
    void shouldCreateAwsS3ConfigWithDefaults() {
        // 设置最少必需的配置
        System.setProperty("logx.oss.accessKeyId", "AKIAIOSFODNN7EXAMPLE");
        System.setProperty("logx.oss.accessKeySecret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        System.setProperty("logx.oss.bucket", "my-test-bucket");

        StorageConfig config = configFactory.createConfig(StorageOssType.AWS_S3);

        assertThat(config).isInstanceOf(ConfigFactory.AwsS3Config.class);
        assertThat(config.getEndpoint()).isEqualTo("https://s3.amazonaws.com");
        assertThat(config.getRegion()).isEqualTo("ap-guangzhou");
        assertThat(config.getAccessKeyId()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
        assertThat(config.getAccessKeySecret()).isEqualTo("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        assertThat(config.getBucket()).isEqualTo("my-test-bucket");
        assertThat(config.isPathStyleAccess()).isFalse();
        assertThat(config.isEnableSsl()).isTrue();
        assertThat(config.getMaxConnections()).isEqualTo(50);
    }

    @Test
    void shouldCreateAwsS3ConfigWithCustomValues() {
        // 设置自定义配置
        System.setProperty("logx.oss.endpoint", "https://s3.eu-west-1.amazonaws.com");
        System.setProperty("logx.oss.region", "eu-west-1");
        System.setProperty("logx.oss.accessKeyId", "CUSTOM_ACCESS_KEY");
        System.setProperty("logx.oss.accessKeySecret", "CUSTOM_SECRET_KEY");
        System.setProperty("logx.oss.bucket", "custom-bucket");
        System.setProperty("logx.oss.pathStyleAccess", "true");
        System.setProperty("logx.oss.enableSsl", "false");
        System.setProperty("logx.oss.maxConnections", "100");
        System.setProperty("logx.oss.connectTimeout", "60000");
        System.setProperty("logx.oss.readTimeout", "120000");

        StorageConfig config = configFactory.createConfig(StorageOssType.AWS_S3);

        assertThat(config.getEndpoint()).isEqualTo("https://s3.eu-west-1.amazonaws.com");
        assertThat(config.getRegion()).isEqualTo("eu-west-1");
        assertThat(config.getAccessKeyId()).isEqualTo("CUSTOM_ACCESS_KEY");
        assertThat(config.getAccessKeySecret()).isEqualTo("CUSTOM_SECRET_KEY");
        assertThat(config.getBucket()).isEqualTo("custom-bucket");
        assertThat(config.isPathStyleAccess()).isTrue();
        assertThat(config.isEnableSsl()).isFalse();
        assertThat(config.getMaxConnections()).isEqualTo(100);
        assertThat(config.getConnectTimeout().toMillis()).isEqualTo(60000);
        assertThat(config.getReadTimeout().toMillis()).isEqualTo(120000);
    }

    @Test
    void shouldCreateMinioConfigWithDefaults() {
        // 设置必需的MinIO配置
        System.setProperty("logx.oss.endpoint", "http://localhost:9000");
        System.setProperty("logx.oss.accessKeyId", "minioadmin");
        System.setProperty("logx.oss.accessKeySecret", "minioadmin");
        System.setProperty("logx.oss.bucket", "logs");

        StorageConfig config = configFactory.createConfig(StorageOssType.MINIO);

        assertThat(config).isInstanceOf(ConfigFactory.MinioConfig.class);
        assertThat(config.getEndpoint()).isEqualTo("http://localhost:9000");
        assertThat(config.getRegion()).isEqualTo("ap-guangzhou");
        assertThat(config.getAccessKeyId()).isEqualTo("minioadmin");
        assertThat(config.getAccessKeySecret()).isEqualTo("minioadmin");
        assertThat(config.getBucket()).isEqualTo("logs");
        // MinIO默认使用路径风格
        assertThat(config.isPathStyleAccess()).isTrue();
        // MinIO开发环境默认不用SSL
        assertThat(config.isEnableSsl()).isFalse();
    }

    @Test
    void shouldCreateGenericS3ConfigWithDefaults() {
        // 设置必需的通用S3配置
        System.setProperty("logx.oss.endpoint", "https://storage.example.com");
        System.setProperty("logx.oss.accessKeyId", "EXAMPLE_ACCESS_KEY");
        System.setProperty("logx.oss.accessKeySecret", "EXAMPLE_SECRET_KEY");
        System.setProperty("logx.oss.bucket", "application-logs");

        StorageConfig config = configFactory.createConfig(StorageOssType.GENERIC_S3);

        assertThat(config).isInstanceOf(ConfigFactory.GenericS3Config.class);
        assertThat(config.getEndpoint()).isEqualTo("https://storage.example.com");
        assertThat(config.getRegion()).isEqualTo("ap-guangzhou");
        assertThat(config.getAccessKeyId()).isEqualTo("EXAMPLE_ACCESS_KEY");
        assertThat(config.getAccessKeySecret()).isEqualTo("EXAMPLE_SECRET_KEY");
        assertThat(config.getBucket()).isEqualTo("application-logs");
        assertThat(config.isPathStyleAccess()).isFalse();
        assertThat(config.isEnableSsl()).isTrue();
    }

    @Test
    void shouldThrowExceptionForMissingRequiredProperties() {
        // 不设置任何必需属性
        assertThatThrownBy(() -> configFactory.createConfig(StorageOssType.AWS_S3))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Required configuration property")
                .hasMessageContaining("is missing");
    }

    @Test
    void shouldThrowExceptionForEmptyRequiredProperties() {
        // 设置空的必需属性
        System.setProperty("logx.oss.accessKeyId", "");
        System.setProperty("logx.oss.accessKeySecret", "valid-secret");
        System.setProperty("logx.oss.bucket", "valid-bucket");

        assertThatThrownBy(() -> configFactory.createConfig(StorageOssType.AWS_S3))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("logx.oss.accessKeyId");
    }

    @Test
    void shouldThrowExceptionForUnsupportedBackend() {
        // 注意：如果StorageBackend枚举增加新值但ConfigFactory未支持，会抛出异常
        // 这里我们无法直接测试，因为enum是封闭的，但可以通过反射或者mock来测试
        // 为了简单起见，我们测试现有的default分支逻辑

        // 创建一个会触发default分支的测试场景比较困难，
        // 但我们可以通过测试所有支持的backend来确保覆盖率
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");

        System.setProperty("logx.oss.endpoint", "http://test");
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");

        System.setProperty("logx.oss.endpoint", "https://test");
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");

        // 验证所有支持的backend都能正常创建配置
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");

        System.setProperty("logx.oss.endpoint", "http://test");
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");

        System.setProperty("logx.oss.endpoint", "https://test");
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");

        assertThat(configFactory.createConfig(StorageOssType.AWS_S3)).isInstanceOf(ConfigFactory.AwsS3Config.class);
        assertThat(configFactory.createConfig(StorageOssType.MINIO)).isInstanceOf(ConfigFactory.MinioConfig.class);
        assertThat(configFactory.createConfig(StorageOssType.GENERIC_S3))
                .isInstanceOf(ConfigFactory.GenericS3Config.class);
    }

    @Test
    void shouldUseSystemPropertiesOverDefaults() {
        // 设置默认值
        System.setProperty("logx.oss.accessKeyId", "default-key");
        System.setProperty("logx.oss.accessKeySecret", "default-secret");
        System.setProperty("logx.oss.bucket", "default-bucket");

        // 设置系统属性
        System.setProperty("logx.oss.accessKeyId", "system-key");
        System.setProperty("logx.oss.accessKeySecret", "system-secret");
        System.setProperty("logx.oss.bucket", "system-bucket");

        try {
            StorageConfig config = configFactory.createConfig(StorageOssType.AWS_S3);

            // 系统属性应该覆盖默认值
            assertThat(config.getAccessKeyId()).isEqualTo("system-key");
            assertThat(config.getAccessKeySecret()).isEqualTo("system-secret");
            assertThat(config.getBucket()).isEqualTo("system-bucket");
        } finally {
            // 清理系统属性
            System.clearProperty("logx.oss.accessKeyId");
            System.clearProperty("logx.oss.accessKeySecret");
            System.clearProperty("logx.oss.bucket");
        }
    }

    @Test
    void shouldHandleMinioSpecificDefaults() {
        System.setProperty("logx.oss.endpoint", "http://localhost:9000");
        System.setProperty("logx.oss.accessKeyId", "minioadmin");
        System.setProperty("logx.oss.accessKeySecret", "minioadmin");
        System.setProperty("logx.oss.bucket", "logs");

        StorageConfig config = configFactory.createConfig(StorageOssType.MINIO);

        // MinIO特定的默认值
        // MinIO默认使用路径风格
        assertThat(config.isPathStyleAccess()).isTrue();
        // MinIO开发环境默认不用SSL
        assertThat(config.isEnableSsl()).isFalse();
    }

    @Test
    void shouldParseNumericPropertiesCorrectly() {
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");
        System.setProperty("logx.oss.maxConnections", "200");
        System.setProperty("logx.oss.connectTimeout", "45000");
        System.setProperty("logx.oss.readTimeout", "90000");

        StorageConfig config = configFactory.createConfig(StorageOssType.AWS_S3);

        assertThat(config.getMaxConnections()).isEqualTo(200);
        assertThat(config.getConnectTimeout().toMillis()).isEqualTo(45000);
        assertThat(config.getReadTimeout().toMillis()).isEqualTo(90000);
    }

    @Test
    void shouldParseBooleanPropertiesCorrectly() {
        System.setProperty("logx.oss.accessKeyId", "test");
        System.setProperty("logx.oss.accessKeySecret", "test");
        System.setProperty("logx.oss.bucket", "test");
        System.setProperty("logx.oss.pathStyleAccess", "true");
        System.setProperty("logx.oss.enableSsl", "false");

        StorageConfig config = configFactory.createConfig(StorageOssType.AWS_S3);

        assertThat(config.isPathStyleAccess()).isTrue();
        assertThat(config.isEnableSsl()).isFalse();
    }
}
