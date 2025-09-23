package org.logx.storage.sf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.storage.StorageConfig;
import org.logx.storage.s3.AwsS3Config;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * SfOssStorageAdapter测试
 * <p>
 * 验证SF OSS存储适配器的构建和基本功能。
 */
class SfOssStorageAdapterTest {

    @Test
    void testAdapterConstructionWithAllParameters() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();
        
        String keyPrefix = "logs/";
        int maxRetries = 3;
        long baseBackoffMs = 200L;
        long maxBackoffMs = 10000L;

        // When
        SfOssStorageAdapter adapter = new SfOssStorageAdapter(
                config, keyPrefix, maxRetries, baseBackoffMs, maxBackoffMs);

        try {
            // Then
            assertThat(adapter.getBackendType()).isEqualTo("SF_OSS");
            assertThat(adapter.getBucketName()).isEqualTo("test-bucket");
        } finally {
            adapter.close();
        }
    }

    @Test
    void testAdapterConstructionWithMinimalParameters() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        // When
        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);

        try {
            // Then
            assertThat(adapter.getBackendType()).isEqualTo("SF_OSS");
            assertThat(adapter.getBucketName()).isEqualTo("test-bucket");
        } finally {
            adapter.close();
        }
    }

    @Test
    void testPutObjectWithValidParameters() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();
        
        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);

        try {
            String key = "test/log.txt";
            byte[] data = "test log content".getBytes();

            // When
            CompletableFuture<Void> future = adapter.putObject(key, data);

            // Then
            assertThat(future).isNotNull();
            // Note: We can't easily test the actual upload without a real SF OSS service
            // In a real test, we would mock the S3Client or use a test double
        } finally {
            adapter.close();
        }
    }

    @Test
    void testPutObjectWithNullKey() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);
        try {
            // When & Then
            CompletableFuture<Void> future = adapter.putObject(null, "test data".getBytes());

            assertThatThrownBy(() -> future.get())
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Key cannot be null or empty");
        } finally {
            adapter.close();
        }
    }

    @Test
    void testPutObjectWithEmptyKey() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);
        try {
            // When & Then
            CompletableFuture<Void> future = adapter.putObject("", "test data".getBytes());

            assertThatThrownBy(() -> future.get())
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Key cannot be null or empty");
        } finally {
            adapter.close();
        }
    }

    @Test
    void testPutObjectWithNullData() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);
        try {
            // When & Then
            CompletableFuture<Void> future = adapter.putObject("test-key", null);

            assertThatThrownBy(() -> future.get())
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Data cannot be null");
        } finally {
            adapter.close();
        }
    }

    @Test
    void testPutObjectsWithValidParameters() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();
        
        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);

        Map<String, byte[]> objects = new HashMap<>();
        objects.put("test/log1.txt", "log1 content".getBytes());
        objects.put("test/log2.txt", "log2 content".getBytes());

        // When
        CompletableFuture<Void> future = adapter.putObjects(objects);

        // Then
        assertThat(future).isNotNull();
        // Note: We can't easily test the actual upload without a real SF OSS service
    }

    @Test
    void testPutObjectsWithNullDataInMap() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);
        try {
            Map<String, byte[]> objects = new HashMap<>();
            objects.put("key", null);

            // When & Then
            CompletableFuture<Void> future = adapter.putObjects(objects);

            assertThatThrownBy(() -> future.get())
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Object data cannot be null");
        } finally {
            adapter.close();
        }
    }

    @Test
    void testPutObjectsWithEmptyMap() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();

        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);
        try {
            // When & Then
            CompletableFuture<Void> future = adapter.putObjects(new HashMap<>());

            assertThatThrownBy(() -> future.get())
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Objects cannot be null or empty");
        } finally {
            adapter.close();
        }
    }

    @Test
    void testGetBackendType() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();
        
        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);

        // When
        String backendType = adapter.getBackendType();

        // Then
        assertThat(backendType).isNotNull();
        assertThat(backendType).isNotEmpty();
        assertThat(backendType).isEqualTo("SF_OSS");
    }

    @Test
    void testGetBucketName() {
        // Given
        String bucketName = "test-bucket";
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket(bucketName)
                .build();
        
        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);

        // When
        String result = adapter.getBucketName();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).isEqualTo(bucketName);
    }

    @Test
    void testClose() {
        // Given
        StorageConfig config = AwsS3Config.builder()
                .endpoint("https://sf-oss-cn-north-1.sf-oss.com")
                .region("cn-north-1")
                .accessKeyId("test-access-key")
                .accessKeySecret("test-secret-key")
                .bucket("test-bucket")
                .build();
        
        SfOssStorageAdapter adapter = new SfOssStorageAdapter(config);

        // When & Then
        assertThatNoException().isThrownBy(adapter::close);
    }
}