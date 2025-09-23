package org.logx.config.validator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * ConfigValidator接口测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.1.0
 */
class ConfigValidatorTest {

    @Test
    void shouldCreateValidResult() {
        ConfigValidator.ValidationResult result = ConfigValidator.ValidationResult.valid();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getErrorSummary()).isEqualTo("Configuration is valid");
    }

    @Test
    void shouldCreateInvalidResult() {
        ConfigValidator.ValidationError error = new ConfigValidator.ValidationError("field", "error message", "suggestion",
                ConfigValidator.ErrorType.INVALID_FORMAT);
        List<ConfigValidator.ValidationError> errors = new ArrayList<>();
        errors.add(error);
        ConfigValidator.ValidationResult result = ConfigValidator.ValidationResult.invalid(errors);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).isSameAs(error);
        assertThat(result.getErrorSummary()).contains("Configuration validation failed");
        assertThat(result.getErrorSummary()).contains("error message");
    }

    @Test
    void shouldHandleNullErrorsList() {
        ConfigValidator.ValidationResult result = new ConfigValidator.ValidationResult(false, null);
        assertThat(result.getErrors()).isNotNull().isEmpty();
    }

    @Test
    void shouldCreateValidationErrorWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        ConfigValidator.ValidationError error = new ConfigValidator.ValidationError("field", "error message", "suggestion",
                ConfigValidator.ErrorType.NETWORK_ERROR, cause);

        assertThat(error.getField()).isEqualTo("field");
        assertThat(error.getMessage()).isEqualTo("error message");
        assertThat(error.getSuggestion()).isEqualTo("suggestion");
        assertThat(error.getType()).isEqualTo(ConfigValidator.ErrorType.NETWORK_ERROR);
        assertThat(error.getCause()).isSameAs(cause);
    }

    @Test
    void shouldCreateValidationErrorWithoutCause() {
        ConfigValidator.ValidationError error = new ConfigValidator.ValidationError("field", "error message", "suggestion",
                ConfigValidator.ErrorType.INVALID_FORMAT);

        assertThat(error.getField()).isEqualTo("field");
        assertThat(error.getMessage()).isEqualTo("error message");
        assertThat(error.getSuggestion()).isEqualTo("suggestion");
        assertThat(error.getType()).isEqualTo(ConfigValidator.ErrorType.INVALID_FORMAT);
        assertThat(error.getCause()).isNull();
    }

    @Test
    void shouldFormatValidationErrorToString() {
        ConfigValidator.ValidationError error = new ConfigValidator.ValidationError("field", "error message", "suggestion",
                ConfigValidator.ErrorType.INVALID_FORMAT);

        String errorString = error.toString();
        assertThat(errorString).contains("[INVALID_FORMAT]");
        assertThat(errorString).contains("field");
        assertThat(errorString).contains("error message");
        assertThat(errorString).contains("suggestion");
    }

    @Test
    void shouldFormatValidationErrorToStringWithoutSuggestion() {
        ConfigValidator.ValidationError error = new ConfigValidator.ValidationError("field", "error message", null,
                ConfigValidator.ErrorType.INVALID_FORMAT);

        String errorString = error.toString();
        assertThat(errorString).contains("[INVALID_FORMAT]");
        assertThat(errorString).contains("field");
        assertThat(errorString).contains("error message");
        assertThat(errorString).doesNotContain("suggestion");
    }

    @Test
    void shouldCreateValidationOptions() {
        ConfigValidator.ValidationOptions defaultOptions = ConfigValidator.ValidationOptions.defaultOptions();
        assertThat(defaultOptions.isStrictMode()).isFalse();
        assertThat(defaultOptions.isIncludeWarnings()).isFalse();

        ConfigValidator.ValidationOptions strictOptions = ConfigValidator.ValidationOptions.strict();
        assertThat(strictOptions.isStrictMode()).isTrue();
        assertThat(strictOptions.isIncludeWarnings()).isTrue();

        ConfigValidator.ValidationOptions warningOptions = ConfigValidator.ValidationOptions.withWarnings();
        assertThat(warningOptions.isStrictMode()).isFalse();
        assertThat(warningOptions.isIncludeWarnings()).isTrue();
    }
}