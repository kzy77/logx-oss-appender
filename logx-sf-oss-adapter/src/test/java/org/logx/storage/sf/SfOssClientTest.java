package org.logx.storage.sf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SfOssClient测试类
 */
public class SfOssClientTest {

    @Test
    public void testSfOssClientCreation() {
        // 测试创建SF OSS客户端
        SfOssClient client = new SfOssClient(
            "https://sf-oss-cn-hangzhou.example.com",
            "cn-hangzhou",
            "test-access-key-id",
            "test-access-key-secret"
        );
        
        assertNotNull(client);
        
        // 验证客户端可以正常关闭
        assertDoesNotThrow(() -> client.close());
    }

    @Test
    public void testSfOssClientPutObject() {
        // 测试SF OSS客户端上传对象
        SfOssClient client = new SfOssClient(
            "https://sf-oss-cn-hangzhou.example.com",
            "cn-hangzhou",
            "test-access-key-id",
            "test-access-key-secret"
        );
        
        assertDoesNotThrow(() -> {
            client.putObject("test-bucket", "test-key", "test-data".getBytes());
        });
        
        assertDoesNotThrow(() -> client.close());
    }

    @Test
    public void testSfOssClientWithNullEndpoint() {
        // 测试使用null endpoint创建SF OSS客户端
        SfOssClient client = new SfOssClient(
            null,
            "cn-hangzhou",
            "test-access-key-id",
            "test-access-key-secret"
        );
        
        assertNotNull(client);
        
        assertDoesNotThrow(() -> {
            client.putObject("test-bucket", "test-key", "test-data".getBytes());
        });
        
        assertDoesNotThrow(() -> client.close());
    }
}