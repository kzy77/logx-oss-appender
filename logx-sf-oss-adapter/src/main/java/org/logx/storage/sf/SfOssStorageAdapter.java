package org.logx.storage.sf;

import org.logx.storage.StorageInterface;

import java.util.concurrent.CompletableFuture;

/**
 * SF OSS存储适配器实现
 * <p>
 * 基于SF OSS特有SDK实现的存储适配器，支持SF OSS服务。
 * 提供基本的单个上传功能，分片和重试等通用功能由核心层统一处理。
 * <p>
 * 注意：根据2025-09-24的架构变更，该适配器不再处理数据分片逻辑和重试机制，
 * 这些功能已移至核心层的BatchProcessor中统一处理。
 * <p>
 * 主要特性：
 * <ul>
 * <li>SF OSS特定配置和认证</li>
 * <li>区域配置和键前缀处理</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class SfOssStorageAdapter implements StorageInterface, AutoCloseable {

    private static final String OSS_TYPE = "SF_OSS";

    private final LogxSfOssClient logxSfOssClient;
    private final String bucketName;
    private final String keyPrefix;

    /**
     * 构造SF OSS存储适配器
     *
     * @param config
     *            SF OSS存储配置
     * @param keyPrefix
     *            对象键前缀，默认 "logs/"
     */
    public SfOssStorageAdapter(org.logx.storage.StorageConfig config, String keyPrefix) {
        this.bucketName = config.getBucket();
        this.keyPrefix = keyPrefix != null ? keyPrefix.replaceAll("^/+|/+$", "") : "logs";

        // 构建SF OSS客户端
        this.logxSfOssClient = new LogxSfOssClient(config.getEndpoint(), config.getRegion(), config.getAccessKeyId(), config.getAccessKeySecret());
    }

    /**
     * 简化构造函数：使用默认参数
     */
    public SfOssStorageAdapter(org.logx.storage.StorageConfig config) {
        this(config, "logs");
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        if (key == null || key.trim().isEmpty()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Key cannot be null or empty"));
            return future;
        }
        if (data == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Data cannot be null"));
            return future;
        }

        String fullKey = buildFullKey(key);

        // 执行标准上传，不处理分片和重试，这些由核心层处理
        return CompletableFuture.runAsync(() -> {
            try {
                logxSfOssClient.putObject(bucketName, fullKey, data);
            } catch (Exception e) {
                // 直接抛出异常，由核心层处理重试和错误处理
                throw new RuntimeException("Failed to upload object to SF OSS: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public String getOssType() {
        return OSS_TYPE;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void close() {
        if (logxSfOssClient != null) {
            logxSfOssClient.close();
        }
    }

    /**
     * 构建完整的对象键
     */
    private String buildFullKey(String key) {
        if (keyPrefix.isEmpty()) {
            return key;
        }
        return keyPrefix + "/" + key;
    }
}