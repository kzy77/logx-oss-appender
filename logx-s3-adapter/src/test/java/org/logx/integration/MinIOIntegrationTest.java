package org.logx.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logx.storage.StorageConfig;
import org.logx.storage.s3.S3StorageServiceAdapter;
import org.logx.storage.s3.StorageConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * MinIO集成测试
 *
 * 验证S3StorageServiceAdapter能够正确连接MinIO并上传文件
 *
 * 前置条件：
 * 1. MinIO服务已启动：cd compatibility-tests/minio && ./start-minio-local.sh
 * 2. 测试bucket已创建：logx-test-bucket
 *
 * 测试配置从minio-test.properties加载，支持环境变量覆盖
 */
class MinIOIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(MinIOIntegrationTest.class);

    private S3StorageServiceAdapter storageService;

    @BeforeEach
    void setUp() throws Exception {
        // 加载MinIO配置文件
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("minio-test.properties")) {
            if (is == null) {
                fail("未找到minio-test.properties配置文件，请确保文件存在于src/test/resources目录");
            }
            properties.load(is);
        }

        logger.info("加载MinIO配置完成");

        // 从properties读取配置
        String endpoint = getProperty(properties, "logx.oss.endpoint", "http://localhost:9000");
        String region = getProperty(properties, "logx.oss.region", "US");
        String accessKeyId = getProperty(properties, "logx.oss.accessKeyId", "minioadmin");
        String accessKeySecret = getProperty(properties, "logx.oss.accessKeySecret", "minioadmin");
        String bucket = getProperty(properties, "logx.oss.bucket", "logx-test-bucket");
        String keyPrefix = getProperty(properties, "logx.oss.keyPrefix", "integration-test/");

        // 打印配置信息（用于调试）
        logger.info("MinIO配置:");
        logger.info("  endpoint: {}", endpoint);
        logger.info("  region: {}", region);
        logger.info("  bucket: {}", bucket);
        logger.info("  keyPrefix: {}", keyPrefix);

        // 直接构建StorageConfig
        StorageConfig config = new StorageConfigBuilder()
                .ossType("S3")
                .endpoint(endpoint)
                .region(region)
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .bucket(bucket)
                .keyPrefix(keyPrefix)
                .pathStyleAccess(true)  // MinIO使用路径风格
                .enableSsl(false)       // MinIO本地开发不用SSL
                .build();

        // 创建S3存储服务
        storageService = new S3StorageServiceAdapter();
        storageService.initialize(config);

        logger.info("MinIO存储服务初始化成功");
    }

    private String getProperty(Properties properties, String key, String defaultValue) {
        // 优先从环境变量读取
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // 其次从properties读取
        String propValue = properties.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            return propValue;
        }

        // 最后使用默认值
        return defaultValue;
    }

    @AfterEach
    void tearDown() {
        if (storageService != null) {
            storageService.close();
        }
    }

    @Test
    void shouldUploadLogsToMinIO() throws Exception {
        // Given - 准备测试数据
        String testContent = "MinIO Integration Test - " + System.currentTimeMillis() + "\n"
                + "This is a test log message.\n"
                + "Verifying that logs can be uploaded to MinIO successfully.\n";
        byte[] data = testContent.getBytes(StandardCharsets.UTF_8);
        String objectKey = "test-logs/integration-test-" + System.currentTimeMillis() + ".log";

        logger.info("准备上传测试文件: {}, 大小: {} bytes", objectKey, data.length);

        // When - 上传到MinIO
        CompletableFuture<Void> uploadFuture = storageService.putObject(objectKey, data);

        // Then - 验证上传成功
        assertThatCode(() -> uploadFuture.get(10, TimeUnit.SECONDS))
                .doesNotThrowAnyException();

        logger.info("✅ 文件上传成功: {}", objectKey);
        logger.info("请在MinIO控制台验证: http://localhost:9001");
        logger.info("Bucket: {}", storageService.getBucketName());
    }

    @Test
    void shouldUploadMultipleFiles() throws Exception {
        // Given - 准备多个测试文件
        int fileCount = 5;

        logger.info("准备上传{}个测试文件到MinIO", fileCount);

        // When - 上传多个文件
        for (int i = 0; i < fileCount; i++) {
            String testContent = String.format("Test file #%d - %d\n", i, System.currentTimeMillis());
            byte[] data = testContent.getBytes(StandardCharsets.UTF_8);
            String objectKey = String.format("test-logs/multi-test-%d-%d.log", i, System.currentTimeMillis());

            CompletableFuture<Void> uploadFuture = storageService.putObject(objectKey, data);
            uploadFuture.get(10, TimeUnit.SECONDS);

            logger.info("  ✅ 文件{}上传成功: {}", i + 1, objectKey);
        }

        // Then - 验证所有文件上传成功
        logger.info("✅ 所有{}个文件上传成功", fileCount);
        logger.info("请在MinIO控制台验证: http://localhost:9001");
    }

    @Test
    void shouldHandleLargeFile() throws Exception {
        // Given - 准备大文件（100KB）
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append(String.format("Line %04d: This is a test log message with some content to make it larger.\n", i));
        }

        byte[] data = content.toString().getBytes(StandardCharsets.UTF_8);
        String objectKey = "test-logs/large-file-" + System.currentTimeMillis() + ".log";

        logger.info("准备上传大文件: {}, 大小: {} bytes ({} KB)",
                objectKey, data.length, data.length / 1024);

        // When - 上传大文件
        CompletableFuture<Void> uploadFuture = storageService.putObject(objectKey, data);

        // Then - 验证上传成功
        assertThatCode(() -> uploadFuture.get(30, TimeUnit.SECONDS))
                .doesNotThrowAnyException();

        logger.info("✅ 大文件上传成功: {}", objectKey);
    }
}
