package org.logx.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.storage.StorageBackend;
import org.logx.storage.StorageConfig;
import org.logx.storage.s3.S3StorageFactory;
import org.logx.storage.StorageInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * S3StorageInterface接口测试
 * <p>
 * 验证S3存储接口的设计正确性，包括方法签名、参数验证、异步操作等。 使用工厂创建的模拟适配器进行测试。
 */
class S3StorageInterfaceTest {

    private StorageInterface storage;

    @BeforeEach
    void setUp() {
        // 使用工厂创建测试用的存储适配器
        TestS3StorageConfig config = TestS3StorageConfig.builder().endpoint("https://test.example.com")
                .region("test-region").accessKeyId("test-key").accessKeySecret("test-secret").bucket("test-bucket")
                .build();

        storage = S3StorageFactory.createAdapter(StorageBackend.GENERIC_S3, config);
    }

    @Test
    void testPutObjectWithValidParameters() {
        // Given
        String key = "test/log.txt";
        byte[] data = "test log content".getBytes();

        // When
        CompletableFuture<Void> future = storage.putObject(key, data);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void testPutObjectWithNullKey() {
        // Given
        String key = null;
        byte[] data = "test content".getBytes();

        // When
        CompletableFuture<Void> future = storage.putObject(key, data);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(future::get).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key cannot be null");
    }

    @Test
    void testPutObjectWithEmptyKey() {
        // Given
        String key = "";
        byte[] data = "test content".getBytes();

        // When
        CompletableFuture<Void> future = storage.putObject(key, data);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(future::get).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key cannot be null or empty");
    }

    @Test
    void testPutObjectWithNullData() {
        // Given
        String key = "test/log.txt";
        byte[] data = null;

        // When
        CompletableFuture<Void> future = storage.putObject(key, data);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(future::get).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data cannot be null");
    }

    @Test
    void testPutObjectsWithValidParameters() {
        // Given
        Map<String, byte[]> objects = new HashMap<>();
        objects.put("test/log1.txt", "log1 content".getBytes());
        objects.put("test/log2.txt", "log2 content".getBytes());

        // When
        CompletableFuture<Void> future = storage.putObjects(objects);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void testPutObjectsWithNullMap() {
        // Given
        Map<String, byte[]> objects = null;

        // When
        CompletableFuture<Void> future = storage.putObjects(objects);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(future::get).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objects cannot be null");
    }

    @Test
    void testPutObjectsWithEmptyMap() {
        // Given
        Map<String, byte[]> objects = new HashMap<>();

        // When
        CompletableFuture<Void> future = storage.putObjects(objects);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(future::get).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("objects cannot be null or empty");
    }

    @Test
    void testGetBackendType() {
        // When
        String backendType = storage.getBackendType();

        // Then
        assertThat(backendType).isNotNull();
        assertThat(backendType).isNotEmpty();
        assertThat(backendType).isEqualTo("GENERIC_S3");
    }

    @Test
    void testGetBucketName() {
        // When
        String bucketName = storage.getBucketName();

        // Then
        assertThat(bucketName).isNotNull();
        assertThat(bucketName).isNotEmpty();
        assertThat(bucketName).isEqualTo("test-bucket");
    }

    /**
     * 测试用的S3StorageConfig实现
     */
    static class TestS3StorageConfig extends StorageConfig {

        public TestS3StorageConfig(Builder builder) {
            super(builder);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends StorageConfig.Builder<Builder> {
            public Builder endpoint(String endpoint) {
                return super.endpoint(endpoint);
            }
            
            public Builder region(String region) {
                return super.region(region);
            }
            
            public Builder accessKeyId(String accessKeyId) {
                return super.accessKeyId(accessKeyId);
            }
            
            public Builder accessKeySecret(String accessKeySecret) {
                return super.accessKeySecret(accessKeySecret);
            }
            
            public Builder bucket(String bucket) {
                return super.bucket(bucket);
            }

            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public TestS3StorageConfig build() {
                return new TestS3StorageConfig(this);
            }
        }
    }
}
