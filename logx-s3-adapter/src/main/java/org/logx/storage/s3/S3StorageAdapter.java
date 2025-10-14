package org.logx.storage.s3;

import org.logx.storage.ProtocolType;
import org.logx.storage.StorageInterface;
import org.logx.storage.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.concurrent.CompletableFuture;


/**
 * AWS S3存储适配器实现
 * <p>
 * 基于AWS SDK v2实现的S3存储适配器，支持标准AWS S3服务。
 * 提供基本的单个上传功能，分片和重试等通用功能由核心层统一处理。
 * <p>
 * 注意：根据2025-09-24的架构变更，该适配器不再处理数据分片逻辑和重试机制，
 * 这些功能已移至核心层的BatchProcessor中统一处理。
 * <p>
 * 主要特性：
 * <ul>
 * <li>基本上传功能：处理单个对象的上传</li>
 * <li>AWS特定配置和认证</li>
 * <li>区域配置和键前缀处理</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class S3StorageAdapter implements StorageInterface, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageAdapter.class);
    private static final ProtocolType PROTOCOL_TYPE = ProtocolType.S3;

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;

    /**
     * 构造S3存储适配器
     *
     * @param config
     *            存储配置
     * @param keyPrefix
     *            对象键前缀，默认 "logs/"
     */
    public S3StorageAdapter(StorageConfig config, String keyPrefix) {
        String region = config.getRegion();
        String accessKeyId = config.getAccessKeyId();
        String secretAccessKey = config.getAccessKeySecret();
        String bucketName = config.getBucket();
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix != null ? keyPrefix.replaceAll("^/+|/+$", "") : "logs";

        // 构建S3客户端 - 标准AWS S3配置
        this.s3Client = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.of(region != null ? region : "US")).build();
    }

    /**
     * 简化构造函数：使用默认参数
     */
    public S3StorageAdapter(StorageConfig config) {
        this(config, config.getKeyPrefix() != null ? config.getKeyPrefix() : "logs");
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
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullKey)
                    .contentLength((long) data.length)
                    .build();

            RequestBody requestBody = RequestBody.fromBytes(data);
            
            try {
                s3Client.putObject(putRequest, requestBody);
            } catch (Exception e) {
                // 直接抛出异常，由核心层处理重试和错误处理
                throw new RuntimeException("Failed to upload object to S3: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public ProtocolType getProtocolType() {
        return PROTOCOL_TYPE;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    /**
     * 构建完整的对象键
     */
    private String buildFullKey(String key) {
        // 如果keyPrefix为空或ObjectNameGenerator生成的key已经是完整路径，则直接返回
        if (keyPrefix.isEmpty() || isFullPathKey(key)) {
            return key;
        }
        return keyPrefix + "/" + key;
    }
    
    /**
     * 判断是否为完整路径键
     * ObjectNameGenerator生成的路径包含日期结构，如 application_2025-09/30/05:07:02:972_192.168.130.118.log
     */
    private boolean isFullPathKey(String key) {
        // 检查是否包含日期路径结构
        return key != null && key.contains("_") && key.contains("/") && key.contains(":");
    }
}
