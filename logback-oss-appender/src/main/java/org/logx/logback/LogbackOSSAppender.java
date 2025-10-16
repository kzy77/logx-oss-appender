package org.logx.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;
import org.logx.core.AsyncEngineConfig;
import org.logx.storage.StorageConfig;

import java.util.HashMap;
import java.util.Map;

public final class LogbackOSSAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;
    private LogbackBridge adapter;

    // XML配置字段
    private final Map<String, String> xmlConfig = new HashMap<>();

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for the appender named \"" + name + "\"");
            return;
        }
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

            this.adapter = new LogbackBridge(storageConfig, engineConfig);
            this.adapter.setEncoder(encoder);
            this.adapter.start();

            super.start();
        } catch (Exception e) {
            addError("Failed to start LogbackOSSAppender", e);
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

        // 批处理配置
        if (xmlConfig.containsKey("logx.oss.batch.count")) {
            properties.getBatch().setCount(Integer.parseInt(xmlConfig.get("logx.oss.batch.count")));
        }
        if (xmlConfig.containsKey("logx.oss.batch.bytes")) {
            properties.getBatch().setBytes(Integer.parseInt(xmlConfig.get("logx.oss.batch.bytes")));
        }
        if (xmlConfig.containsKey("logx.oss.batch.maxAgeMs")) {
            properties.getBatch().setMaxAgeMs(Long.parseLong(xmlConfig.get("logx.oss.batch.maxAgeMs")));
        }

        // 队列配置
        if (xmlConfig.containsKey("logx.oss.queue.capacity")) {
            properties.getQueue().setCapacity(Integer.parseInt(xmlConfig.get("logx.oss.queue.capacity")));
        }
        if (xmlConfig.containsKey("logx.oss.queue.dropWhenFull")) {
            properties.getQueue().setDropWhenFull(Boolean.parseBoolean(xmlConfig.get("logx.oss.queue.dropWhenFull")));
        }

        // 重试配置
        if (xmlConfig.containsKey("logx.oss.retry.maxRetries")) {
            properties.getRetry().setMaxRetries(Integer.parseInt(xmlConfig.get("logx.oss.retry.maxRetries")));
        }
        if (xmlConfig.containsKey("logx.oss.retry.baseBackoffMs")) {
            properties.getRetry().setBaseBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.retry.baseBackoffMs")));
        }
        if (xmlConfig.containsKey("logx.oss.retry.maxBackoffMs")) {
            properties.getRetry().setMaxBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.retry.maxBackoffMs")));
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted() || adapter == null) {
            return;
        }
        try {
            adapter.append(eventObject);
        } catch (Exception e) {
            addError("Failed to encode and send log event", e);
        }
    }

    @Override
    public void stop() {
        if (adapter != null) {
            try {
                adapter.stop();
            } catch (Exception e) {
                addError("Failed to gracefully close adapter", e);
            }
        }
        super.stop();
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
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
        xmlConfig.put("logx.oss.queue.capacity", String.valueOf(queueCapacity));
    }

    public void setMaxBatchCount(int maxBatchCount) {
        xmlConfig.put("logx.oss.batch.count", String.valueOf(maxBatchCount));
    }

    public void setMaxBatchBytes(int maxBatchBytes) {
        xmlConfig.put("logx.oss.batch.bytes", String.valueOf(maxBatchBytes));
    }

    public void setMaxMessageAgeMs(long maxMessageAgeMs) {
        xmlConfig.put("logx.oss.batch.maxAgeMs", String.valueOf(maxMessageAgeMs));
    }

    public void setDropWhenQueueFull(boolean dropWhenQueueFull) {
        xmlConfig.put("logx.oss.queue.dropWhenFull", String.valueOf(dropWhenQueueFull));
    }

    public void setMaxRetries(int maxRetries) {
        xmlConfig.put("logx.oss.retry.maxRetries", String.valueOf(maxRetries));
    }

    public void setBaseBackoffMs(long baseBackoffMs) {
        xmlConfig.put("logx.oss.retry.baseBackoffMs", String.valueOf(baseBackoffMs));
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        xmlConfig.put("logx.oss.retry.maxBackoffMs", String.valueOf(maxBackoffMs));
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        xmlConfig.put("logx.oss.storage.pathStyleAccess", String.valueOf(pathStyleAccess));
    }
}