package org.logx.storage.sf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogxSfOssClient测试类
 * <p>
 * 测试配置从sf-oss-test.properties加载，支持环境变量覆盖
 */
public class LogxSfOssClientTest {

    private Properties properties;

    @BeforeEach
    void setUp() throws Exception {
        properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sf-oss-test.properties")) {
            if (is == null) {
                fail("未找到sf-oss-test.properties配置文件，请确保文件存在于src/test/resources目录");
            }
            properties.load(is);
        }
    }

    @Test
    public void testLogxSfOssClientCreation() {
        String endpoint = getProperty("logx.oss.endpoint", "https://sf-oss-cn-hangzhou.example.com");
        String region = getProperty("logx.oss.region", "cn-hangzhou");
        String accessKeyId = getProperty("logx.oss.accessKeyId", "test-access-key-id");
        String accessKeySecret = getProperty("logx.oss.accessKeySecret", "test-access-key-secret");

        LogxSfOssClient client = new LogxSfOssClient(
            endpoint,
            region,
            accessKeyId,
            accessKeySecret
        );

        assertNotNull(client);

        assertDoesNotThrow(() -> client.close());
    }

    @Test
    public void testLogxSfOssClientPutObject() {
        String endpoint = getProperty("logx.oss.endpoint", "https://sf-oss-cn-hangzhou.example.com");
        String region = getProperty("logx.oss.region", "cn-hangzhou");
        String accessKeyId = getProperty("logx.oss.accessKeyId", "test-access-key-id");
        String accessKeySecret = getProperty("logx.oss.accessKeySecret", "test-access-key-secret");
        String bucket = getProperty("logx.oss.bucket", "test-bucket");

        LogxSfOssClient client = new LogxSfOssClient(
            endpoint,
            region,
            accessKeyId,
            accessKeySecret
        );

        assertDoesNotThrow(() -> {
            client.putObject(bucket, "test-key", "test-data".getBytes());
        });

        assertDoesNotThrow(() -> client.close());
    }

    @Test
    public void testLogxSfOssClientWithNullEndpoint() {
        String region = getProperty("logx.oss.region", "cn-hangzhou");
        String accessKeyId = getProperty("logx.oss.accessKeyId", "test-access-key-id");
        String accessKeySecret = getProperty("logx.oss.accessKeySecret", "test-access-key-secret");
        String bucket = getProperty("logx.oss.bucket", "test-bucket");

        LogxSfOssClient client = new LogxSfOssClient(
            null,
            region,
            accessKeyId,
            accessKeySecret
        );

        assertNotNull(client);

        assertDoesNotThrow(() -> {
            client.putObject(bucket, "test-key", "test-data".getBytes());
        });

        assertDoesNotThrow(() -> client.close());
    }

    /**
     * 获取配置属性，支持环境变量覆盖
     */
    private String getProperty(String key, String defaultValue) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        String propValue = properties.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            return propValue;
        }

        return defaultValue;
    }
}