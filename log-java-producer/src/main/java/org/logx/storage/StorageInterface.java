package org.logx.storage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 存储接口
 * <p>
 * 定义所有存储服务的通用接口，支持S3兼容存储（AWS S3、阿里云OSS、MinIO等）和非S3兼容存储（如SF OSS）。
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
     * 批量上传多个对象
     *
     * @param objects
     *            对象映射，键为对象键，值为对象数据
     *
     * @return CompletableFuture表示异步操作结果
     */
    CompletableFuture<Void> putObjects(Map<String, byte[]> objects);

    /**
     * 获取后端类型
     *
     * @return 后端类型字符串
     */
    String getBackendType();

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    String getBucketName();
}