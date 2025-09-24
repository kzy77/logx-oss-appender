package org.logx.storage.s3;

import org.logx.storage.StorageService;
import org.logx.storage.StorageConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * S3存储服务实现
 * <p>
 * 基于S3StorageAdapter实现的存储服务，支持标准AWS S3服务和S3兼容存储。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class S3StorageService implements StorageService {
    private final S3StorageAdapter s3Adapter;
    private static final String BACKEND_TYPE = "S3";

    /**
     * 构造S3存储服务
     *
     * @param config 存储配置
     */
    public S3StorageService(StorageConfig config) {
        this.s3Adapter = new S3StorageAdapter(
            config,
            config.getKeyPrefix() != null ? config.getKeyPrefix() : "logs",
            3,  // maxRetries
            200L,  // baseBackoffMs
            10000L  // maxBackoffMs
        );
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        return s3Adapter.putObject(key, data);
    }

    @Override
    public CompletableFuture<Void> putObjects(Map<String, byte[]> objects) {
        return s3Adapter.putObjects(objects);
    }

    @Override
    public String getBackendType() {
        return BACKEND_TYPE;
    }

    @Override
    public String getBucketName() {
        return s3Adapter.getBucketName();
    }

    @Override
    public void close() {
        s3Adapter.close();
    }

    @Override
    public boolean supportsBackend(String backendType) {
        return BACKEND_TYPE.equalsIgnoreCase(backendType) || 
               "AWS_S3".equalsIgnoreCase(backendType) ||
               "ALIYUN_OSS".equalsIgnoreCase(backendType) ||
               "TENCENT_COS".equalsIgnoreCase(backendType) ||
               "MINIO".equalsIgnoreCase(backendType) ||
               "HUAWEI_OBS".equalsIgnoreCase(backendType) ||
               "GENERIC_S3".equalsIgnoreCase(backendType);
    }

    /**
     * 创建S3存储服务实例
     *
     * @param config 存储配置
     * @return S3存储服务实例
     * @throws IllegalStateException 如果AWS SDK不在classpath中
     */
    public static StorageService create(StorageConfig config) {
        // 检查AWS SDK是否在classpath中
        try {
            Class.forName("software.amazon.awssdk.services.s3.S3Client");
            return new S3StorageService(config);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("AWS SDK not found in classpath. Please add AWS SDK dependency to use S3 storage.", e);
        }
    }
}