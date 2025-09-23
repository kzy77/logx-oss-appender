package org.logx.config.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.logx.storage.s3.AwsS3Config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * AWS S3配置类单元测试
 */
public class AwsS3ConfigTest {

    // 保存原始环境变量值，用于测试后恢复
    private Map<String, String> originalSystemProperties;

    @BeforeEach
    void setUp() {
        // 保存原始系统属性
        originalSystemProperties = new HashMap<>();
        String[] properties = { "aws.accessKeyId", "aws.secretAccessKey", "aws.region", "aws.s3.bucket" };
        for (String prop : properties) {
            originalSystemProperties.put(prop, System.getProperty(prop));
        }
    }

    @AfterEach
    void tearDown() {
        // 恢复原始系统属性
        for (Map.Entry<String, String> entry : originalSystemProperties.entrySet()) {
            if (entry.getValue() != null) {
                System.setProperty(entry.getKey(), entry.getValue());
            } else {
                System.clearProperty(entry.getKey());
            }
        }
    }

    @Test
    void builder_withValidInputs_shouldCreateConfig() {
        // When
        AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com").accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key").region("us-east-1").bucket("test-bucket").build();

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getAccessKeyId()).isEqualTo("test-access-key");
        assertThat(config.getAccessKeySecret()).isEqualTo("test-secret-key");
        assertThat(config.getRegion()).isEqualTo("us-east-1");
        assertThat(config.getBucket()).isEqualTo("test-bucket");
        assertThat(config.isPathStyleAccess()).isFalse(); // AWS S3默认使用虚拟主机风格
        assertThat(config.isEnableSsl()).isTrue();
    }

    @Test
    void builder_withDefaultValues_shouldUseAwsDefaults() {
        // When
        AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com").accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key").bucket("test-bucket").build();

        // Then
        assertThat(config.getRegion()).isEqualTo("us-east-1"); // 默认区域
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.isPathStyleAccess()).isFalse();
        assertThat(config.isEnableSsl()).isTrue();
    }

    @Test
    void builder_withCustomTimeouts_shouldSetCorrectly() {
        // When
        AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com").accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key").bucket("test-bucket").connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(60)).build();

        // Then
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void validateConfig_withValidAwsRegion_shouldPass() {
        // Given
        AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com").accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key").region("us-west-2").bucket("test-bucket").build();

        // When & Then
        assertThatCode(() -> config.validateConfig()).doesNotThrowAnyException();
    }

    @Test
    void validateConfig_withInvalidAwsRegion_shouldFail() {
        // Given
        AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com").accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key").region("invalid-region").bucket("test-bucket").build();

        // When & Then
        assertThatThrownBy(() -> config.validateConfig()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid AWS region format: invalid-region");
    }

    @Test
    void validateConfig_withAllValidAwsRegions_shouldPass() {
        // Given - 测试各种有效的AWS区域格式
        String[] validRegions = { "ap-guangzhou", "us-west-2", "eu-west-1", "eu-central-1", "ap-southeast-1",
                "ap-northeast-1", "sa-east-1" };

        for (String region : validRegions) {
            AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com")
                    .accessKeyId("test-access-key").accessKeySecret("test-secret-key").region(region)
                    .bucket("test-bucket").build();

            // When & Then
            assertThatCode(() -> config.validateConfig()).as("Region " + region + " should be valid")
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void validateConfig_withInvalidAwsRegionFormats_shouldFail() {
        // Given - 测试各种无效的AWS区域格式
        String[] invalidRegions = { "us-east", "us-east-1-extra", "useast1", "US-EAST-1", "us_east_1", "123-456-789",
                "", "us-" };

        for (String region : invalidRegions) {
            AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com")
                    .accessKeyId("test-access-key").accessKeySecret("test-secret-key").region(region)
                    .bucket("test-bucket").build();

            // When & Then
            if (region.isEmpty()) {
                assertThatThrownBy(() -> config.validateConfig()).as("Region " + region + " should be invalid")
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Region cannot be null or empty");
            } else {
                assertThatThrownBy(() -> config.validateConfig()).as("Region " + region + " should be invalid")
                        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid AWS region format");
            }
        }
    }

    @Test
    void fromEnvironment_withSystemProperties_shouldCreateConfig() {
        // Given
        System.setProperty("aws.accessKeyId", "prop-access-key");
        System.setProperty("aws.secretAccessKey", "prop-secret-key");
        System.setProperty("aws.region", "eu-west-1");
        System.setProperty("aws.s3.bucket", "prop-bucket");

        // When
        AwsS3Config config = AwsS3Config.fromEnvironment();

        // Then
        assertThat(config.getAccessKeyId()).isEqualTo("prop-access-key");
        assertThat(config.getAccessKeySecret()).isEqualTo("prop-secret-key");
        assertThat(config.getRegion()).isEqualTo("eu-west-1");
        assertThat(config.getBucket()).isEqualTo("prop-bucket");
    }

    @Test
    void fromEnvironment_withoutConfig_shouldUseDefaults() {
        // Given - 清除所有相关配置
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
        System.clearProperty("aws.region");
        System.clearProperty("aws.s3.bucket");

        // When
        AwsS3Config config = AwsS3Config.fromEnvironment();

        // Then
        assertThat(config.getAccessKeyId()).isNull();
        assertThat(config.getAccessKeySecret()).isNull();
        assertThat(config.getRegion()).isEqualTo("us-east-1"); // 默认区域
        assertThat(config.getBucket()).isNull();
    }

    @Test
    void fromEnvironment_withDefaultRegion_shouldReturnUsEast1() {
        // Given - 不设置区域配置
        System.clearProperty("aws.region");

        // When
        AwsS3Config config = AwsS3Config.fromEnvironment();

        // Then
        assertThat(config.getRegion()).isEqualTo("us-east-1");
    }

    @Test
    void validateConfig_withMissingRequiredFields_shouldFail() {
        // Given - 缺少必需字段的配置
        AwsS3Config config = AwsS3Config.builder().region("us-east-1").build();

        // When & Then
        assertThatThrownBy(() -> config.validateConfig()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void builder_chainedCalls_shouldWork() {
        // When
        AwsS3Config config = AwsS3Config.builder().accessKeyId("test-key").accessKeySecret("test-secret")
                .region("ap-southeast-1").bucket("test-bucket").pathStyleAccess(true).enableSsl(false)
                .connectTimeout(Duration.ofSeconds(15)).readTimeout(Duration.ofSeconds(45)).maxConnections(100).build();

        // Then
        assertThat(config.getAccessKeyId()).isEqualTo("test-key");
        assertThat(config.getAccessKeySecret()).isEqualTo("test-secret");
        assertThat(config.getRegion()).isEqualTo("ap-southeast-1");
        assertThat(config.getBucket()).isEqualTo("test-bucket");
        assertThat(config.isPathStyleAccess()).isTrue();
        assertThat(config.isEnableSsl()).isFalse();
        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(config.getReadTimeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(config.getMaxConnections()).isEqualTo(100);
    }

    @Test
    void builder_multipleBuilds_shouldCreateIndependentConfigs() {
        // Given
        AwsS3Config.Builder builder = AwsS3Config.builder().accessKeyId("test-key").accessKeySecret("test-secret")
                .bucket("test-bucket");

        // When
        AwsS3Config config1 = builder.region("us-east-1").build();
        AwsS3Config config2 = builder.region("eu-west-1").build();

        // Then
        assertThat(config1.getRegion()).isEqualTo("us-east-1");
        assertThat(config2.getRegion()).isEqualTo("eu-west-1");
        assertThat(config1.getAccessKeyId()).isEqualTo(config2.getAccessKeyId());
    }
}
