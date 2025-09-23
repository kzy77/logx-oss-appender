package org.logx.storage.sf;

import org.logx.storage.StorageInterface;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SF OSS存储适配器实现
 * <p>
 * 基于SF OSS特有SDK实现的存储适配器，支持SF OSS服务。
 * 提供高性能的单个和批量上传功能，支持大文件的分片上传。
 * <p>
 * 主要特性：
 * <ul>
 * <li>智能分片上传：>5MB数据自动使用分片上传</li>
 * <li>SF OSS特定错误处理和重试逻辑</li>
 * <li>连接健康检查和权限验证</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class SfOssStorageAdapter implements StorageInterface, AutoCloseable {

    private static final long MULTIPART_THRESHOLD = 5L * 1024 * 1024; // 5MB
    private static final int PART_SIZE = 5 * 1024 * 1024; // 5MB per part
    private static final String BACKEND_TYPE = "SF_OSS";

    private final SfOssClient sfOssClient;
    private final String bucketName;
    private final String keyPrefix;
    private final Executor executor;
    private final int maxRetries;
    private final long baseBackoffMs;
    private final long maxBackoffMs;

    /**
     * 构造SF OSS存储适配器
     *
     * @param config
     *            SF OSS存储配置
     * @param keyPrefix
     *            对象键前缀，默认 "logs/"
     * @param maxRetries
     *            最大重试次数，默认3次
     * @param baseBackoffMs
     *            基础退避时间（毫秒），默认200ms
     * @param maxBackoffMs
     *            最大退避时间（毫秒），默认10000ms
     */
    public SfOssStorageAdapter(org.logx.storage.StorageConfig config, String keyPrefix, int maxRetries, long baseBackoffMs, long maxBackoffMs) {
        this.bucketName = config.getBucket();
        this.keyPrefix = keyPrefix != null ? keyPrefix.replaceAll("^/+|/+$", "") : "logs";
        this.maxRetries = Math.max(0, maxRetries);
        this.baseBackoffMs = Math.max(100L, baseBackoffMs);
        this.maxBackoffMs = Math.max(this.baseBackoffMs, maxBackoffMs);
        this.executor = ForkJoinPool.commonPool();

        // 构建SF OSS客户端
        this.sfOssClient = new SfOssClient(config.getEndpoint(), config.getRegion(), config.getAccessKeyId(), config.getAccessKeySecret());
    }

    /**
     * 简化构造函数：使用默认重试参数
     */
    public SfOssStorageAdapter(org.logx.storage.StorageConfig config) {
        this(config, "logs", 3, 200L, 10000L);
    }

    @Override
    public CompletableFuture<Void> putObject(String key, byte[] data) {
        if (key == null || key.trim().isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Key cannot be null or empty"));
        }
        if (data == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Data cannot be null"));
        }

        String fullKey = buildFullKey(key);

        return CompletableFuture.supplyAsync(() -> {
            // 根据数据大小选择上传方式
            if (data.length > MULTIPART_THRESHOLD) {
                return uploadMultipart(fullKey, data);
            } else {
                return uploadStandard(fullKey, data);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> putObjects(Map<String, byte[]> objects) {
        if (objects == null || objects.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Objects cannot be null or empty"));
        }

        // 验证所有key和data都不为空
        for (Map.Entry<String, byte[]> entry : objects.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                return CompletableFuture
                        .failedFuture(new IllegalArgumentException("Object key cannot be null or empty"));
            }
            if (entry.getValue() == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Object data cannot be null"));
            }
        }

        // 并行批量上传
        List<CompletableFuture<Void>> futures = objects.entrySet().stream()
                .map(entry -> putObject(entry.getKey(), entry.getValue())).collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    

    @Override
    public String getBackendType() {
        return BACKEND_TYPE;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void close() {
        if (sfOssClient != null) {
            sfOssClient.close();
        }
    }

    /**
     * 标准上传方式（用于小于5MB的数据）
     */
    private Void uploadStandard(String key, byte[] data) {
        return executeWithRetry(() -> {
            sfOssClient.putObject(bucketName, key, data);
            return null;
        });
    }

    /**
     * 分片上传方式（用于大于5MB的数据）
     */
    private Void uploadMultipart(String key, byte[] data) {
        // 1. 初始化分片上传
        String uploadId = executeWithRetry(() -> sfOssClient.initiateMultipartUpload(bucketName, key));

        List<PartETag> partETags = new ArrayList<>();

        try {
            // 2. 分片上传数据
            int partNumber = 1;
            int offset = 0;

            while (offset < data.length) {
                final int currentPartNumber = partNumber;
                int length = Math.min(PART_SIZE, data.length - offset);
                byte[] partData = new byte[length];
                System.arraycopy(data, offset, partData, 0, length);

                PartETag partETag = executeWithRetry(() -> 
                    sfOssClient.uploadPart(bucketName, key, uploadId, currentPartNumber, partData));

                partETags.add(partETag);

                offset += length;
                partNumber++;
            }

            // 3. 完成分片上传
            executeWithRetry(() -> {
                sfOssClient.completeMultipartUpload(bucketName, key, uploadId, partETags);
                return null;
            });

        } catch (Exception e) {
            // 上传失败时清理分片
            try {
                sfOssClient.abortMultipartUpload(bucketName, key, uploadId);
            } catch (Exception abortException) {
                // 忽略清理失败
            }
            throw new RuntimeException("Multipart upload failed", e);
        }

        return null;
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

    /**
     * 带重试的执行器
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;

                if (attempt >= maxRetries) {
                    break;
                }

                // SF OSS特定错误处理
                if (isRetryableException(e)) {
                    try {
                        Thread.sleep(computeBackoff(attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted", ie);
                    }
                } else {
                    // 不可重试的错误直接抛出
                    break;
                }
            }
        }

        throw new RuntimeException("Operation failed after " + (maxRetries + 1) + " attempts", lastException);
    }

    /**
     * 判断异常是否可以重试
     */
    private boolean isRetryableException(Exception e) {
        // SF OSS特定的可重试错误
        if (e instanceof SfOssException) {
            SfOssException sfEx = (SfOssException) e;
            int statusCode = sfEx.getStatusCode();

            if (statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504) {
                return true; // 服务器错误
            }

            if (statusCode == 429) {
                return true; // 限流错误
            }

            String errorCode = sfEx.getErrorCode();
            if ("ThrottlingException".equals(errorCode) || "InternalError".equals(errorCode)
                    || "ServiceUnavailable".equals(errorCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 计算指数退避时间（含抖动）
     */
    private long computeBackoff(int attempt) {
        long exp = Math.min(maxBackoffMs, (long) (baseBackoffMs * Math.pow(2, attempt)));
        long jitter = (long) (Math.random() * exp / 3);
        return Math.min(maxBackoffMs, exp + jitter);
    }

    /**
     * 可重试操作的函数式接口
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}