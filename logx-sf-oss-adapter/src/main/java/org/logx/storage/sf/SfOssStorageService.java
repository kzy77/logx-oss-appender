package org.logx.storage.sf;

import org.logx.storage.StorageService;
import org.logx.storage.StorageConfig;

import java.util.concurrent.CompletableFuture;

/**
 * SF OSS存储服务实现
 * <p>
 * 基于SfOssStorageAdapter实现的存储服务，支持SF OSS存储。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class SfOssStorageService implements StorageService {
    private final SfOssStorageAdapter sfOssAdapter;
    private static final String BACKEND_TYPE = "SF_OSS";

    /**
     * 构造SF OSS存储服务
     *
     * @param config 存储配置
     */
    public SfOssStorageService(StorageConfig config) {
        this.sfOssAdapter = new SfOssStorageAdapter(
            config,
            config.getKeyPrefix() != null ? config.getKeyPrefix() : "logs",
            3,  // maxRetries
            200L,  // baseBackoffMs
            10000L  // maxBackoffMs
        );
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        return sfOssAdapter.putObject(key, data);
    }

    @Override
    public String getBackendType() {
        return BACKEND_TYPE;
    }

    @Override
    public String getBucketName() {
        return sfOssAdapter.getBucketName();
    }

    @Override
    public void close() {
        sfOssAdapter.close();
    }

    @Override
    public boolean supportsBackend(String backendType) {
        return BACKEND_TYPE.equalsIgnoreCase(backendType);
    }

    /**
     * 创建SF OSS存储服务实例
     *
     * @param config 存储配置
     * @return SF OSS存储服务实例
     */
    public static StorageService create(StorageConfig config) {
        return new SfOssStorageService(config);
    }
}