package org.logx.storage.sf;

import org.logx.storage.ProtocolType;
import org.logx.storage.StorageService;

import java.util.concurrent.CompletableFuture;

/**
 * SF OSS存储服务实现（用于SPI实例化）
 * <p>
 * 这个类是专门为Java SPI机制设计的，提供无参构造函数。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class SfOssStorageServiceProvider implements StorageService {
    private static final ProtocolType PROTOCOL_TYPE = ProtocolType.SF_OSS;

    /**
     * 构造SF OSS存储服务（用于SPI实例化）
     */
    public SfOssStorageServiceProvider() {
        // 无参构造函数用于SPI实例化
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        throw new IllegalStateException("This SPI provider should not be used directly. Use StorageServiceFactory instead.");
    }

    @Override
    public ProtocolType getProtocolType() {
        return PROTOCOL_TYPE;
    }

    @Override
    public String getBucketName() {
        throw new IllegalStateException("This SPI provider should not be used directly. Use StorageServiceFactory instead.");
    }

    @Override
    public String getKeyPrefix() {
        throw new IllegalStateException("This SPI provider should not be used directly. Use StorageServiceFactory instead.");
    }

    @Override
    public void close() {
        // 无需关闭资源
    }

    @Override
    public boolean supportsProtocol(ProtocolType protocol) {
        return PROTOCOL_TYPE == protocol;
    }
}