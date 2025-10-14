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
        return new ConfigFactory.AwsS3Config.Builder()
                .endpoint(getProperty("logx.oss.endpoint", "https://s3.amazonaws.com"))
                .region(getRegionWithDefault(StorageOssType.AWS_S3))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(getPathStyleAccessWithDefault(StorageOssType.AWS_S3))
                .connectTimeout(getConnectTimeoutWithDefault())
                .readTimeout(getReadTimeoutWithDefault())
                .maxConnections(getMaxConnectionsWithDefault())
                .enableSsl(getEnableSslWithDefault(StorageOssType.AWS_S3))
                .build();
    }

    /**
     * 创建MinIO配置
     */
    private StorageConfig createMinioConfig() {
        return new MinioConfig.Builder()
                .endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getRegionWithDefault(StorageOssType.MINIO))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(getPathStyleAccessWithDefault(StorageOssType.MINIO))
                .connectTimeout(getConnectTimeoutWithDefault())
                .readTimeout(getReadTimeoutWithDefault())
                .maxConnections(getMaxConnectionsWithDefault())
                .enableSsl(getEnableSslWithDefault(StorageOssType.MINIO))
                .build();
    }

    /**
     * 创建SF OSS配置
     * SF OSS使用个性化OSS协议
     */
    private StorageConfig createSfOssConfig() {
        return new SfOssConfig.Builder()
                .endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getRegionWithDefault(StorageOssType.SF_OSS))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(getPathStyleAccessWithDefault(StorageOssType.SF_OSS))
                .connectTimeout(getConnectTimeoutWithDefault())
                .readTimeout(getReadTimeoutWithDefault())
                .maxConnections(getMaxConnectionsWithDefault())
                .enableSsl(getEnableSslWithDefault(StorageOssType.SF_OSS))
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
                .region(getRegionWithDefault(StorageOssType.SF_S3))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(getPathStyleAccessWithDefault(StorageOssType.SF_S3))
                .connectTimeout(getConnectTimeoutWithDefault())
                .readTimeout(getReadTimeoutWithDefault())
                .maxConnections(getMaxConnectionsWithDefault())
                .enableSsl(getEnableSslWithDefault(StorageOssType.SF_S3))
                .build();
    }

    /**
     * 创建通用S3配置
     */
    private StorageConfig createGenericS3Config() {
        return new GenericS3Config.Builder()
                .endpoint(getRequiredProperty("logx.oss.endpoint"))
                .region(getRegionWithDefault(StorageOssType.GENERIC_S3))
                .accessKeyId(getRequiredProperty("logx.oss.accessKeyId"))
                .accessKeySecret(getRequiredProperty("logx.oss.accessKeySecret"))
                .bucket(getRequiredProperty("logx.oss.bucket"))
                .pathStyleAccess(getPathStyleAccessWithDefault(StorageOssType.GENERIC_S3))
                .connectTimeout(getConnectTimeoutWithDefault())
                .readTimeout(getReadTimeoutWithDefault())
                .maxConnections(getMaxConnectionsWithDefault())
                .enableSsl(getEnableSslWithDefault(StorageOssType.GENERIC_S3))
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
     * 获取region配置，支持vendor特定默认值
     * <p>
     * 优先级：
     * <ol>
     * <li>JVM系统属性、环境变量、配置文件</li>
     * <li>Vendor特定默认值（SF_S3/SF_OSS/MINIO使用"US"）</li>
     * <li>通用默认值（ap-guangzhou）</li>
     * </ol>
     */
    private String getRegionWithDefault(StorageOssType ossType) {
        String region = configManager.getProperty("logx.oss.region");
        if (region != null) {
            return region;
        }

        // Vendor特定默认值
        switch (ossType) {
            case SF_S3:
            case SF_OSS:
            case MINIO:
                return "US";
            default:
                return CommonConfig.Defaults.REGION;
        }
    }

    /**
     * 获取pathStyleAccess配置，支持vendor特定默认值
     */
    private boolean getPathStyleAccessWithDefault(StorageOssType ossType) {
        String value = configManager.getProperty("logx.oss.pathStyleAccess");
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        // Vendor特定默认值：根据云服务商类型决定
        switch (ossType) {
            case SF_S3:
            case SF_OSS:
            case MINIO:
            case ALIYUN_OSS:
            case TENCENT_COS:
            case HUAWEI_OBS:
                return true;  // 路径风格
            case AWS_S3:
            case GENERIC_S3:
            default:
                return false; // 虚拟主机风格
        }
    }

    /**
     * 获取enableSsl配置，支持vendor特定默认值
     * <p>
     * 特殊逻辑：MINIO且使用http://端点时默认为false，其他情况默认为true
     */
    private boolean getEnableSslWithDefault(StorageOssType ossType) {
        String value = configManager.getProperty("logx.oss.enableSsl");
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        // Vendor特定默认值：MINIO使用http://端点时默认false
        String endpoint = configManager.getProperty("logx.oss.endpoint");
        if (ossType == StorageOssType.MINIO && endpoint != null && endpoint.toLowerCase().startsWith("http://")) {
            return false;
        }
        return true;
    }

    /**
     * 获取connectTimeout配置，默认30秒
     */
    private Duration getConnectTimeoutWithDefault() {
        String value = configManager.getProperty("logx.oss.connectTimeout");
        return value != null
            ? Duration.ofMillis(Long.parseLong(value))
            : Duration.ofMillis(30000);
    }

    /**
     * 获取readTimeout配置，默认60秒
     */
    private Duration getReadTimeoutWithDefault() {
        String value = configManager.getProperty("logx.oss.readTimeout");
        return value != null
            ? Duration.ofMillis(Long.parseLong(value))
            : Duration.ofMillis(60000);
    }

    /**
     * 获取maxConnections配置，默认50
     */
    private int getMaxConnectionsWithDefault() {
        String value = configManager.getProperty("logx.oss.maxConnections");
        return value != null ? Integer.parseInt(value) : 50;
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
     * 根据云服务商类型，统一应用个性化默认配置，优先级：
     * <ol>
     * <li>baseConfig中的显式配置值（来自XML配置）</li>
     * <li>JVM系统属性、环境变量、配置文件（通过ConfigManager查询）</li>
     * <li>Vendor特定默认值（如SF_S3的region=US）</li>
     * <li>通用默认值（如region=ap-guangzhou）</li>
     * </ol>
     * <p>
     * Vendor特定规则：
     * <ul>
     * <li>SF_S3/SF_OSS: region=US, pathStyleAccess=true</li>
     * <li>MINIO: region=US, pathStyleAccess=true, enableSsl=false（HTTP端点时）</li>
     * <li>AWS_S3: pathStyleAccess=false</li>
     * <li>其他云服务商: 使用各自的pathStyleDefault配置</li>
     * </ul>
     *
     * @param baseConfig 基础配置（来自框架适配器）
     * @param ossType 云服务商类型
     * @return 应用了个性化配置的新配置对象
     */
    public static StorageConfig applyVendorSpecificDefaults(StorageConfig baseConfig, StorageOssType ossType) {
        if (baseConfig == null || ossType == null) {
            return baseConfig;
        }

        // 创建ConfigManager实例用于读取外部配置
        ConfigManager configManager = new ConfigManager();

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

        // 保留baseConfig中已有的值
        builder.endpoint(baseConfig.getEndpoint())
                .accessKeyId(baseConfig.getAccessKeyId())
                .accessKeySecret(baseConfig.getAccessKeySecret())
                .bucket(baseConfig.getBucket())
                .keyPrefix(baseConfig.getKeyPrefix())
                .connectTimeout(baseConfig.getConnectTimeout())
                .readTimeout(baseConfig.getReadTimeout())
                .maxConnections(baseConfig.getMaxConnections());

        // region: baseConfig → ConfigManager → vendor默认值 → 通用默认值
        String region = baseConfig.getRegion();
        if (region == null || region.isEmpty()) {
            region = configManager.getProperty("logx.oss.region");
            if (region == null || region.isEmpty()) {
                // Vendor特定默认值
                if (ossType == StorageOssType.SF_S3 || ossType == StorageOssType.SF_OSS || ossType == StorageOssType.MINIO) {
                    region = "US";
                } else {
                    region = CommonConfig.Defaults.REGION;
                }
            }
        }
        builder.region(region);

        // pathStyleAccess: ConfigManager → vendor默认值 (总是重新计算，因为boolean无法判断是否配置过)
        String pathStyleStr = configManager.getProperty("logx.oss.pathStyleAccess");
        boolean pathStyleAccess;
        if (pathStyleStr != null && !pathStyleStr.isEmpty()) {
            pathStyleAccess = Boolean.parseBoolean(pathStyleStr);
        } else {
            // Vendor特定默认值
            pathStyleAccess = ossType.isPathStyleDefault();
        }
        builder.pathStyleAccess(pathStyleAccess);

        // enableSsl: ConfigManager → 根据endpoint和vendor决定 (总是重新计算)
        String enableSslStr = configManager.getProperty("logx.oss.enableSsl");
        boolean enableSsl;
        if (enableSslStr != null && !enableSslStr.isEmpty()) {
            enableSsl = Boolean.parseBoolean(enableSslStr);
        } else {
            // 根据endpoint判断
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
        }
        builder.enableSsl(enableSsl);

        // 注意：不要覆盖ossType字段，保持用户配置的云服务商类型（如SF_S3、MINIO等）
        // 在需要协议类型时，通过ossType.getProtocolType()动态获取

        return builder.build();
    }
}
