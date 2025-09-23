package org.logx.storage.s3;

import org.logx.storage.StorageConfig;
import org.logx.config.validator.ConfigValidator;
import org.logx.config.validator.ConfigValidatorAdapter;

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
public class S3ConfigValidator extends ConfigValidatorAdapter<StorageConfig> {
    
    private static final Pattern BUCKET_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");
    private static final Pattern REGION_PATTERN = Pattern.compile("^[a-z0-9-]+$");

    public S3ConfigValidator() {
        super(StorageConfig.class);
    }

    @Override
    protected ConfigValidator.ValidationResult validateConfig(StorageConfig config, ConfigValidator.ValidationOptions options) {
        List<ConfigValidator.ValidationError> errors = new ArrayList<>();
        List<ConfigValidator.ValidationError> warnings = new ArrayList<>();

        // 验证必需字段
        validateRequiredFields(config, errors);

        // 验证字段格式
        validateFieldFormats(config, errors);

        // 验证业务逻辑
        validateBusinessLogic(config, errors, warnings);

        // 验证字段依赖关系
        validateDependencies(config, errors, warnings);

        // 根据选项决定是否包含警告
        if (options.isIncludeWarnings()) {
            errors.addAll(warnings);
        }

        // 在严格模式下，警告也会导致验证失败
        boolean isValid = errors.isEmpty();
        if (options.isStrictMode() && !warnings.isEmpty()) {
            isValid = false;
        }

        return isValid ? ConfigValidator.ValidationResult.valid() : ConfigValidator.ValidationResult.invalid(errors);
    }

    /**
     * 验证必需字段
     */
    private void validateRequiredFields(StorageConfig config, List<ConfigValidator.ValidationError> errors) {
        if (isNullOrEmpty(config.getEndpoint())) {
            errors.add(new ConfigValidator.ValidationError("endpoint", "Endpoint is required",
                    "Set endpoint to your S3 service URL (e.g., https://s3.amazonaws.com)",
                    ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getRegion())) {
            errors.add(new ConfigValidator.ValidationError("region", "Region is required",
                    "Set region to your S3 region (e.g., us-east-1)", ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getBucket())) {
            errors.add(new ConfigValidator.ValidationError("bucket", "Bucket name is required", "Set bucket to your S3 bucket name",
                    ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getAccessKeyId())) {
            errors.add(new ConfigValidator.ValidationError("accessKeyId", "Access key ID is required",
                    "Set accessKeyId to your S3 access key", ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING));
        }

        if (isNullOrEmpty(config.getAccessKeySecret())) {
            errors.add(new ConfigValidator.ValidationError("accessKeySecret", "Access key secret is required",
                    "Set accessKeySecret to your S3 secret key", ConfigValidator.ErrorType.REQUIRED_FIELD_MISSING));
        }
    }

    /**
     * 验证字段格式
     */
    private void validateFieldFormats(StorageConfig config, List<ConfigValidator.ValidationError> errors) {
        // 验证endpoint URL格式
        if (!isNullOrEmpty(config.getEndpoint())) {
            try {
                URL url = new URL(config.getEndpoint());
                if (!"https".equals(url.getProtocol())) {
                    errors.add(new ConfigValidator.ValidationError("endpoint", "Endpoint must use HTTPS protocol",
                            "Change endpoint URL to use https:// instead of http://", ConfigValidator.ErrorType.INVALID_FORMAT));
                }
            } catch (MalformedURLException e) {
                errors.add(new ConfigValidator.ValidationError("endpoint", "Invalid endpoint URL format: " + e.getMessage(),
                        "Provide a valid URL (e.g., https://s3.amazonaws.com)", ConfigValidator.ErrorType.INVALID_FORMAT));
            }
        }

        // 验证bucket名称格式
        if (!isNullOrEmpty(config.getBucket()) && !BUCKET_NAME_PATTERN.matcher(config.getBucket()).matches()) {
            errors.add(new ConfigValidator.ValidationError("bucket", "Invalid bucket name format",
                    "Bucket name must be 3-63 characters, lowercase letters, numbers, dots and hyphens only",
                    ConfigValidator.ErrorType.INVALID_FORMAT));
        }

        // 验证region格式
        if (!isNullOrEmpty(config.getRegion()) && !REGION_PATTERN.matcher(config.getRegion()).matches()) {
            errors.add(new ConfigValidator.ValidationError("region", "Invalid region format",
                    "Region must contain only lowercase letters, numbers and hyphens", ConfigValidator.ErrorType.INVALID_FORMAT));
        }

        // 验证访问密钥格式（宽松验证，因为不同云服务商格式可能不同）
        if (!isNullOrEmpty(config.getAccessKeyId()) && config.getAccessKeyId().length() < 16) {
            errors.add(new ConfigValidator.ValidationError("accessKeyId", "Access key ID too short",
                    "Access key ID should be at least 16 characters long", ConfigValidator.ErrorType.INVALID_FORMAT));
        }

        if (!isNullOrEmpty(config.getAccessKeySecret()) && config.getAccessKeySecret().length() < 32) {
            errors.add(new ConfigValidator.ValidationError("accessKeySecret", "Access key secret too short",
                    "Access key secret should be at least 32 characters long", ConfigValidator.ErrorType.INVALID_FORMAT));
        }
    }

    /**
     * 验证业务逻辑
     */
    private void validateBusinessLogic(StorageConfig config, List<ConfigValidator.ValidationError> errors, List<ConfigValidator.ValidationError> warnings) {
        // 验证超时配置
        if (config.getConnectTimeout() != null && config.getConnectTimeout().isNegative()) {
            errors.add(new ConfigValidator.ValidationError("connectTimeout", "Connect timeout must be non-negative",
                    "Set connectTimeout to a positive duration (e.g., Duration.ofSeconds(10))",
                    ConfigValidator.ErrorType.VALUE_OUT_OF_RANGE));
        }

        if (config.getReadTimeout() != null && config.getReadTimeout().isNegative()) {
            errors.add(new ConfigValidator.ValidationError("readTimeout", "Read timeout must be non-negative",
                    "Set readTimeout to a positive duration (e.g., Duration.ofSeconds(30))",
                    ConfigValidator.ErrorType.VALUE_OUT_OF_RANGE));
        }

        // 验证超时时间合理性（作为警告）
        if (config.getConnectTimeout() != null && config.getConnectTimeout().getSeconds() > 60) {
            warnings.add(new ConfigValidator.ValidationError("connectTimeout", "Connect timeout is large",
                    "Consider reducing connectTimeout to under 60 seconds for better responsiveness",
                    ConfigValidator.ErrorType.WARNING));
        }

        if (config.getReadTimeout() != null && config.getReadTimeout().getSeconds() > 300) { // 5 minutes
            warnings.add(new ConfigValidator.ValidationError("readTimeout", "Read timeout is large",
                    "Consider reducing readTimeout to under 5 minutes", ConfigValidator.ErrorType.WARNING));
        }

        // 验证连接数配置
        if (config.getMaxConnections() <= 0) {
            errors.add(new ConfigValidator.ValidationError("maxConnections", "Max connections must be positive",
                    "Set maxConnections to a positive value (e.g., 50)", ConfigValidator.ErrorType.VALUE_OUT_OF_RANGE));
        }

        if (config.getMaxConnections() > 1000) {
            warnings.add(new ConfigValidator.ValidationError("maxConnections", "Max connections is large",
                    "Consider reducing maxConnections to under 1000 for resource efficiency",
                    ConfigValidator.ErrorType.WARNING));
        }
    }

    /**
     * 验证字段依赖关系
     */
    private void validateDependencies(StorageConfig config, List<ConfigValidator.ValidationError> errors, List<ConfigValidator.ValidationError> warnings) {
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
                    errors.add(new ConfigValidator.ValidationError("endpoint", "Endpoint region does not match configured region",
                            "Ensure endpoint contains the correct region: " + region, ConfigValidator.ErrorType.DEPENDENCY_CONFLICT));
                }
            }
        }

        // 验证SSL配置与endpoint的一致性
        if (!isNullOrEmpty(config.getEndpoint())) {
            boolean endpointUsesHttps = config.getEndpoint().toLowerCase().startsWith("https://");
            if (config.isEnableSsl() && !endpointUsesHttps) {
                errors.add(new ConfigValidator.ValidationError("enableSsl", "SSL is enabled but endpoint uses HTTP",
                        "Change endpoint to use HTTPS or disable SSL", ConfigValidator.ErrorType.DEPENDENCY_CONFLICT));
            }
            if (!config.isEnableSsl() && endpointUsesHttps) {
                errors.add(new ConfigValidator.ValidationError("enableSsl", "SSL is disabled but endpoint uses HTTPS",
                        "Enable SSL or change endpoint to use HTTP", ConfigValidator.ErrorType.DEPENDENCY_CONFLICT));
            }
        }

        // 检查是否使用了默认区域（作为警告）
        if (!isNullOrEmpty(config.getRegion()) && "us-east-1".equals(config.getRegion())) {
            warnings.add(new ConfigValidator.ValidationError("region", "Using default region",
                    "Consider explicitly setting a region closer to your deployment", ConfigValidator.ErrorType.WARNING));
        }
    }

    /**
     * 检查字符串是否为null或空
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}