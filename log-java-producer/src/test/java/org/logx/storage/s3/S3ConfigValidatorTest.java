package org.logx.storage.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.config.factory.ConfigFactory;
import org.logx.config.validator.ConfigValidator;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * S3ConfigValidator测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class S3ConfigValidatorTest {

    private S3ConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new S3ConfigValidator();
    }

    @Test
    void shouldValidateValidConfig() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket").build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldRejectNonS3ConfigObject() {
        String invalidConfig = "not a config object";

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(invalidConfig);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.BUSINESS_LOGIC_ERROR);
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                // 故意省略region, accessKeyId, accessKeySecret, bucket
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSizeGreaterThanOrEqualTo(4);

        // 检查是否包含必需字段错误
        assertThat(result.getErrors()).extracting(org.logx.config.validator.ConfigValidator.ValidationError::getType)
                .contains(org.logx.config.validator.ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING);
    }

    @Test
    void shouldRejectInvalidEndpointFormat() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("invalid-url-format")
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket").build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("endpoint");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.INVALID_FORMAT);
        });
    }

    @Test
    void shouldRejectHttpEndpoint() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("http://s3.amazonaws.com") // HTTP
                                                                                                                       // instead
                                                                                                                       // of
                                                                                                                       // HTTPS
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket").build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("endpoint");
            assertThat(error.getMessage()).contains("HTTPS protocol");
        });
    }

    @Test
    void shouldRejectInvalidBucketName() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .bucket("Invalid-Bucket-Name-With-Uppercase") // 无效的bucket名称
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("bucket");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.INVALID_FORMAT);
        });
    }

    @Test
    void shouldRejectShortAccessKeys() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                .region("us-east-1").accessKeyId("SHORT") // 太短的access key
                .accessKeySecret("ALSO-SHORT") // 太短的secret key
                .bucket("my-test-bucket").build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("accessKeyId");
            assertThat(error.getMessage()).contains("too short");
        });
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("accessKeySecret");
            assertThat(error.getMessage()).contains("too short");
        });
    }

    @Test
    void shouldRejectNegativeTimeouts() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket")
                .connectTimeout(Duration.ofSeconds(-1)) // 负数超时
                .readTimeout(Duration.ofSeconds(-1)) // 负数超时
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("connectTimeout");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.VALUE_OUT_OF_RANGE);
        });
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("readTimeout");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.VALUE_OUT_OF_RANGE);
        });
    }

    @Test
    void shouldWarnAboutLargeTimeouts() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket")
                .connectTimeout(Duration.ofMinutes(2)) // 过大的连接超时
                .readTimeout(Duration.ofMinutes(10)) // 过大的读取超时
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("connectTimeout");
            assertThat(error.getMessage()).contains("too large");
        });
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("readTimeout");
            assertThat(error.getMessage()).contains("too large");
        });
    }

    @Test
    void shouldRejectInvalidMaxConnections() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket").maxConnections(0) // 无效的连接数
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("maxConnections");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.VALUE_OUT_OF_RANGE);
        });
    }

    @Test
    void shouldWarnAboutTooManyConnections() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder().endpoint("https://s3.amazonaws.com")
                .region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket")
                .maxConnections(2000) // 过多的连接数
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("maxConnections");
            assertThat(error.getMessage()).contains("too large");
        });
    }

    @Test
    void shouldValidateEndpointRegionConsistency() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder()
                .endpoint("https://s3.ap-guangzhou.amazonaws.com").region("us-east-1") // 与endpoint不一致的region
                .accessKeyId("AKIAIOSFODNN7EXAMPLE").accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .bucket("my-test-bucket").build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("endpoint");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.DEPENDENCY_CONFLICT);
        });
    }

    @Test
    void shouldValidateSslEndpointConsistency() {
        // SSL启用但endpoint使用HTTP
        ConfigFactory.AwsS3Config httpConfig = new ConfigFactory.AwsS3Config.Builder()
                .endpoint("http://s3.amazonaws.com").region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket").enableSsl(true) // SSL启用但endpoint是HTTP
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result1 = validator.validate(httpConfig);
        assertThat(result1.isValid()).isFalse();
        assertThat(result1.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("enableSsl");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.DEPENDENCY_CONFLICT);
        });

        // SSL禁用但endpoint使用HTTPS
        ConfigFactory.AwsS3Config httpsConfig = new ConfigFactory.AwsS3Config.Builder()
                .endpoint("https://s3.amazonaws.com").region("us-east-1").accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY").bucket("my-test-bucket").enableSsl(false) // SSL禁用但endpoint是HTTPS
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result2 = validator.validate(httpsConfig);
        assertThat(result2.isValid()).isFalse();
        assertThat(result2.getErrors()).anySatisfy(error -> {
            assertThat(error.getField()).isEqualTo("enableSsl");
            assertThat(error.getType()).isEqualTo(org.logx.config.validator.ConfigValidator.ErrorType.DEPENDENCY_CONFLICT);
        });
    }

    @Test
    void shouldProvideHelpfulErrorMessages() {
        ConfigFactory.AwsS3Config config = new ConfigFactory.AwsS3Config.Builder()
                // 故意留空所有必需字段
                .build();

        org.logx.config.validator.ConfigValidator.ValidationResult result = validator.validate(config);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorSummary()).contains("Configuration validation failed");

        // 验证每个错误都有建议
        for (org.logx.config.validator.ConfigValidator.ValidationError error : result.getErrors()) {
            assertThat(error.getSuggestion()).isNotNull();
            assertThat(error.getSuggestion()).isNotEmpty();
        }
    }
}
