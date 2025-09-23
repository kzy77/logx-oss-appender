package org.logx.storage.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * S3StorageConfig配置测试
 * <p>
 * 验证S3存储配置的构建、验证和属性访问功能。
 */
class S3StorageConfigTest {

    @Test
    void testAwsS3ConfigBuilder() {
        // Given
        String endpoint = "https://s3.us-west-2.amazonaws.com";
        String region = "us-west-2";
        String accessKeyId = "test-access-key";
        String accessKeySecret = "test-secret-key";
        String bucket = "test-bucket";

        // When
        AwsS3Config config = AwsS3Config.builder()
                .endpoint(endpoint)
                .region(region)
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .bucket(bucket)
                .pathStyleAccess(false)
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
        assertThat(config.isPathStyleAccess()).isFalse();
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getMaxConnections()).isEqualTo(100);
        assertThat(config.isEnableSsl()).isTrue();
    }

    @Test
    void testConfigValidationWithValidConfig() {
        // Given
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatNoException().isThrownBy(config::validateConfig);
    }

    @ParameterizedTest
    @ValueSource(strings = { "us-west-2", "ap-guangzhou", "cn-north-1" })
    void testConfigValidationWithValidRegions(String region) {
        // Given
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region(region)
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When & Then
        assertThatNoException().isThrownBy(config::validateConfig);
    }

    @Test
    void testConfigValidationWithNullEndpoint() {
        // Given
        AwsS3Config config = AwsS3Config.builder()
                .endpoint(null)
                .region("us-west-2")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("")
                .region("us-west-2")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
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
        AwsS3Config config1 = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        AwsS3Config config2 = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        AwsS3Config config3 = AwsS3Config.builder()
                .endpoint("https://s3.us-east-1.amazonaws.com")
                .region("us-east-1")
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
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.us-west-2.amazonaws.com")
                .region("us-west-2")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When
        String configString = config.toString();

        // Then
        assertThat(configString).contains("endpoint='https://s3.us-west-2.amazonaws.com'");
        assertThat(configString).contains("region='us-west-2'");
        assertThat(configString).contains("accessKeyId='te****ey'");
        assertThat(configString).contains("bucket='test-bucket'");
    }
}