package org.logx.config;

import org.logx.config.properties.LogxOssProperties;

public class LogxOssConfigResolver {

    private final ConfigManager configManager;

    public LogxOssConfigResolver(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public LogxOssProperties resolve() {
        LogxOssProperties properties = new LogxOssProperties();
        resolveStorage(properties.getStorage());
        resolveBatch(properties.getBatch());
        resolveRetry(properties.getRetry());
        resolveQueue(properties.getQueue());
        return properties;
    }

    private String resolve(String value) {
        return configManager.resolvePlaceholders(value);
    }

    private void resolveStorage(LogxOssProperties.Storage storage) {
        storage.setOssType(resolve(configManager.getProperty("logx.oss.storage.ossType", storage.getOssType())));
        storage.setEndpoint(resolve(configManager.getProperty("logx.oss.storage.endpoint")));
        storage.setRegion(resolve(configManager.getProperty("logx.oss.storage.region")));
        storage.setAccessKeyId(resolve(configManager.getProperty("logx.oss.storage.accessKeyId")));
        storage.setAccessKeySecret(resolve(configManager.getProperty("logx.oss.storage.accessKeySecret")));
        storage.setBucket(resolve(configManager.getProperty("logx.oss.storage.bucket")));
        storage.setKeyPrefix(resolve(configManager.getProperty("logx.oss.storage.keyPrefix", storage.getKeyPrefix())));
        storage.setPathStyleAccess(configManager.getBooleanProperty("logx.oss.storage.pathStyleAccess", storage.isPathStyleAccess()));
        // Default values
        if(storage.getOssType() == null) {
            storage.setOssType("sf_s3");
        }
        // Personalized default values
        if ("minio".equalsIgnoreCase(storage.getOssType()) || "sf_s3".equalsIgnoreCase(storage.getOssType())) {
            storage.setPathStyleAccess(true);
        } else if ("aws_s3".equalsIgnoreCase(storage.getOssType())) {
            if (storage.getEndpoint() == null) {
                storage.setEndpoint("s3.amazonaws.com");
            }
        }
    }

    private void resolveBatch(LogxOssProperties.Batch batch) {
        batch.setCount(configManager.getIntProperty("logx.oss.batch.count", batch.getCount()));
        batch.setBytes(configManager.getIntProperty("logx.oss.batch.bytes", batch.getBytes()));
        batch.setMaxAgeMs(configManager.getLongProperty("logx.oss.batch.maxAgeMs", batch.getMaxAgeMs()));
    }

    private void resolveRetry(LogxOssProperties.Retry retry) {
        retry.setMaxRetries(configManager.getIntProperty("logx.oss.retry.maxRetries", retry.getMaxRetries()));
        retry.setBaseBackoffMs(configManager.getLongProperty("logx.oss.retry.baseBackoffMs", retry.getBaseBackoffMs()));
        retry.setMaxBackoffMs(configManager.getLongProperty("logx.oss.retry.maxBackoffMs", retry.getMaxBackoffMs()));
    }

    private void resolveQueue(LogxOssProperties.Queue queue) {
        queue.setCapacity(configManager.getIntProperty("logx.oss.queue.capacity", queue.getCapacity()));
        queue.setDropWhenFull(configManager.getBooleanProperty("logx.oss.queue.dropWhenFull", queue.isDropWhenFull()));
    }
}