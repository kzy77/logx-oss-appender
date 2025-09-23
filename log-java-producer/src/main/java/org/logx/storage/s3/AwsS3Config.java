package org.logx.storage.s3;

import org.logx.storage.s3.S3StorageConfig;
import java.time.Duration;

/**
 * AWS S3特定配置类
 * <p>
 * 继承基础S3StorageConfig，添加AWS S3特有的配置参数和验证逻辑。 支持标准AWS S3服务的全部配置选项。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class AwsS3Config extends S3StorageConfig {

    // AWS特定的默认值
    private static final String DEFAULT_REGION = "ap-guangzhou";

    /**
     * 私有构造函数，使用Builder模式
     */
    private AwsS3Config(Builder builder) {
        super(builder);
    }

    @Override
    public void validateConfig() {
        super.validateConfig(); // 调用基类验证

        // AWS S3特定验证
        String region = getRegion();
        if (region != null && !region.trim().isEmpty() && !isValidAwsRegion(region)) {
            throw new IllegalArgumentException("Invalid AWS region format: " + region);
        }
    }

    /**
     * 验证AWS区域格式
     */
    private boolean isValidAwsRegion(String region) {
        // AWS区域格式验证：例如 us-east-1, eu-west-1, ap-southeast-1
        return region != null && region.matches("^[a-z]{2}-[a-z]+-\\d+$");
    }

    /**
     * AWS S3配置构建器
     */
    public static class Builder extends S3StorageConfig.Builder<Builder> {

        public Builder() {
            // AWS S3默认配置
            region(DEFAULT_REGION);
            pathStyleAccess(false); // AWS S3使用虚拟主机风格
            connectTimeout(Duration.ofSeconds(10));
            readTimeout(Duration.ofSeconds(30));
            enableSsl(true);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public AwsS3Config build() {
            return new AwsS3Config(this);
        }
    }

    /**
     * 创建AWS S3配置构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 从环境变量创建AWS S3配置
     */
    public static AwsS3Config fromEnvironment() {
        return builder().accessKeyId(getEnvOrProperty("AWS_ACCESS_KEY_ID", "aws.accessKeyId"))
                .accessKeySecret(getEnvOrProperty("AWS_SECRET_ACCESS_KEY", "aws.secretAccessKey"))
                .region(getEnvOrProperty("AWS_DEFAULT_REGION", "aws.region", DEFAULT_REGION))
                .bucket(getEnvOrProperty("AWS_S3_BUCKET", "aws.s3.bucket")).build();
    }

    /**
     * 获取环境变量或系统属性的值
     */
    private static String getEnvOrProperty(String envName, String propName) {
        return getEnvOrProperty(envName, propName, null);
    }

    private static String getEnvOrProperty(String envName, String propName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(propName);
        }
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
}