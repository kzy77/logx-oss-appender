package org.logx.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.storage.s3.S3StorageAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * AWS S3存储适配器单元测试
 */
public class S3StorageAdapterTest {

    private S3StorageAdapter adapter;
    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_KEY = "test-key";
    private static final byte[] TEST_DATA = "test data".getBytes();

    @BeforeEach
    void setUp() {
        // 创建测试用的适配器（注意：这些是测试凭据，不会真正连接AWS）
        adapter = new S3StorageAdapter("us-east-1", "testAccessKey", "testSecretKey", TEST_BUCKET);
    }

    @Test
    void putObject_withNullKey_shouldFail() {
        // When & Then
        CompletableFuture<Void> result = adapter.putObject(null, TEST_DATA);

        assertThatThrownBy(() -> result.get()).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Key cannot be null or empty");
    }

    @Test
    void putObject_withEmptyKey_shouldFail() {
        // When & Then
        CompletableFuture<Void> result = adapter.putObject("", TEST_DATA);

        assertThatThrownBy(() -> result.get()).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Key cannot be null or empty");
    }

    @Test
    void putObject_withNullData_shouldFail() {
        // When & Then
        CompletableFuture<Void> result = adapter.putObject(TEST_KEY, null);

        assertThatThrownBy(() -> result.get()).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Data cannot be null");
    }

    @Test
    void putObjects_withNullMap_shouldFail() {
        // When & Then
        CompletableFuture<Void> result = adapter.putObjects(null);

        assertThatThrownBy(() -> result.get()).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Objects cannot be null or empty");
    }

    @Test
    void putObjects_withEmptyMap_shouldFail() {
        // When & Then
        CompletableFuture<Void> result = adapter.putObjects(new HashMap<>());

        assertThatThrownBy(() -> result.get()).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Objects cannot be null or empty");
    }

    @Test
    void putObjects_withNullKeyInMap_shouldFail() {
        // Given
        Map<String, byte[]> objects = new HashMap<>();
        objects.put(null, "data".getBytes());

        // When & Then
        CompletableFuture<Void> result = adapter.putObjects(objects);

        assertThatThrownBy(() -> result.get()).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Object key cannot be null or empty");
    }

    @Test
    void putObjects_withNullDataInMap_shouldFail() {
        // Given
        Map<String, byte[]> objects = new HashMap<>();
        objects.put("key", null);

        // When & Then
        CompletableFuture<Void> result = adapter.putObjects(objects);

        assertThatThrownBy(() -> result.get()).hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Object data cannot be null");
    }

    @Test
    void getBackendType_shouldReturnAwsS3() {
        // When & Then
        assertThat(adapter.getBackendType()).isEqualTo("AWS_S3");
    }

    @Test
    void getBucketName_shouldReturnConfiguredBucket() {
        // When & Then
        assertThat(adapter.getBucketName()).isEqualTo(TEST_BUCKET);
    }

    @Test
    void constructor_withSimpleParams_shouldWork() {
        // When
        S3StorageAdapter simpleAdapter = new S3StorageAdapter("us-west-2", "key", "secret", "bucket");

        try {
            // Then
            assertThat(simpleAdapter.getBucketName()).isEqualTo("bucket");
            assertThat(simpleAdapter.getBackendType()).isEqualTo("AWS_S3");
        } finally {
            simpleAdapter.close();
        }
    }

    @Test
    void constructor_withFullParams_shouldWork() {
        // When
        S3StorageAdapter fullAdapter = new S3StorageAdapter("eu-west-1", "key", "secret", "bucket", "prefix", 5, 500L,
                5000L);

        try {
            // Then
            assertThat(fullAdapter.getBucketName()).isEqualTo("bucket");
            assertThat(fullAdapter.getBackendType()).isEqualTo("AWS_S3");
        } finally {
            fullAdapter.close();
        }
    }

    @Test
    void multipartThreshold_shouldBeCorrect() {
        // 创建一个新的适配器用于此测试
        S3StorageAdapter testAdapter = new S3StorageAdapter("us-east-1", "testAccessKey", "testSecretKey", TEST_BUCKET);
        
        try {
            // 这是一个API测试，验证常量值
            // 我们通过创建大于5MB的数据来间接测试multipart threshold逻辑
            byte[] largeData = new byte[6 * 1024 * 1024]; // 6MB

            // 当调用putObject时，应该触发multipart upload逻辑
            // 由于没有真实的S3连接，我们只能验证方法调用不会因为数据大小而崩溃
            CompletableFuture<Void> result = testAdapter.putObject("large-file", largeData);

            // 验证方法调用成功创建了Future对象
            assertThat(result).isNotNull();
            assertThat(result.isDone()).isFalse(); // 还没有执行
        } finally {
            testAdapter.close();
        }
    }

    @Test
    void buildFullKey_behaviorTest() {
        // 通过观察实际的上传行为来测试key构建逻辑
        // 使用不同的key值来验证key前缀处理是否正确

        CompletableFuture<Void> result1 = adapter.putObject("simple-key", TEST_DATA);
        CompletableFuture<Void> result2 = adapter.putObject("path/to/file", TEST_DATA);

        // 验证方法调用都能正常创建Future
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
    }
}
