package org.logx.storage.s3;

import org.logx.storage.StorageService;
import org.logx.storage.StorageConfig;

import java.util.concurrent.CompletableFuture;

/**
 * S3存储服务实现（用于SPI实例化）
 * <p>
 * 这个类是专门为Java SPI机制设计的，提供无参构造函数。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class S3StorageServiceProvider implements StorageService {
    private static final String OSS_TYPE = "S3";

    /**
     * 构造S3存储服务（用于SPI实例化）
     */
    public S3StorageServiceProvider() {
        // 无参构造函数用于SPI实例化
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        throw new IllegalStateException("This SPI provider should not be used directly. Use StorageServiceFactory instead.");
    }

    @Override
    public String getOssType() {
        return OSS_TYPE;
    }

    @Override
    public String getBucketName() {
        throw new IllegalStateException("This SPI provider should not be used directly. Use StorageServiceFactory instead.");
    }

    @Override
    public void close() {
        // 无需关闭资源
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
}