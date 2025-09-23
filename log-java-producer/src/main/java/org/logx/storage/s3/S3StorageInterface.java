package org.logx.storage.s3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * S3兼容存储的统一接口抽象
 * <p>
 * 支持AWS S3、阿里云OSS、MinIO等S3兼容存储服务， 通过统一接口提供日志数据的异步上传能力。
 * <p>
 * 实现类必须保证线程安全，支持并发操作。 所有方法都应该是非阻塞的，使用CompletableFuture提供异步操作支持。
 * <p>
 * 接口设计基于S3标准API，确保与主流S3兼容存储服务的互操作性：
 * <ul>
 * <li>AWS S3 - 原生S3服务</li>
 * <li>阿里云OSS - 通过S3兼容API</li>
 * <li>腾讯云COS - 通过S3兼容API</li>
 * <li>MinIO - 开源S3兼容存储</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public interface S3StorageInterface {

    /**
     * 异步上传单个对象到S3存储
     * <p>
     * 这是核心上传方法，将指定的数据异步上传到S3兼容存储中。 方法遵循S3 putObject API标准，支持所有S3兼容存储后端。
     *
     * @param key
     *            存储对象的键（路径），不能为空或空字符串
     * @param data
     *            要上传的数据内容，不能为空
     *
     * @return CompletableFuture 异步上传操作的结果，成功时完成，失败时包含异常信息
     *
     * @throws IllegalArgumentException
     *             如果key为空或data为空
     */
    CompletableFuture<Void> putObject(String key, byte[] data);

    /**
     * 异步批量上传多个对象到S3存储
     * <p>
     * 提供批量上传能力，可以在一次调用中上传多个对象。 实现应该优化批量操作的性能，并保证操作的原子性。
     *
     * @param objects
     *            要上传的对象映射，key为存储路径，value为数据内容
     *
     * @return CompletableFuture 异步批量上传操作的结果
     *
     * @throws IllegalArgumentException
     *             如果objects为空，或包含空的key或data
     */
    CompletableFuture<Void> putObjects(Map<String, byte[]> objects);

    

    /**
     * 获取存储后端的类型标识
     * <p>
     * 返回当前实现所对应的存储后端类型，用于日志记录、监控和调试。
     *
     * @return 存储后端类型的字符串标识（如"AWS_S3", "ALIYUN_OSS", "MINIO"等）
     */
    String getBackendType();

    /**
     * 获取当前使用的存储桶名称
     * <p>
     * 返回配置的S3存储桶名称，用于日志记录和调试。
     *
     * @return 存储桶名称
     */
    String getBucketName();
}
