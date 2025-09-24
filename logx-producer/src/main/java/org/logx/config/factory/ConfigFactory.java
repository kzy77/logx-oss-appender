package org.logx.config.factory;

import org.logx.config.ConfigManager;
import org.logx.storage.StorageBackend;
import org.logx.storage.StorageConfig;
import java.time.Duration;

/**
 * 配置工厂类
 * <p>
 * 根据存储后端类型创建相应的配置对象，支持从ConfigManager读取统一配置。 提供默认值管理和配置模板功能。
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
        // 直接使用传入的配置管理器，避免重新加载导致配置丢失
        this.configManager = configManager;
    }

    /**
     * 根据存储后端类型创建配置
     *
     * @param backend
     *            存储后端类型
     *
     * @return 对应的配置对象
     *
     * @throws IllegalArgumentException
     *             如果后端类型不支持
     */
    public StorageConfig createConfig(StorageBackend backend) {
        switch (backend) {
            case AWS_S3:
                return createAwsS3Config();
            case MINIO:
                return createMinioConfig();
            case GENERIC_S3:
                return createGenericS3Config();
            default:
                throw new IllegalArgumentException("Unsupported storage backend: " + backend);
        }
    }

    /**
     * 创建AWS S3配置
     */
    private StorageConfig createAwsS3Config() {
        return new ConfigFactory.AwsS3Config.Builder().endpoint(getProperty("aws.s3.endpoint", "https://s3.amazonaws.com"))
                .region(getProperty("aws.s3.region", "us-east-1"))
                .accessKeyId(getRequiredProperty("aws.s3.accessKeyId"))
                .accessKeySecret(getRequiredProperty("aws.s3.secretAccessKey"))
                .bucket(getRequiredProperty("aws.s3.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("aws.s3.pathStyleAccess", false))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("aws.s3.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("aws.s3.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("aws.s3.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("aws.s3.enableSsl", true)).build();
    }

    /**
     * 创建MinIO配置
     */
    private StorageConfig createMinioConfig() {
        return new MinioConfig.Builder().endpoint(getRequiredProperty("minio.endpoint"))
                .region(getProperty("minio.region", "us-east-1")).accessKeyId(getRequiredProperty("minio.accessKeyId"))
                .accessKeySecret(getRequiredProperty("minio.secretAccessKey"))
                .bucket(getRequiredProperty("minio.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("minio.pathStyleAccess", true)) // MinIO默认使用路径风格
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("minio.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("minio.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("minio.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("minio.enableSsl", false)) // MinIO开发环境通常不用SSL
                .build();
    }

    /**
     * 创建通用S3配置
     */
    private StorageConfig createGenericS3Config() {
        return new GenericS3Config.Builder().endpoint(getRequiredProperty("s3.endpoint"))
                .region(getProperty("s3.region", "us-east-1")).accessKeyId(getRequiredProperty("s3.accessKeyId"))
                .accessKeySecret(getRequiredProperty("s3.secretAccessKey")).bucket(getRequiredProperty("s3.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("s3.pathStyleAccess", false))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("s3.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("s3.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("s3.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("s3.enableSsl", true)).build();
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
