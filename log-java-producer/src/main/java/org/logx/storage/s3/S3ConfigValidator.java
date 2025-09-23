package org.logx.storage.s3;

import org.logx.storage.StorageConfig;
import org.logx.config.validator.ConfigValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * S3存储配置验证器
 * <p>
 * 验证S3存储配置的完整性、格式正确性和业务逻辑一致性。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class S3ConfigValidator implements ConfigValidator {

    private static final Pattern BUCKET_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");
    private static final Pattern REGION_PATTERN = Pattern.compile("^[a-z0-9-]+$");

    @Override
    public ValidationResult validate(Object config) {
        if (!(config instanceof StorageConfig)) {
            return ValidationResult.invalid(List.of(new ValidationError("config", "Expected StorageConfig instance",
                    "Provide a valid StorageConfig object", ErrorType.BUSINESS_LOGIC_ERROR)));
        }

        StorageConfig s3Config = (StorageConfig) config;
        List<ValidationError> errors = new ArrayList<>();

        // 验证必需字段
        validateRequiredFields(s3Config, errors);

        // 验证字段格式
        validateFieldFormats(s3Config, errors);

        // 验证业务逻辑
        validateBusinessLogic(s3Config, errors);

        // 验证字段依赖关系
        validateDependencies(s3Config, errors);

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    /**
     * 验证必需字段
     */
    private void validateRequiredFields(StorageConfig config, List<ValidationError> errors) {
        if (isNullOrEmpty(config.getEndpoint())) {
            errors.add(new ValidationError("endpoint", "Endpoint is required",
                    "Set endpoint to your S3 service URL (e.g., https://s3.amazonaws.com)",
                    ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getRegion())) {
            errors.add(new ValidationError("region", "Region is required",
                    "Set region to your S3 region (e.g., us-east-1)", ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getBucket())) {
            errors.add(new ValidationError("bucket", "Bucket name is required", "Set bucket to your S3 bucket name",
                    ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getAccessKeyId())) {
            errors.add(new ValidationError("accessKeyId", "Access key ID is required",
                    "Set accessKeyId to your S3 access key", ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getAccessKeySecret())) {
            errors.add(new ValidationError("accessKeySecret", "Access key secret is required",
                    "Set accessKeySecret to your S3 secret key", ErrorType.REQUIRED_FIELD_MISSING));
        }
    }

    /**
     * 验证字段格式
     */
    private void validateFieldFormats(StorageConfig config, List<ValidationError> errors) {
        // 验证endpoint URL格式
        if (!isNullOrEmpty(config.getEndpoint())) {
            try {
                URL url = new URL(config.getEndpoint());
                if (!"https".equals(url.getProtocol())) {
                    errors.add(new ValidationError("endpoint", "Endpoint must use HTTPS protocol",
                            "Change endpoint URL to use https:// instead of http://", ErrorType.INVALID_FORMAT));
                }
            } catch (MalformedURLException e) {
                errors.add(new ValidationError("endpoint", "Invalid endpoint URL format: " + e.getMessage(),
                        "Provide a valid URL (e.g., https://s3.amazonaws.com)", ErrorType.INVALID_FORMAT));
            }
        }

        // 验证bucket名称格式
        if (!isNullOrEmpty(config.getBucket()) && !BUCKET_NAME_PATTERN.matcher(config.getBucket()).matches()) {
            errors.add(new ValidationError("bucket", "Invalid bucket name format",
                    "Bucket name must be 3-63 characters, lowercase letters, numbers, dots and hyphens only",
                    ErrorType.INVALID_FORMAT));
        }

        // 验证region格式
        if (!isNullOrEmpty(config.getRegion()) && !REGION_PATTERN.matcher(config.getRegion()).matches()) {
            errors.add(new ValidationError("region", "Invalid region format",
                    "Region must contain only lowercase letters, numbers and hyphens", ErrorType.INVALID_FORMAT));
        }

        // 验证访问密钥格式（宽松验证，因为不同云服务商格式可能不同）
        if (!isNullOrEmpty(config.getAccessKeyId()) && config.getAccessKeyId().length() < 16) {
            errors.add(new ValidationError("accessKeyId", "Access key ID too short",
                    "Access key ID should be at least 16 characters long", ErrorType.INVALID_FORMAT));
        }

        if (!isNullOrEmpty(config.getAccessKeySecret()) && config.getAccessKeySecret().length() < 32) {
            errors.add(new ValidationError("accessKeySecret", "Access key secret too short",
                    "Access key secret should be at least 32 characters long", ErrorType.INVALID_FORMAT));
        }
    }

    /**
     * 验证业务逻辑
     */
    private void validateBusinessLogic(StorageConfig config, List<ValidationError> errors) {
        // 验证超时配置
        if (config.getConnectTimeout() != null && config.getConnectTimeout().isNegative()) {
            errors.add(new ValidationError("connectTimeout", "Connect timeout must be non-negative",
                    "Set connectTimeout to a positive duration (e.g., Duration.ofSeconds(10))",
                    ErrorType.VALUE_OUT_OF_RANGE));
        }

        if (config.getReadTimeout() != null && config.getReadTimeout().isNegative()) {
            errors.add(new ValidationError("readTimeout", "Read timeout must be non-negative",
                    "Set readTimeout to a positive duration (e.g., Duration.ofSeconds(30))",
                    ErrorType.VALUE_OUT_OF_RANGE));
        }

        // 验证超时时间合理性
        if (config.getConnectTimeout() != null && config.getConnectTimeout().getSeconds() > 60) {
            errors.add(new ValidationError("connectTimeout", "Connect timeout too large",
                    "Consider reducing connectTimeout to under 60 seconds for better responsiveness",
                    ErrorType.VALUE_OUT_OF_RANGE));
        }

        if (config.getReadTimeout() != null && config.getReadTimeout().getSeconds() > 300) { // 5 minutes
            errors.add(new ValidationError("readTimeout", "Read timeout too large",
                    "Consider reducing readTimeout to under 5 minutes", ErrorType.VALUE_OUT_OF_RANGE));
        }

        // 验证连接数配置
        if (config.getMaxConnections() <= 0) {
            errors.add(new ValidationError("maxConnections", "Max connections must be positive",
                    "Set maxConnections to a positive value (e.g., 50)", ErrorType.VALUE_OUT_OF_RANGE));
        }

        if (config.getMaxConnections() > 1000) {
            errors.add(new ValidationError("maxConnections", "Max connections too large",
                    "Consider reducing maxConnections to under 1000 for resource efficiency",
                    ErrorType.VALUE_OUT_OF_RANGE));
        }
    }

    /**
     * 验证字段依赖关系
     */
    private void validateDependencies(StorageConfig config, List<ValidationError> errors) {
        // 验证endpoint与region的一致性
        if (!isNullOrEmpty(config.getEndpoint()) && !isNullOrEmpty(config.getRegion())) {
            String endpoint = config.getEndpoint().toLowerCase();
            String region = config.getRegion();

            // AWS S3 endpoint格式验证
            if (endpoint.contains("amazonaws.com")) {
                // 允许全局端点 s3.amazonaws.com
                boolean isGlobalEndpoint = endpoint.equals("https://s3.amazonaws.com")
                        || endpoint.equals("http://s3.amazonaws.com");

                // 如果不是全局端点，检查是否包含区域
                if (!isGlobalEndpoint && !endpoint.contains(region)) {
                    errors.add(new ValidationError("endpoint", "Endpoint region does not match configured region",
                            "Ensure endpoint contains the correct region: " + region, ErrorType.DEPENDENCY_CONFLICT));
                }
            }
        }

        // 验证SSL配置与endpoint的一致性
        if (!isNullOrEmpty(config.getEndpoint())) {
            boolean endpointUsesHttps = config.getEndpoint().toLowerCase().startsWith("https://");
            if (config.isEnableSsl() && !endpointUsesHttps) {
                errors.add(new ValidationError("enableSsl", "SSL is enabled but endpoint uses HTTP",
                        "Change endpoint to use HTTPS or disable SSL", ErrorType.DEPENDENCY_CONFLICT));
            }
            if (!config.isEnableSsl() && endpointUsesHttps) {
                errors.add(new ValidationError("enableSsl", "SSL is disabled but endpoint uses HTTPS",
                        "Enable SSL or change endpoint to use HTTP", ErrorType.DEPENDENCY_CONFLICT));
            }
        }
    }

    /**
     * 检查字符串是否为null或空
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}