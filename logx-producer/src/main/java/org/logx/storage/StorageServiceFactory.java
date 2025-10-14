package org.logx.storage;

import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.Iterator;

import org.logx.config.ConfigManager;
import org.logx.config.factory.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 存储服务工厂类
 * <p>
 * 提供创建和加载存储服务实例的工厂方法，支持基于OSS类型的动态加载。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class StorageServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(StorageServiceFactory.class);

    /**
     * 根据配置创建存储服务实例
     *
     * @param config 存储配置
     * @return StorageService 存储服务实例
     * @throws IllegalStateException 如果找不到合适的存储服务适配器
     */
    public static StorageService createStorageService(StorageConfig config) {
        // 首先尝试自动检测OSS类型
        StorageConfig detectedConfig = StorageConfig.detectBackendType(config);
        String ossType = detectedConfig.getOssType();

        // 使用Java SPI机制加载存储服务实现
        ServiceLoader<StorageService> loader = ServiceLoader.load(StorageService.class);
        Iterator<StorageService> iterator = loader.iterator();

        // 遍历所有可用的存储服务实现
        while (iterator.hasNext()) {
            try {
                StorageService service = iterator.next();
                if (service.supportsOssType(ossType)) {
                    // 尝试调用初始化方法
                    try {
                        // 使用反射调用initialize方法（如果存在）
                        Method initializeMethod = service.getClass().getMethod("initialize", StorageConfig.class);
                        initializeMethod.invoke(service, detectedConfig);
                    } catch (Exception e) {
                        // 如果没有initialize方法或调用失败，继续使用服务
                        // 这对于不需要初始化的服务是正常的
                    }
                    return service;
                }
            } catch (Exception e) {
                // 忽略单个服务加载失败，继续尝试其他服务
                logger.error("Failed to load storage service: {}", e.getMessage());
            }
        }

        // 如果没有找到支持的存储服务，抛出异常
        throw new IllegalStateException("No storage service found for OSS type: " + ossType + 
            ". Please ensure the appropriate adapter module is in the classpath.");
    }

    /**
     * 根据OSS类型和配置创建存储服务实例
     *
     * @param ossType OSS类型
     * @param config 存储配置
     * @return StorageService 存储服务实例
     * @throws IllegalStateException 如果找不到合适的存储服务适配器
     */
    public static StorageService createStorageService(String ossType, StorageConfig config) {
        // 创建一个新的配置对象，设置OSS类型
        // 创建一个具体的Builder实现来构建更新后的配置
        class ConfigBuilder extends StorageConfig.Builder<ConfigBuilder> {
            @Override
            protected ConfigBuilder self() {
                return this;
            }

            @Override
            public StorageConfig build() {
                return new StorageConfig(this);
            }
        }

        ConfigBuilder builder = new ConfigBuilder();

        StorageConfig newConfig = builder
            .ossType(ossType)
            .endpoint(config.getEndpoint())
            .region(config.getRegion())
            .accessKeyId(config.getAccessKeyId())
            .accessKeySecret(config.getAccessKeySecret())
            .bucket(config.getBucket())
            .keyPrefix(config.getKeyPrefix())
            .pathStyleAccess(config.isPathStyleAccess())
            .connectTimeout(config.getConnectTimeout())
            .readTimeout(config.getReadTimeout())
            .maxConnections(config.getMaxConnections())
            .enableSsl(config.isEnableSsl())
            .build();

        return createStorageService(newConfig);
    }
}