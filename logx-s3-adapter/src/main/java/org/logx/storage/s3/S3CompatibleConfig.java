package org.logx.storage.s3;

import org.logx.storage.StorageConfig;
import java.time.Duration;

/**
 * S3兼容存储配置类
 * <p>
 * 继承基础StorageConfig，添加S3兼容存储特有的配置参数和验证逻辑。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class S3CompatibleConfig extends StorageConfig {

    // S3兼容存储特定的默认值
    private static final String DEFAULT_REGION = "us-east-1";

    /**
     * 私有构造函数，使用Builder模式
     */
    private S3CompatibleConfig(Builder builder) {
        super(builder);
    }

    @Override
    public void validateConfig() {
        super.validateConfig(); // 调用基类验证

        // S3兼容存储特定验证
        String region = getRegion();
        if (region != null && !region.trim().isEmpty() && !isValidS3Region(region)) {
            throw new IllegalArgumentException("Invalid S3 region format: " + region);
        }
    }

    /**
     * 验证S3区域格式
     */
    private boolean isValidS3Region(String region) {
        // S3区域格式验证 - 简化版
        return region != null && !region.trim().isEmpty();
    }

    /**
     * S3兼容配置构建器
     */
    public static class Builder extends StorageConfig.Builder<Builder> {

        public Builder() {
            // S3兼容存储默认配置
            region(DEFAULT_REGION);
            pathStyleAccess(false); // S3默认使用虚拟主机风格
            connectTimeout(Duration.ofSeconds(10));
            readTimeout(Duration.ofSeconds(30));
            enableSsl(true);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public S3CompatibleConfig build() {
            return new S3CompatibleConfig(this);
        }
    }

    /**
     * 创建S3兼容配置构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 从环境变量创建S3兼容配置
     */
    public static S3CompatibleConfig fromEnvironment() {
        return builder().accessKeyId(getEnvOrProperty("S3_ACCESS_KEY_ID", "s3.accessKeyId"))
                .accessKeySecret(getEnvOrProperty("S3_SECRET_ACCESS_KEY", "s3.secretAccessKey"))
                .region(getEnvOrProperty("S3_DEFAULT_REGION", "s3.region", DEFAULT_REGION))
                .bucket(getEnvOrProperty("S3_BUCKET", "s3.bucket")).build();
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

    /**
     * 检测配置
     */
    public static Config detectConfig(String endpoint, String region) {
        Config config = new Config();
        config.normalizedEndpoint = endpoint;
        config.region = region != null && !region.trim().isEmpty() ? region : DEFAULT_REGION;
        config.forcePathStyle = shouldForcePathStyle(endpoint);
        return config;
    }

    /**
     * 判断是否应该强制路径风格
     */
    private static boolean shouldForcePathStyle(String endpoint) {
        if (endpoint == null) {
            return false;
        }
        
        // 某些S3兼容服务需要强制路径风格
        return endpoint.contains("aliyuncs.com") || 
               endpoint.contains("myqcloud.com") || 
               endpoint.contains("myhuaweicloud.com") ||
               endpoint.contains("localhost") ||
               endpoint.contains("127.0.0.1");
    }

    /**
     * 配置信息类
     */
    public static class Config {
        public String normalizedEndpoint;
        public String region;
        public boolean forcePathStyle;
    }
}