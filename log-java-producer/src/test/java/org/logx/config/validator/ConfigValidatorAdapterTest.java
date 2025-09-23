package org.logx.config.validator;

import org.junit.jupiter.api.Test;
import org.logx.storage.s3.AwsS3Config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ConfigValidatorAdapter测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.1.0
 */
class ConfigValidatorAdapterTest {

    private static class TestConfigValidator extends ConfigValidatorAdapter<AwsS3Config> {
        private boolean validateConfigCalled = false;
        private AwsS3Config lastConfig;
        private ConfigValidator.ValidationOptions lastOptions;

        protected TestConfigValidator() {
            super(AwsS3Config.class);
        }

        @Override
        protected ConfigValidator.ValidationResult validateConfig(AwsS3Config config,
                ConfigValidator.ValidationOptions options) {
            validateConfigCalled = true;
            lastConfig = config;
            lastOptions = options;
            return ConfigValidator.ValidationResult.valid();
        }

        public boolean isValidateConfigCalled() {
            return validateConfigCalled;
        }

        public AwsS3Config getLastConfig() {
            return lastConfig;
        }

        public ConfigValidator.ValidationOptions getLastOptions() {
            return lastOptions;
        }
    }

    @Test
    void shouldValidateCorrectConfigType() {
        TestConfigValidator adapter = new TestConfigValidator();
        AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com").region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE").accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .bucket("my-test-bucket").build();

        ConfigValidator.ValidationResult result = adapter.validate(config);

        assertThat(result.isValid()).isTrue();
        assertThat(adapter.isValidateConfigCalled()).isTrue();
        assertThat(adapter.getLastConfig()).isSameAs(config);
    }

    @Test
    void shouldRejectWrongConfigType() {
        TestConfigValidator adapter = new TestConfigValidator();
        String wrongConfig = "not a AwsS3Config";

        ConfigValidator.ValidationResult result = adapter.validate(wrongConfig);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getType()).isEqualTo(ConfigValidator.ErrorType.BUSINESS_LOGIC_ERROR);
        assertThat(result.getErrors().get(0).getMessage()).contains("Expected configuration type: AwsS3Config");
        assertThat(adapter.isValidateConfigCalled()).isFalse();
    }

    @Test
    void shouldRejectNullConfig() {
        TestConfigValidator adapter = new TestConfigValidator();

        ConfigValidator.ValidationResult result = adapter.validate(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getType()).isEqualTo(ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING);
        assertThat(result.getErrors().get(0).getMessage()).contains("Configuration object is null");
        assertThat(adapter.isValidateConfigCalled()).isFalse();
    }

    @Test
    void shouldValidateWithOptions() {
        TestConfigValidator adapter = new TestConfigValidator();
        AwsS3Config config = AwsS3Config.builder().endpoint("https://s3.amazonaws.com").region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE").accessKeySecret("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .bucket("my-test-bucket").build();
        ConfigValidator.ValidationOptions options = ConfigValidator.ValidationOptions.strict();

        ConfigValidator.ValidationResult result = adapter.validate(config, options);

        assertThat(result.isValid()).isTrue();
        assertThat(adapter.isValidateConfigCalled()).isTrue();
        assertThat(adapter.getLastOptions()).isSameAs(options);
    }

    @Test
    void shouldGetConfigType() {
        TestConfigValidator adapter = new TestConfigValidator();
        assertThat(adapter.getConfigType()).isEqualTo(AwsS3Config.class);
    }
}