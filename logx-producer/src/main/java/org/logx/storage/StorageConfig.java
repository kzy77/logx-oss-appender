package org.logx.storage;

import java.time.Duration;
import java.util.Objects;

/**
 * 存储配置类
 * <p>
 * 定义所有存储服务的通用配置参数，支持多种存储后端（S3兼容存储、SF OSS等）。
 * 提供配置验证、Builder模式构建和不可变对象特性。
 * <p>
 * 配置类遵循不可变对象设计模式，一旦创建不可修改，确保线程安全。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class StorageConfig {

    // 必需配置字段
    private final String backendType;
    private final String endpoint;
    private final String region;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucket;

    // 可选配置字段
    private final String keyPrefix;
    private final boolean pathStyleAccess;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final int maxConnections;
    private final boolean enableSsl;

    /**
     * 构造函数，由Builder调用创建不可变配置对象
     */
    protected StorageConfig(Builder<?> builder) {
        this.backendType = builder.backendType;
        this.endpoint = builder.endpoint;
        this.region = builder.region;
        this.accessKeyId = builder.accessKeyId;
        this.accessKeySecret = builder.accessKeySecret;
        this.bucket = builder.bucket;
        this.keyPrefix = builder.keyPrefix;

        this.pathStyleAccess = builder.pathStyleAccess;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxConnections = builder.maxConnections;
        this.enableSsl = builder.enableSsl;
    }

    /**
     * 验证配置的有效性
     * <p>
     * 检查所有必需字段是否已设置，可选字段是否在合理范围内。
     *
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validateConfig() {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }

        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("Region cannot be null or empty");
        }

        if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
            throw new IllegalArgumentException("AccessKeyId cannot be null or empty");
        }

        if (accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
            throw new IllegalArgumentException("AccessKeySecret cannot be null or empty");
        }

        if (bucket == null || bucket.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket cannot be null or empty");
        }

        if (maxConnections <= 0) {
            throw new IllegalArgumentException("MaxConnections must be positive");
        }

        if (connectTimeout != null && connectTimeout.isNegative()) {
            throw new IllegalArgumentException("ConnectTimeout cannot be negative");
        }

        if (readTimeout != null && readTimeout.isNegative()) {
            throw new IllegalArgumentException("ReadTimeout cannot be negative");
        }
    }

    /**
     * 自动检测后端类型
     *
     * @return StorageConfig 配置对象
     */
    public static StorageConfig detectBackendType(StorageConfig config) {
        if (config.getBackendType() != null && !config.getBackendType().isEmpty()) {
            return config;
        }

        // 创建一个具体的Builder实现来构建更新后的配置
        class ConfigBuilder extends Builder<ConfigBuilder> {
            @Override
            protected ConfigBuilder self() {
                return this;
            }

            @Override
            public StorageConfig build() {
                return new StorageConfig(this);
            }
        }

        ConfigBuilder builder = new ConfigBuilder();

        // 复制现有配置
        builder.backendType(config.getBackendType())
               .endpoint(config.getEndpoint())
               .region(config.getRegion())
               .accessKeyId(config.getAccessKeyId())
               .accessKeySecret(config.getAccessKeySecret())
               .bucket(config.getBucket())
               .keyPrefix(config.getKeyPrefix())
               .pathStyleAccess(config.isPathStyleAccess())
               .connectTimeout(config.getConnectTimeout())
               .readTimeout(config.getReadTimeout())
               .maxConnections(config.getMaxConnections())
               .enableSsl(config.isEnableSsl());

        // 根据endpoint自动检测后端类型
        String endpoint = config.getEndpoint();
        if (endpoint != null) {
            if (endpoint.contains("sf-oss.com")) {
                builder.backendType("SF_OSS");
            } else if (endpoint.contains("aliyuncs.com")) {
                builder.backendType("S3");
            } else if (endpoint.contains("amazonaws.com")) {
                builder.backendType("S3");
            } else if (endpoint.contains("myqcloud.com")) {
                builder.backendType("S3");
            } else if (endpoint.contains("myhuaweicloud.com")) {
                builder.backendType("S3");
            } else {
                // 默认使用CommonConfig中定义的默认值
                builder.backendType(CommonConfig.Defaults.OSS_TYPE);
            }
        } else {
            // 默认使用CommonConfig中定义的默认值
            builder.backendType(CommonConfig.Defaults.OSS_TYPE);
        }

        return builder.build();
    }

    // Getter方法
    public String getBackendType() {
        return backendType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public boolean isEnableSsl() {
        return enableSsl;
    }

    /**
     * Builder基类，支持流式配置和类型安全
     *
     * @param <T> 具体Builder类型，支持方法链式调用
     */
    public static abstract class Builder<T extends Builder<T>> {
        private String backendType;
        private String endpoint;
        private String region;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucket;
        private String keyPrefix = CommonConfig.Defaults.KEY_PREFIX;
        private boolean pathStyleAccess = false;
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofSeconds(60);
        private int maxConnections = 50;
        private boolean enableSsl = true;

        /**
         * 设置后端类型
         *
         * @param backendType 后端类型（如"S3"、"SF_OSS"等）
         * @return Builder实例，支持链式调用
         */
        public T backendType(String backendType) {
            this.backendType = backendType;
            return self();
        }

        /**
         * 设置S3服务端点
         *
         * @param endpoint S3服务端点URL
         * @return Builder实例，支持链式调用
         */
        public T endpoint(String endpoint) {
            this.endpoint = endpoint;
            return self();
        }

        /**
         * 设置存储区域
         *
         * @param region 存储区域标识
         * @return Builder实例，支持链式调用
         */
        public T region(String region) {
            this.region = region;
            return self();
        }

        /**
         * 设置访问密钥ID
         *
         * @param accessKeyId 访问密钥ID
         * @return Builder实例，支持链式调用
         */
        public T accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return self();
        }

        /**
         * 设置访问密钥Secret
         *
         * @param accessKeySecret 访问密钥Secret
         * @return Builder实例，支持链式调用
         */
        public T accessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
            return self();
        }

        /**
         * 设置存储桶名称
         *
         * @param bucket 存储桶名称
         * @return Builder实例，支持链式调用
         */
        public T bucket(String bucket) {
            this.bucket = bucket;
            return self();
        }

        /**
         * 设置对象键前缀
         *
         * @param keyPrefix 对象键前缀
         * @return Builder实例，支持链式调用
         */
        public T keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return self();
        }

        /**
         * 设置是否使用路径风格访问
         *
         * @param pathStyleAccess true使用路径风格，false使用虚拟主机风格
         * @return Builder实例，支持链式调用
         */
        public T pathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
            return self();
        }

        /**
         * 设置连接超时时间
         *
         * @param connectTimeout 连接超时时间
         * @return Builder实例，支持链式调用
         */
        public T connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return self();
        }

        /**
         * 设置读取超时时间
         *
         * @param readTimeout 读取超时时间
         * @return Builder实例，支持链式调用
         */
        public T readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return self();
        }

        /**
         * 设置最大连接数
         *
         * @param maxConnections 最大连接数
         * @return Builder实例，支持链式调用
         */
        public T maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return self();
        }

        /**
         * 设置是否启用SSL
         *
         * @param enableSsl true启用SSL，false使用HTTP
         * @return Builder实例，支持链式调用
         */
        public T enableSsl(boolean enableSsl) {
            this.enableSsl = enableSsl;
            return self();
        }

        /**
         * 返回具体Builder类型，支持方法链式调用
         */
        protected abstract T self();

        /**
         * 构建配置对象
         */
        public abstract StorageConfig build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageConfig that = (StorageConfig) o;
        return pathStyleAccess == that.pathStyleAccess && maxConnections == that.maxConnections
                && enableSsl == that.enableSsl && Objects.equals(backendType, that.backendType)
                && Objects.equals(endpoint, that.endpoint) && Objects.equals(region, that.region)
                && Objects.equals(accessKeyId, that.accessKeyId) && Objects.equals(accessKeySecret, that.accessKeySecret)
                && Objects.equals(bucket, that.bucket) && Objects.equals(keyPrefix, that.keyPrefix)
                && Objects.equals(connectTimeout, that.connectTimeout) && Objects.equals(readTimeout, that.readTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backendType, endpoint, region, accessKeyId, accessKeySecret, bucket, keyPrefix,
                pathStyleAccess, connectTimeout, readTimeout, maxConnections, enableSsl);
    }

    @Override
    public String toString() {
        return "StorageConfig{" + "backendType='" + backendType + '\'' + ", endpoint='" + endpoint + '\''
                + ", region='" + region + '\'' + ", accessKeyId='" + maskSensitive(accessKeyId) + '\''
                + ", bucket='" + bucket + '\'' + ", keyPrefix='" + keyPrefix + '\'' + ", pathStyleAccess="
                + pathStyleAccess + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
                + ", maxConnections=" + maxConnections + ", enableSsl=" + enableSsl + '}';
    }

    /**
     * 掩码敏感信息用于日志输出
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}