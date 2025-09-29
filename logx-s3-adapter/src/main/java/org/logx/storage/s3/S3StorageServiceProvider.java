package org.logx.storage.s3;

import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import java.util.concurrent.CompletableFuture;

/**
 * S3存储服务提供者实现
 * <p>
 * 这个类是专门为Java SPI机制设计的，提供无参构造函数。
 * 它会创建实际的S3StorageAdapter实例。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class S3StorageServiceProvider implements StorageService {
    private static final String OSS_TYPE = "S3";
    private S3StorageAdapter adapter;

    /**
     * 构造S3存储服务（用于SPI实例化）
     */
    public S3StorageServiceProvider() {
        // 无参构造函数用于SPI实例化
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        ensureInitialized();
        return adapter.putObject(key, data);
    }

    @Override
    public String getOssType() {
        return OSS_TYPE;
    }

    @Override
    public String getBucketName() {
        ensureInitialized();
        return adapter.getBucketName();
    }

    @Override
    public void close() {
        if (adapter != null) {
            adapter.close();
        }
    }

    @Override
    public boolean supportsOssType(String ossType) {
        return OSS_TYPE.equalsIgnoreCase(ossType) ||
               "AWS_S3".equalsIgnoreCase(ossType) ||
               "ALIYUN_OSS".equalsIgnoreCase(ossType) ||
               "TENCENT_COS".equalsIgnoreCase(ossType) ||
               "MINIO".equalsIgnoreCase(ossType) ||
               "HUAWEI_OBS".equalsIgnoreCase(ossType) ||
               "GENERIC_S3".equalsIgnoreCase(ossType);
    }

    /**
     * 初始化存储服务
     * @param config 存储配置
     */
    public void initialize(StorageConfig config) {
        if (this.adapter == null) {
            this.adapter = new S3StorageAdapter(config);
        }
    }

    /**
     * 确保存储服务已初始化
     */
    private void ensureInitialized() {
        if (adapter == null) {
            throw new IllegalStateException("Storage service not initialized. Use StorageServiceFactory instead.");
        }
    }
}