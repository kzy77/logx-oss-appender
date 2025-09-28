package org.logx.storage.sf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LogxSfOssClient测试类
 */
public class LogxSfOssClientTest {

    @Test
    public void testLogxSfOssClientCreation() {
        // 测试创建SF OSS客户端
        LogxSfOssClient client = new LogxSfOssClient(
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
    public void testLogxSfOssClientPutObject() {
        // 测试SF OSS客户端上传对象
        LogxSfOssClient client = new LogxSfOssClient(
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
    public void testLogxSfOssClientWithNullEndpoint() {
        // 测试使用null endpoint创建SF OSS客户端
        LogxSfOssClient client = new LogxSfOssClient(
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