package org.logx.storage;

import java.util.concurrent.CompletableFuture;

/**
 * 存储接口
 * <p>
 * 定义所有存储服务的通用接口，支持多种存储后端（S3兼容存储、SF OSS等）。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public interface StorageInterface {

    /**
     * 上传单个对象
     *
     * @param key
     *            对象键
     * @param data
     *            对象数据
     *
     * @return CompletableFuture表示异步操作结果
     */
    CompletableFuture<Void> putObject(String key, byte[] data);

    /**
     * 获取OSS类型
     *
     * @return OSS类型字符串
     */
    String getOssType();

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    String getBucketName();

    /**
     * 关闭存储服务，释放资源
     */
    void close();
}