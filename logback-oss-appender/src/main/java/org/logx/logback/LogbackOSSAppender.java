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
    private ConfigManager configManager;

    // XML配置字段
    private final Map<String, String> xmlConfig = new HashMap<>();

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for the appender named \"" + name + "\"");
            return;
        }
        try {
            // 打印XML配置
            addInfo("=== XML Config Debug ===");
            for (Map.Entry<String, String> entry : xmlConfig.entrySet()) {
                addInfo(entry.getKey() + " = " + entry.getValue());
            }

            this.configManager = new ConfigManager();
            LogxOssProperties properties = configManager.getLogxOssProperties();

            // 应用XML配置（如果有的话）
            applyXmlConfig(properties);

            // 打印最终配置
            addInfo("=== Final Config Debug ===");
            addInfo("ossType: " + properties.getStorage().getOssType());
            addInfo("endpoint: " + properties.getStorage().getEndpoint());
            addInfo("region: " + properties.getStorage().getRegion());
            addInfo("accessKeyId: " + properties.getStorage().getAccessKeyId());
            addInfo("bucket: " + properties.getStorage().getBucket());

            StorageConfig storageConfig = new StorageConfig(properties);

            AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig();
            engineConfig.queueCapacity(properties.getEngine().getQueue().getCapacity());
            engineConfig.batchMaxMessages(properties.getEngine().getBatch().getCount());
            engineConfig.batchMaxBytes(properties.getEngine().getBatch().getBytes());
            engineConfig.maxMessageAgeMs(properties.getEngine().getBatch().getMaxAgeMs());
            engineConfig.blockOnFull(!properties.getEngine().getQueue().isDropWhenFull());

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
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.endpoint"));
            properties.getStorage().setEndpoint(value);
        }
        if (xmlConfig.containsKey("logx.oss.storage.region")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.region"));
            properties.getStorage().setRegion(value);
        }
        if (xmlConfig.containsKey("logx.oss.storage.accessKeyId")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.accessKeyId"));
            properties.getStorage().setAccessKeyId(value);
        }
        if (xmlConfig.containsKey("logx.oss.storage.accessKeySecret")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.accessKeySecret"));
            properties.getStorage().setAccessKeySecret(value);
        }
        if (xmlConfig.containsKey("logx.oss.storage.bucket")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.bucket"));
            properties.getStorage().setBucket(value);
        }
        if (xmlConfig.containsKey("logx.oss.storage.keyPrefix")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.keyPrefix"));
            properties.getStorage().setKeyPrefix(value);
        }
        if (xmlConfig.containsKey("logx.oss.storage.ossType")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.ossType"));
            properties.getStorage().setOssType(value);
        }
        if (xmlConfig.containsKey("logx.oss.storage.pathStyleAccess")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.storage.pathStyleAccess"));
            properties.getStorage().setPathStyleAccess(Boolean.parseBoolean(value));
        }

        // 引擎配置 - 批处理
        if (xmlConfig.containsKey("logx.oss.engine.batch.count")) {
            try {
                String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.batch.count"));
                properties.getEngine().getBatch().setCount(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                addError("Invalid batch.count value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.bytes")) {
            try {
                String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.batch.bytes"));
                properties.getEngine().getBatch().setBytes(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                addError("Invalid batch.bytes value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.maxAgeMs")) {
            try {
                String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.batch.maxAgeMs"));
                properties.getEngine().getBatch().setMaxAgeMs(Long.parseLong(value));
            } catch (NumberFormatException e) {
                addError("Invalid batch.maxAgeMs value", e);
            }
        }

        // 引擎配置 - 队列
        if (xmlConfig.containsKey("logx.oss.engine.queue.capacity")) {
            try {
                String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.queue.capacity"));
                properties.getEngine().getQueue().setCapacity(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                addError("Invalid queue.capacity value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.queue.dropWhenFull")) {
            String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.queue.dropWhenFull"));
            properties.getEngine().getQueue().setDropWhenFull(Boolean.parseBoolean(value));
        }

        // 引擎配置 - 重试
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxRetries")) {
            try {
                String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.retry.maxRetries"));
                properties.getEngine().getRetry().setMaxRetries(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                addError("Invalid retry.maxRetries value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.baseBackoffMs")) {
            try {
                String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.retry.baseBackoffMs"));
                properties.getEngine().getRetry().setBaseBackoffMs(Long.parseLong(value));
            } catch (NumberFormatException e) {
                addError("Invalid retry.baseBackoffMs value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxBackoffMs")) {
            try {
                String value = configManager.resolvePlaceholders(xmlConfig.get("logx.oss.engine.retry.maxBackoffMs"));
                properties.getEngine().getRetry().setMaxBackoffMs(Long.parseLong(value));
            } catch (NumberFormatException e) {
                addError("Invalid retry.maxBackoffMs value", e);
            }
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

    public void setQueueCapacity(String queueCapacity) {
        xmlConfig.put("logx.oss.engine.queue.capacity", queueCapacity);
    }

    public void setMaxBatchCount(String maxBatchCount) {
        xmlConfig.put("logx.oss.engine.batch.count", maxBatchCount);
    }

    public void setMaxBatchBytes(String maxBatchBytes) {
        xmlConfig.put("logx.oss.engine.batch.bytes", maxBatchBytes);
    }

    public void setMaxMessageAgeMs(String maxMessageAgeMs) {
        xmlConfig.put("logx.oss.engine.batch.maxAgeMs", maxMessageAgeMs);
    }

    public void setDropWhenQueueFull(String dropWhenQueueFull) {
        xmlConfig.put("logx.oss.engine.queue.dropWhenFull", dropWhenQueueFull);
    }

    public void setMaxRetries(String maxRetries) {
        xmlConfig.put("logx.oss.engine.retry.maxRetries", maxRetries);
    }

    public void setBaseBackoffMs(String baseBackoffMs) {
        xmlConfig.put("logx.oss.engine.retry.baseBackoffMs", baseBackoffMs);
    }

    public void setMaxBackoffMs(String maxBackoffMs) {
        xmlConfig.put("logx.oss.engine.retry.maxBackoffMs", maxBackoffMs);
    }

    public void setPathStyleAccess(String pathStyleAccess) {
        xmlConfig.put("logx.oss.storage.pathStyleAccess", pathStyleAccess);
    }
}