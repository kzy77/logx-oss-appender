package org.logx.storage.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.logx.storage.StorageConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * S3StorageAdapter测试类
 */
public class S3StorageAdapterTest {

    private StorageConfig config;

    @BeforeEach
    public void setUp() {
        config = new StorageConfigBuilder()
            .ossType("S3")
            .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
            .region("cn-hangzhou")
            .accessKeyId("test-access-key-id")
            .accessKeySecret("test-access-key-secret")
            .bucket("test-bucket")
            .build();
    }

    @Test
    public void testS3StorageAdapterCreationWithEndpoint() {
        // 测试使用endpoint创建S3StorageAdapter
        assertDoesNotThrow(() -> {
            S3StorageAdapter adapter = new S3StorageAdapter(config);
            assertNotNull(adapter);
            assertEquals("S3", adapter.getOssType());
            assertEquals("test-bucket", adapter.getBucketName());
            adapter.close();
        });
    }

    @Test
    public void testS3StorageAdapterCreationWithoutEndpoint() {
        // 测试不使用endpoint创建S3StorageAdapter
        StorageConfig configWithoutEndpoint = new StorageConfigBuilder()
            .ossType("S3")
            .region("us-east-1")
            .accessKeyId("test-access-key-id")
            .accessKeySecret("test-access-key-secret")
            .bucket("test-bucket")
            .build();

        assertDoesNotThrow(() -> {
            S3StorageAdapter adapter = new S3StorageAdapter(configWithoutEndpoint);
            assertNotNull(adapter);
            assertEquals("S3", adapter.getOssType());
            assertEquals("test-bucket", adapter.getBucketName());
            adapter.close();
        });
    }

    @Test
    public void testS3StorageAdapterCreationWithInvalidEndpoint() {
        // 测试使用无效endpoint创建S3StorageAdapter
        StorageConfig configWithInvalidEndpoint = new StorageConfigBuilder()
            .ossType("S3")
            .endpoint("invalid-url")
            .region("cn-hangzhou")
            .accessKeyId("test-access-key-id")
            .accessKeySecret("test-access-key-secret")
            .bucket("test-bucket")
            .build();

        // 注意：AWS SDK可能会在实际使用时才验证URL，所以在构造时可能不会抛出异常
        assertDoesNotThrow(() -> {
            S3StorageAdapter adapter = new S3StorageAdapter(configWithInvalidEndpoint);
            assertNotNull(adapter);
            adapter.close();
        });
    }
}