package org.logx.config.factory;

import org.logx.config.CommonConfig;
import org.logx.config.ConfigManager;
import org.logx.storage.StorageOssType;
import org.logx.storage.StorageConfig;
import java.time.Duration;

/**
 * 配置工厂类
 * <p>
 * 根据存储后端类型创建相应的配置对象，支持从ConfigManager读取统一配置。
 * 支持logx.oss前缀风格配置，如logx.oss.region=us。
 * 提供默认值管理和配置模板功能。
 * <p>
 * 统一管理所有云服务商的个性化配置，包括：
 * <ul>
 * <li>SF_S3: region=US, pathStyleAccess=true</li>
 * <li>MINIO: pathStyleAccess=true, enableSsl=false（本地HTTP）</li>
 * <li>AWS_S3: pathStyleAccess=false</li>
 * </ul>
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
            case SF_S3:
                return createSfS3Config();
            case SF_OSS:
                return createSfOssConfig();
            case ALIYUN_OSS:
            case TENCENT_COS:
            case HUAWEI_OBS:
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
                .region(getProperty("logx.oss.region", "US"))
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
                .region(getProperty("logx.oss.region", "US")).accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
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
     * 创建SF OSS配置
     * SF OSS使用个性化OSS协议
     */
    private StorageConfig createSfOssConfig() {
        return new SfOssConfig.Builder()
                .endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getProperty("logx.oss.region", "US"))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("logx.oss.pathStyleAccess", true))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("logx.oss.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("logx.oss.enableSsl", true))
                .build();
    }

    /**
     * 创建SF S3配置
     * <p>
     * SF OSS使用S3协议时的特殊配置：
     * <ul>
     * <li>pathStyleAccess默认为true</li>
     * <li>自动添加X-Delete-After元数据（1年有效期）</li>
     * <li>region默认为"US"（SF S3个性化配置）</li>
     * </ul>
     */
    private StorageConfig createSfS3Config() {
        return new SfS3Config.Builder()
                .endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getProperty("logx.oss.region", "US"))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("logx.oss.pathStyleAccess", true))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("logx.oss.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("logx.oss.enableSsl", true))
                .build();
    }

    /**
     * 创建通用S3配置
     */
    private StorageConfig createGenericS3Config() {
        return new GenericS3Config.Builder()
                .endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getProperty("logx.oss.region", "US"))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(configManager.getBooleanProperty("logx.oss.pathStyleAccess", false))
                .connectTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.connectTimeout", 30000)))
                .readTimeout(Duration.ofMillis(configManager.getLongProperty("logx.oss.readTimeout", 60000)))
                .maxConnections(configManager.getIntProperty("logx.oss.maxConnections", 50))
                .enableSsl(configManager.getBooleanProperty("logx.oss.enableSsl", true))
                .build();
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
     * SF OSS特定配置实现
     */
    public static class SfOssConfig extends StorageConfig {
        private SfOssConfig(Builder builder) {
            super(builder);
        }

        public static class Builder extends StorageConfig.Builder<Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public SfOssConfig build() {
                return new SfOssConfig(this);
            }
        }
    }

    /**
     * SF S3特定配置实现
     */
    public static class SfS3Config extends StorageConfig {
        private SfS3Config(Builder builder) {
            super(builder);
        }

        public static class Builder extends StorageConfig.Builder<Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public SfS3Config build() {
                return new SfS3Config(this);
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

    /**
     * 应用云服务商特定的个性化配置
     * <p>
     * 根据云服务商类型，统一应用个性化默认配置：
     * <ul>
     * <li>SF_S3: region=US, pathStyleAccess=true, enableSsl=true</li>
     * <li>MINIO: pathStyleAccess=true, enableSsl=false（本地HTTP端点）</li>
     * <li>AWS_S3: pathStyleAccess=false, enableSsl=true</li>
     * <li>其他云服务商: 使用各自的pathStyleDefault配置</li>
     * </ul>
     *
     * @param baseConfig 基础配置
     * @param ossType 云服务商类型
     * @return 应用了个性化配置的新配置对象
     */
    public static StorageConfig applyVendorSpecificDefaults(StorageConfig baseConfig, StorageOssType ossType) {
        if (baseConfig == null || ossType == null) {
            return baseConfig;
        }

        class ConfigBuilder extends StorageConfig.Builder<ConfigBuilder> {
            @Override
            protected ConfigBuilder self() {
                return this;
            }

            @Override
            public StorageConfig build() {
                try {
                    java.lang.reflect.Constructor<StorageConfig> constructor =
                        StorageConfig.class.getDeclaredConstructor(StorageConfig.Builder.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(this);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create StorageConfig instance", e);
                }
            }
        }

        ConfigBuilder builder = new ConfigBuilder();

        builder.endpoint(baseConfig.getEndpoint())
                .accessKeyId(baseConfig.getAccessKeyId())
                .accessKeySecret(baseConfig.getAccessKeySecret())
                .bucket(baseConfig.getBucket())
                .keyPrefix(baseConfig.getKeyPrefix())
                .connectTimeout(baseConfig.getConnectTimeout())
                .readTimeout(baseConfig.getReadTimeout())
                .maxConnections(baseConfig.getMaxConnections());

        String region = baseConfig.getRegion();
        if (region == null || region.isEmpty()) {
            if (ossType == StorageOssType.SF_S3) {
                region = "US";
            } else {
                region = CommonConfig.Defaults.REGION;
            }
        }
        builder.region(region);

        boolean pathStyleAccess = ossType.isPathStyleDefault();
        builder.pathStyleAccess(pathStyleAccess);

        boolean enableSsl;
        String endpoint = baseConfig.getEndpoint();
        if (endpoint != null && !endpoint.isEmpty()) {
            String lowerEndpoint = endpoint.toLowerCase();
            if (ossType == StorageOssType.MINIO && lowerEndpoint.startsWith("http://")) {
                enableSsl = false;
            } else {
                enableSsl = lowerEndpoint.startsWith("https://");
            }
        } else {
            enableSsl = true;
        }
        builder.enableSsl(enableSsl);

        String protocolType = ossType.getProtocolType();
        builder.ossType(protocolType);

        return builder.build();
    }
}
