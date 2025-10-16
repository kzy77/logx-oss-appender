package org.logx.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;
import org.logx.core.AsyncEngineConfig;
import org.logx.storage.StorageConfig;

import java.util.HashMap;
import java.util.Map;

public class Log4jOSSAppender extends AppenderSkeleton {

    private Log4j1xBridge adapter;

    // XML配置字段
    private final Map<String, String> xmlConfig = new HashMap<>();

    @Override
    public void activateOptions() {
        super.activateOptions();

        try {
            ConfigManager configManager = new ConfigManager();
            LogxOssProperties properties = configManager.getLogxOssProperties();

            // 应用XML配置（如果有的话）
            applyXmlConfig(properties);

            StorageConfig storageConfig = new StorageConfig(properties);

            AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig();
            engineConfig.queueCapacity(properties.getQueue().getCapacity());
            engineConfig.batchMaxMessages(properties.getBatch().getCount());
            engineConfig.batchMaxBytes(properties.getBatch().getBytes());
            engineConfig.maxMessageAgeMs(properties.getBatch().getMaxAgeMs());
            engineConfig.blockOnFull(!properties.getQueue().isDropWhenFull());

            this.adapter = new Log4j1xBridge(storageConfig, engineConfig);
            this.adapter.setLayout(layout);
            this.adapter.start();

        } catch (Exception e) {
            getErrorHandler().error("Failed to initialize Log4jOSSAppender: " + e.getMessage(), e, 1);
        }
    }

    private void applyXmlConfig(LogxOssProperties properties) {
        // 存储配置
        if (xmlConfig.containsKey("logx.oss.storage.endpoint")) {
            properties.getStorage().setEndpoint(xmlConfig.get("logx.oss.storage.endpoint"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.region")) {
            properties.getStorage().setRegion(xmlConfig.get("logx.oss.storage.region"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.accessKeyId")) {
            properties.getStorage().setAccessKeyId(xmlConfig.get("logx.oss.storage.accessKeyId"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.accessKeySecret")) {
            properties.getStorage().setAccessKeySecret(xmlConfig.get("logx.oss.storage.accessKeySecret"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.bucket")) {
            properties.getStorage().setBucket(xmlConfig.get("logx.oss.storage.bucket"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.keyPrefix")) {
            properties.getStorage().setKeyPrefix(xmlConfig.get("logx.oss.storage.keyPrefix"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.ossType")) {
            properties.getStorage().setOssType(xmlConfig.get("logx.oss.storage.ossType"));
        }
        if (xmlConfig.containsKey("logx.oss.storage.pathStyleAccess")) {
            properties.getStorage().setPathStyleAccess(Boolean.parseBoolean(xmlConfig.get("logx.oss.storage.pathStyleAccess")));
        }

        // 引擎配置 - 批处理
        if (xmlConfig.containsKey("logx.oss.engine.batch.count")) {
            properties.getBatch().setCount(Integer.parseInt(xmlConfig.get("logx.oss.engine.batch.count")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.bytes")) {
            properties.getBatch().setBytes(Integer.parseInt(xmlConfig.get("logx.oss.engine.batch.bytes")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.maxAgeMs")) {
            properties.getBatch().setMaxAgeMs(Long.parseLong(xmlConfig.get("logx.oss.engine.batch.maxAgeMs")));
        }

        // 引擎配置 - 队列
        if (xmlConfig.containsKey("logx.oss.engine.queue.capacity")) {
            properties.getQueue().setCapacity(Integer.parseInt(xmlConfig.get("logx.oss.engine.queue.capacity")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.queue.dropWhenFull")) {
            properties.getQueue().setDropWhenFull(Boolean.parseBoolean(xmlConfig.get("logx.oss.engine.queue.dropWhenFull")));
        }

        // 引擎配置 - 重试
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxRetries")) {
            properties.getRetry().setMaxRetries(Integer.parseInt(xmlConfig.get("logx.oss.engine.retry.maxRetries")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.baseBackoffMs")) {
            properties.getRetry().setBaseBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.engine.retry.baseBackoffMs")));
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxBackoffMs")) {
            properties.getRetry().setMaxBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.engine.retry.maxBackoffMs")));
        }
    }

    @Override
    protected void append(LoggingEvent event) {
        if (!isAsSevereAsThreshold(event.getLevel()) || adapter == null) {
            return;
        }

        try {
            adapter.append(event);
        } catch (Exception e) {
            getErrorHandler().error("Failed to append log event", e, 1, event);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        try {
            if (adapter != null) {
                adapter.stop();
            }
        } catch (Exception e) {
            getErrorHandler().error("Failed to close OSSAppender", e, 1);
        }

        closed = true;
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    // Setter方法保存XML配置，使用新格式
    public void setEndpoint(String endpoint) {
        xmlConfig.put("logx.oss.storage.endpoint", endpoint);
    }

    public void setRegion(String region) {
        xmlConfig.put("logx.oss.storage.region", region);
    }

    public void setAccessKeyId(String accessKeyId) {
        xmlConfig.put("logx.oss.storage.accessKeyId", accessKeyId);
    }

    public void setAccessKeySecret(String accessKeySecret) {
        xmlConfig.put("logx.oss.storage.accessKeySecret", accessKeySecret);
    }

    public void setBucket(String bucket) {
        xmlConfig.put("logx.oss.storage.bucket", bucket);
    }

    public void setKeyPrefix(String keyPrefix) {
        xmlConfig.put("logx.oss.storage.keyPrefix", keyPrefix);
    }

    public void setOssType(String ossType) {
        xmlConfig.put("logx.oss.storage.ossType", ossType);
    }

    public void setQueueCapacity(int queueCapacity) {
        xmlConfig.put("logx.oss.engine.queue.capacity", String.valueOf(queueCapacity));
    }

    public void setMaxBatchCount(int maxBatchCount) {
        xmlConfig.put("logx.oss.engine.batch.count", String.valueOf(maxBatchCount));
    }

    public void setMaxBatchBytes(int maxBatchBytes) {
        xmlConfig.put("logx.oss.engine.batch.bytes", String.valueOf(maxBatchBytes));
    }

    public void setMaxMessageAgeMs(long maxMessageAgeMs) {
        xmlConfig.put("logx.oss.engine.batch.maxAgeMs", String.valueOf(maxMessageAgeMs));
    }

    public void setDropWhenQueueFull(boolean dropWhenQueueFull) {
        xmlConfig.put("logx.oss.engine.queue.dropWhenFull", String.valueOf(dropWhenQueueFull));
    }

    public void setMaxRetries(int maxRetries) {
        xmlConfig.put("logx.oss.engine.retry.maxRetries", String.valueOf(maxRetries));
    }

    public void setBaseBackoffMs(long baseBackoffMs) {
        xmlConfig.put("logx.oss.engine.retry.baseBackoffMs", String.valueOf(baseBackoffMs));
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        xmlConfig.put("logx.oss.engine.retry.maxBackoffMs", String.valueOf(maxBackoffMs));
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        xmlConfig.put("logx.oss.storage.pathStyleAccess", String.valueOf(pathStyleAccess));
    }
}
