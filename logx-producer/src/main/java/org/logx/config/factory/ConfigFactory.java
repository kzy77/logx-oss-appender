package org.logx.config.factory;

import org.logx.config.ConfigManager;
import org.logx.storage.StorageOssType;
import org.logx.storage.StorageConfig;
import java.time.Duration;

/**
 * 配置工厂类
 * <p>
 * 根据存储后端类型创建相应的配置对象，支持从ConfigManager读取统一配置。
 * 支持logx.oss前缀风格配置，如logx.oss.region=ap-guangzhou。
 * 提供默认值管理和配置模板功能。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ConfigFactory {

    private final ConfigManager configManager;

    /**
     * 构造配置工厂
     *
     * @param configManager
     *            配置管理器
     */
    public ConfigFactory(ConfigManager configManager) {
        // 配置管理器是复杂对象，直接引用即可
        this.configManager = configManager;
    }

    /**
     * 根据存储后端类型创建配置
     *
     * @param ossType
     *            存储后端类型
     *
     * @return 对应的配置对象
     *
     * @throws IllegalArgumentException
     *             如果后端类型不支持
     */
    public StorageConfig createConfig(StorageOssType ossType) {
        switch (ossType) {
            case AWS_S3:
                return createAwsS3Config();
            case MINIO:
                return createMinioConfig();
            case GENERIC_S3:
                return createGenericS3Config();
            default:
                throw new IllegalArgumentException("Unsupported storage ossType: " + ossType);
        }
    }

    /**
     * 创建AWS S3配置
     */
    private StorageConfig createAwsS3Config() {
        return new ConfigFactory.AwsS3Config.Builder().endpoint(getProperty("logx.oss.endpoint", "https://s3.amazonaws.com"))
                .region(getProperty("logx.oss.region", "ap-guangzhou"))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("logx.oss.pathStyleAccess", false))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("logx.oss.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("logx.oss.enableSsl", true)).build();
    }

    /**
     * 创建MinIO配置
     */
    private StorageConfig createMinioConfig() {
        // MinIO默认使用路径风格
        // MinIO开发环境通常不用SSL
        return new MinioConfig.Builder().endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getProperty("logx.oss.region", "ap-guangzhou")).accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("logx.oss.pathStyleAccess", true))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("logx.oss.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("logx.oss.enableSsl", false))
                .build();
    }

    /**
     * 创建通用S3配置
     */
    private StorageConfig createGenericS3Config() {
        return new GenericS3Config.Builder().endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getProperty("logx.oss.region", "ap-guangzhou")).accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret")).bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("logx.oss.pathStyleAccess", false))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("logx.oss.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("logx.oss.enableSsl", true)).build();
    }

    /**
     * 获取属性值，支持默认值
     */
    private String getProperty(String key, String defaultValue) {
        return configManager.getProperty(key, defaultValue);
    }

    /**
     * 获取必需属性值，如果不存在抛出异常
     */
    private String getRequiredProperty(String key) {
        String value = configManager.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required configuration property '" + key + "' is missing");
        }
        return value;
    }

    /**
     * AWS S3特定配置实现
     */
    public static class AwsS3Config extends StorageConfig {
        private AwsS3Config(Builder builder) {
            super(builder);
        }

        public static class Builder extends StorageConfig.Builder<Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public AwsS3Config build() {
                return new AwsS3Config(this);
            }
        }
    }

    /**
     * MinIO特定配置实现
     */
    public static class MinioConfig extends StorageConfig {
        private MinioConfig(Builder builder) {
            super(builder);
        }

        public static class Builder extends StorageConfig.Builder<Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public MinioConfig build() {
                return new MinioConfig(this);
            }
        }
    }

    /**
     * 通用S3配置实现
     */
    public static class GenericS3Config extends StorageConfig {
        private GenericS3Config(Builder builder) {
            super(builder);
        }

        public static class Builder extends StorageConfig.Builder<Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public GenericS3Config build() {
                return new GenericS3Config(this);
            }
        }
    }
}
