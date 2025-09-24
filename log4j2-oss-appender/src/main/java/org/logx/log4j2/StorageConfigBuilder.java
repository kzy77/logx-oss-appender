package org.logx.log4j2;

import org.logx.storage.StorageConfig;

/**
 * StorageConfig的具体Builder实现
 * 用于解决跨包访问StorageConfig.Builder的protected构造函数问题
 */
public class StorageConfigBuilder extends StorageConfig.Builder<StorageConfigBuilder> {
    
    @Override
    protected StorageConfigBuilder self() {
        return this;
    }

    @Override
    public StorageConfig build() {
        // 使用反射创建StorageConfig实例，绕过protected构造函数限制
        try {
            java.lang.reflect.Constructor<StorageConfig> constructor = StorageConfig.class.getDeclaredConstructor(StorageConfig.Builder.class);
            constructor.setAccessible(true);
            return constructor.newInstance(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create StorageConfig instance", e);
        }
    }
}