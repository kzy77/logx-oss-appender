package org.logx.config.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.storage.s3.AwsS3Config;
import org.logx.storage.s3.S3ConfigValidator;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * ConfigValidatorFactory测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.1.0
 */
class ConfigValidatorFactoryTest {

    private ConfigValidatorFactory factory;

    @BeforeEach
    void setUp() {
        factory = ConfigValidatorFactory.getInstance();
    }

    @Test
    void shouldGetRegisteredValidator() {
        ConfigValidator validator = factory.getValidator(AwsS3Config.class);
        assertThat(validator).isNotNull().isInstanceOf(S3ConfigValidator.class);
    }

    @Test
    void shouldReturnNullForUnregisteredValidator() {
        // 清理非默认验证器，确保测试环境干净
        factory.clearNonDefaultValidators();
        ConfigValidator validator = factory.getValidator(String.class);
        assertThat(validator).isNull();
    }

    @Test
    void shouldThrowExceptionForUnregisteredValidator() {
        // 清理非默认验证器，确保测试环境干净
        factory.clearNonDefaultValidators();
        assertThatThrownBy(() -> factory.getValidatorOrThrow(String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No validator found for config type: String");
    }

    @Test
    void shouldRegisterAndRetrieveCustomValidator() {
        // 清理非默认验证器，确保测试环境干净
        factory.clearNonDefaultValidators();
        
        ConfigValidator customValidator = new ConfigValidator() {
            @Override
            public ConfigValidator.ValidationResult validate(Object config) {
                return ConfigValidator.ValidationResult.valid();
            }
        };

        factory.registerValidator(String.class, customValidator);
        ConfigValidator retrievedValidator = factory.getValidator(String.class);
        assertThat(retrievedValidator).isSameAs(customValidator);
    }

    @Test
    void shouldValidateConfigUsingFactory() {
        // 创建一个有效的AwsS3Config
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.amazonaws.com")
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .bucket("my-test-bucket")
                .build();

        ConfigValidator.ValidationResult result = factory.validate(config);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldValidateConfigWithWarnings() {
        // 创建一个会触发警告的AwsS3Config（超时时间较长）
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.amazonaws.com")
                .region("us-west-1")  // 使用非默认区域
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .bucket("my-test-bucket")
                .connectTimeout(Duration.ofSeconds(120)) // 较长的连接超时时间
                .build();

        // 使用包含警告的选项进行验证
        ConfigValidator.ValidationResult result = factory.validate(config, ConfigValidator.ValidationOptions.withWarnings());
        assertThat(result.isValid()).isFalse(); // 在包含警告的模式下，警告会导致验证失败
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getType()).isEqualTo(ConfigValidator.ErrorType.WARNING);
        assertThat(result.getErrors().get(0).getMessage()).contains("Connect timeout is large");
    }

    @Test
    void shouldValidateConfigWithStrictMode() {
        // 创建一个会触发警告的AwsS3Config（使用默认区域）
        AwsS3Config config = AwsS3Config.builder()
                .endpoint("https://s3.amazonaws.com")
                .region("us-east-1") // 默认区域
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .bucket("my-test-bucket")
                .build();

        // 使用严格模式进行验证
        ConfigValidator.ValidationResult result = factory.validate(config, ConfigValidator.ValidationOptions.strict());
        assertThat(result.isValid()).isFalse(); // 在严格模式下，警告也会导致验证失败
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getType()).isEqualTo(ConfigValidator.ErrorType.WARNING);
        assertThat(result.getErrors().get(0).getMessage()).contains("Using default region");
    }

    @Test
    void shouldRejectNullConfig() {
        ConfigValidator.ValidationResult result = factory.validate(null);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getType()).isEqualTo(ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING);
        assertThat(result.getErrors().get(0).getMessage()).contains("Configuration object is null");
    }

    @Test
    void shouldRejectConfigWithoutValidator() {
        ConfigValidator.ValidationResult result = factory.validate("not a config object");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getType()).isEqualTo(ConfigValidator.ErrorType.BUSINESS_LOGIC_ERROR);
        assertThat(result.getErrors().get(0).getMessage()).contains("No validator found for config type: String");
    }
}