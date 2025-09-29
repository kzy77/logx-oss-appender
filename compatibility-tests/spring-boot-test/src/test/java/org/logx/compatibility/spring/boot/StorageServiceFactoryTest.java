package org.logx.compatibility.spring.boot;

import org.junit.jupiter.api.Test;
import org.logx.storage.StorageService;
import org.logx.storage.StorageServiceFactory;
import org.logx.storage.StorageConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试存储服务工厂是否能正确创建S3存储服务
 */
public class StorageServiceFactoryTest {

    @Test
    public void testCreateS3StorageService() {
        // 创建存储配置
        StorageConfig config = new StorageConfig.Builder() {
            @Override
            protected StorageConfig.Builder self() {
                return this;
            }

            @Override
            public StorageConfig build() {
                // 使用反射调用protected构造函数
                try {
                    return StorageConfig.class.getDeclaredConstructor(StorageConfig.Builder.class)
                            .newInstance(this);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
            .ossType("S3")
            .endpoint("https://s3.amazonaws.com")
            .region("us-east-1")
            .accessKeyId("test-access-key")
            .accessKeySecret("test-secret-key")
            .bucket("test-bucket")
            .build();
        
        // 尝试创建存储服务
        try {
            StorageService storageService = StorageServiceFactory.createStorageService(config);
            assertNotNull(storageService);
            assertEquals("S3", storageService.getOssType());
            System.out.println("成功创建S3存储服务: " + storageService.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
            fail("创建存储服务失败: " + e.getMessage());
        }
    }
}