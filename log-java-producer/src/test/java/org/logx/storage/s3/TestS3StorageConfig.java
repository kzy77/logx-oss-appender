package org.logx.storage;

import org.logx.storage.StorageConfig;
import java.time.Duration;

/**
 * 测试用的S3存储配置类
 */
public class TestS3StorageConfig extends StorageConfig {
    private TestS3StorageConfig(Builder builder) {
        super(builder);
    }

    public static class Builder extends StorageConfig.Builder<Builder> {
        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public StorageConfig build() {
            return new TestS3StorageConfig(this);
        }
    }
}