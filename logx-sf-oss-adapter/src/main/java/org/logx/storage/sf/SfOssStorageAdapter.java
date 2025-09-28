package org.logx.storage.sf;

import org.logx.storage.StorageInterface;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * SF OSS存储适配器实现
 * <p>
 * 基于SF OSS特有SDK实现的存储适配器，支持SF OSS服务。
 * 提供高性能的单个和批量上传功能。
 * <p>
 * 主要特性：
 * <ul>
 * <li>SF OSS特定错误处理和重试逻辑</li>
 * <li>连接健康检查和权限验证</li>
 * </ul>
 * <p>
 * 注意：根据2025-09-24的架构变更，该适配器不再处理数据分片逻辑，
 * 分片处理已移至核心层的BatchProcessor中统一处理。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class SfOssStorageAdapter implements StorageInterface, AutoCloseable {

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

        return CompletableFuture.supplyAsync(() -> {
            return uploadStandard(fullKey, data);
        }, executor);
    }

    

    

    @Override
    public String getOssType() {
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
     * 标准上传方式
     */
    private Void uploadStandard(String key, byte[] data) {
        return executeWithRetry(() -> {
            sfOssClient.putObject(bucketName, key, data);
            return null;
        });
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