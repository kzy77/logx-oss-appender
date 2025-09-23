package org.logx.storage.sf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * SfOssConfig配置测试
 * <p>
 * 验证SF OSS存储配置的构建、验证和属性访问功能。
 */
class SfOssConfigTest {

    @Test
    void testSfOssConfigBuilder() {
        // Given
        String endpoint = "https://sf-oss-cn-north-1.sf-oss.com";
        String region = "cn-north-1";
        String accessKeyId = "test-access-key";
        String accessKeySecret = "test-secret-key";
        String bucket = "test-bucket";

        // When
        SfOssConfig config = SfOssConfig.builder()
                .endpoint(endpoint)
                .region(region)
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .bucket(bucket)
                .pathStyleAccess(true)
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .maxConnections(100)
                .enableSsl(true)
                .build();

        // Then
        assertThat(config.getEndpoint()).isEqualTo(endpoint);
        assertThat(config.getRegion()).isEqualTo(region);
        assertThat(config.getAccessKeyId()).isEqualTo(accessKeyId);
        assertThat(config.getAccessKeySecret()).isEqualTo(accessKeySecret);
        assertThat(config.getBucket()).isEqualTo(bucket);
        assertThat(config.isPathStyleAccess()).isTrue();
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getMaxConnections()).isEqualTo(100);
        assertThat(config.isEnableSsl()).isTrue();
    }

    @Test
    void testConfigValidationWithValidConfig() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatNoException().isThrownBy(config::validateConfig);
    }

    @ParameterizedTest
    @ValueSource(strings = { "cn-north-1", "cn-south-1", "us-west-1", "eu-central-1" })
    void testConfigValidationWithValidRegions(String region) {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region(region)
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatNoException().isThrownBy(config::validateConfig);
    }

    @Test
    void testConfigValidationWithInvalidRegion() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("invalid-region")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("Invalid SF OSS region format");
    }

    @Test
    void testConfigValidationWithNullEndpoint() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint(null)
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("Endpoint cannot be null or empty");
    }

    @Test
    void testConfigValidationWithEmptyEndpoint() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("Endpoint cannot be null or empty");
    }

    @Test
    void testConfigValidationWithNullRegion() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region(null)
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("Region cannot be null or empty");
    }

    @Test
    void testConfigValidationWithEmptyRegion() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("Region cannot be null or empty");
    }

    @Test
    void testConfigValidationWithNullAccessKeyId() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId(null)
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("AccessKeyId cannot be null or empty");
    }

    @Test
    void testConfigValidationWithNullAccessKeySecret() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret(null)
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("AccessKeySecret cannot be null or empty");
    }

    @Test
    void testConfigValidationWithNullBucket() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket(null)
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("Bucket cannot be null or empty");
    }

    @Test
    void testConfigValidationWithInvalidMaxConnections() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .maxConnections(0)
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("MaxConnections must be positive");
    }

    @Test
    void testConfigValidationWithNegativeConnectTimeout() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .connectTimeout(Duration.ofSeconds(-1))
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("ConnectTimeout cannot be negative");
    }

    @Test
    void testConfigValidationWithNegativeReadTimeout() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .readTimeout(Duration.ofSeconds(-1))
                .build();

        // When & Then
        assertThatIllegalArgumentException()
                .isThrownBy(config::validateConfig)
                .withMessageContaining("ReadTimeout cannot be negative");
    }

    @Test
    void testConfigEqualsAndHashCode() {
        // Given
        SfOssConfig config1 = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        SfOssConfig config2 = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        SfOssConfig config3 = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-south-1.sf-oss.com")
                .region("cn-south-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // Then
        assertThat(config1).isEqualTo(config2);
        assertThat(config1).hasSameHashCodeAs(config2);
        assertThat(config1).isNotEqualTo(config3);
        assertThat(config1.hashCode()).isNotEqualTo(config3.hashCode());
    }

    @Test
    void testConfigToString() {
        // Given
        SfOssConfig config = SfOssConfig.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When
        String configString = config.toString();

        // Then
        assertThat(configString).contains("endpoint='https://sf-oss-cn-north-1.sf-oss.com'");
        assertThat(configString).contains("region='cn-north-1'");
        assertThat(configString).contains("accessKeyId='te****ey'");
        assertThat(configString).contains("bucket='test-bucket'");
    }

    @Test
    void testFromEnvironment() {
        // This test verifies the fromEnvironment method structure
        // Since we cannot modify environment variables in a running JVM,
        // we just ensure the method can be called without throwing exceptions
        // In a real scenario, the environment variables would need to be set before JVM startup
        
        // When & Then
        assertThatNoException().isThrownBy(() -> {
            // This should not throw an exception even if environment variables are not set
            SfOssConfig config = SfOssConfig.fromEnvironment();
            // The config may have null values, which is expected when env vars are not set
        });
    }
}