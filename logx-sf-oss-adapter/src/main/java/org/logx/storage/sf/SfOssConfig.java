package org.logx.storage.sf;

import org.logx.storage.StorageConfig;
import java.time.Duration;

/**
 * SF OSS特定配置类
 * <p>
 * 继承基础StorageConfig，添加SF OSS特有的配置参数和验证逻辑。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class SfOssConfig extends StorageConfig {

    // SF OSS特定的默认值
    private static final String DEFAULT_REGION = "cn-north-1";

    /**
     * 私有构造函数，使用Builder模式
     */
    private SfOssConfig(Builder builder) {
        super(builder);
    }

    @Override
    public void validateConfig() {
        super.validateConfig(); // 调用基类验证

        // SF OSS特定验证
        String region = getRegion();
        if (region != null && !region.trim().isEmpty() && !isValidSfRegion(region)) {
            throw new IllegalArgumentException("Invalid SF OSS region format: " + region);
        }
    }

    /**
     * 验证SF OSS区域格式
     */
    private boolean isValidSfRegion(String region) {
        // SF OSS区域格式验证
        return region != null && (region.matches("^cn-[a-z]+-\\d+$") || region.matches("^[a-z]{2}-[a-z]+-\\d+$"));
    }

    /**
     * SF OSS配置构建器
     */
    public static class Builder extends StorageConfig.Builder<Builder> {

        public Builder() {
            // SF OSS默认配置
            region(DEFAULT_REGION);
            pathStyleAccess(true); // SF OSS使用路径风格
            connectTimeout(Duration.ofSeconds(10));
            readTimeout(Duration.ofSeconds(30));
            enableSsl(true);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public SfOssConfig build() {
            return new SfOssConfig(this);
        }
    }

    /**
     * 创建SF OSS配置构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 从环境变量创建SF OSS配置
     */
    public static SfOssConfig fromEnvironment() {
        return builder().accessKeyId(getEnvOrProperty("SF_OSS_ACCESS_KEY_ID", "sf.oss.accessKeyId"))
                .accessKeySecret(getEnvOrProperty("SF_OSS_SECRET_ACCESS_KEY", "sf.oss.secretAccessKey"))
                .region(getEnvOrProperty("SF_OSS_DEFAULT_REGION", "sf.oss.region", DEFAULT_REGION))
                .bucket(getEnvOrProperty("SF_OSS_BUCKET", "sf.oss.bucket")).build();
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