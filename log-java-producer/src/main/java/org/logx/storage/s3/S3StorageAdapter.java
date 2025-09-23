package org.logx.storage.s3;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS S3存储适配器实现
 * <p>
 * 基于AWS SDK v2实现的S3存储适配器，支持标准AWS S3服务。 提供高性能的单个和批量上传功能，支持大文件的multipart upload。
 * <p>
 * 主要特性：
 * <ul>
 * <li>智能分片上传：>5MB数据自动使用multipart upload</li>
 * <li>AWS特定错误处理和重试逻辑</li>
 * <li>区域自动检测和配置验证</li>
 * <li>连接健康检查和权限验证</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public final class S3StorageAdapter implements S3StorageInterface, AutoCloseable {

    private static final long MULTIPART_THRESHOLD = 5L * 1024 * 1024; // 5MB
    private static final int PART_SIZE = 5 * 1024 * 1024; // 5MB per part
    private static final String BACKEND_TYPE = "AWS_S3";

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;
    private final Executor executor;
    private final int maxRetries;
    private final long baseBackoffMs;
    private final long maxBackoffMs;

    /**
     * 构造AWS S3存储适配器
     *
     * @param region
     *            AWS区域，如 us-east-1
     * @param accessKeyId
     *            AWS访问密钥ID
     * @param secretAccessKey
     *            AWS访问密钥Secret
     * @param bucketName
     *            S3存储桶名称
     * @param keyPrefix
     *            对象键前缀，默认 "logs/"
     * @param maxRetries
     *            最大重试次数，默认3次
     * @param baseBackoffMs
     *            基础退避时间（毫秒），默认200ms
     * @param maxBackoffMs
     *            最大退避时间（毫秒），默认10000ms
     */
    public S3StorageAdapter(String region, String accessKeyId, String secretAccessKey, String bucketName,
            String keyPrefix, int maxRetries, long baseBackoffMs, long maxBackoffMs) {
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix != null ? keyPrefix.replaceAll("^/+|/+$", "") : "logs";
        this.maxRetries = Math.max(0, maxRetries);
        this.baseBackoffMs = Math.max(100L, baseBackoffMs);
        this.maxBackoffMs = Math.max(this.baseBackoffMs, maxBackoffMs);
        this.executor = ForkJoinPool.commonPool();

        // 构建S3客户端 - 标准AWS S3配置
        this.s3Client = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.of(region != null ? region : "us-east-1")).build();
    }

    /**
     * 简化构造函数：使用默认重试参数
     */
    public S3StorageAdapter(String region, String accessKeyId, String secretAccessKey, String bucketName) {
        this(region, accessKeyId, secretAccessKey, bucketName, "logs", 3, 200L, 10000L);
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
        if (s3Client != null) {
            s3Client.close();
        }
    }

    /**
     * 标准上传方式（用于小于5MB的数据）
     */
    private Void uploadStandard(String key, byte[] data) {
        PutObjectRequest putRequest = PutObjectRequest.builder().bucket(bucketName).key(key)
                .contentLength((long) data.length).build();

        RequestBody requestBody = RequestBody.fromBytes(data);

        return executeWithRetry(() -> {
            s3Client.putObject(putRequest, requestBody);
            return null;
        });
    }

    /**
     * 分片上传方式（用于大于5MB的数据）
     */
    private Void uploadMultipart(String key, byte[] data) {
        // 1. 初始化分片上传
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder().bucket(bucketName).key(key)
                .build();

        CreateMultipartUploadResponse createResponse = executeWithRetry(
                () -> s3Client.createMultipartUpload(createRequest));

        String uploadId = createResponse.uploadId();
        List<CompletedPart> completedParts = new ArrayList<>();

        try {
            // 2. 分片上传数据
            int partNumber = 1;
            int offset = 0;

            while (offset < data.length) {
                int length = Math.min(PART_SIZE, data.length - offset);
                byte[] partData = new byte[length];
                System.arraycopy(data, offset, partData, 0, length);

                UploadPartRequest partRequest = UploadPartRequest.builder().bucket(bucketName).key(key)
                        .uploadId(uploadId).partNumber(partNumber).build();

                RequestBody partBody = RequestBody.fromBytes(partData);

                UploadPartResponse partResponse = executeWithRetry(() -> s3Client.uploadPart(partRequest, partBody));

                CompletedPart completedPart = CompletedPart.builder().partNumber(partNumber).eTag(partResponse.eTag())
                        .build();

                completedParts.add(completedPart);

                offset += length;
                partNumber++;
            }

            // 3. 完成分片上传
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder().bucket(bucketName)
                    .key(key).uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build()).build();

            executeWithRetry(() -> {
                s3Client.completeMultipartUpload(completeRequest);
                return null;
            });

        } catch (Exception e) {
            // 上传失败时清理分片
            try {
                AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder().bucket(bucketName)
                        .key(key).uploadId(uploadId).build();
                s3Client.abortMultipartUpload(abortRequest);
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

                // AWS特定错误处理
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
        if (e instanceof S3Exception) {
            S3Exception s3e = (S3Exception) e;
            int statusCode = s3e.statusCode();

            // AWS S3特定的可重试错误
            if (statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504) {
                return true; // 服务器错误
            }

            if (statusCode == 429) {
                return true; // 限流错误
            }

            String errorCode = s3e.awsErrorDetails().errorCode();
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
