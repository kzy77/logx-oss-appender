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

            // 预先解析xmlConfig中的所有占位符
            resolveXmlConfigPlaceholders();

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
        // 存储配置（占位符已在resolveXmlConfigPlaceholders中解析）
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
            try {
                properties.getEngine().getBatch().setCount(Integer.parseInt(xmlConfig.get("logx.oss.engine.batch.count")));
            } catch (NumberFormatException e) {
                addError("Invalid batch.count value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.bytes")) {
            try {
                properties.getEngine().getBatch().setBytes(Integer.parseInt(xmlConfig.get("logx.oss.engine.batch.bytes")));
            } catch (NumberFormatException e) {
                addError("Invalid batch.bytes value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.batch.maxAgeMs")) {
            try {
                properties.getEngine().getBatch().setMaxAgeMs(Long.parseLong(xmlConfig.get("logx.oss.engine.batch.maxAgeMs")));
            } catch (NumberFormatException e) {
                addError("Invalid batch.maxAgeMs value", e);
            }
        }

        // 引擎配置 - 队列
        if (xmlConfig.containsKey("logx.oss.engine.queue.capacity")) {
            try {
                properties.getEngine().getQueue().setCapacity(Integer.parseInt(xmlConfig.get("logx.oss.engine.queue.capacity")));
            } catch (NumberFormatException e) {
                addError("Invalid queue.capacity value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.queue.dropWhenFull")) {
            properties.getEngine().getQueue().setDropWhenFull(Boolean.parseBoolean(xmlConfig.get("logx.oss.engine.queue.dropWhenFull")));
        }

        // 引擎配置 - 重试
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxRetries")) {
            try {
                properties.getEngine().getRetry().setMaxRetries(Integer.parseInt(xmlConfig.get("logx.oss.engine.retry.maxRetries")));
            } catch (NumberFormatException e) {
                addError("Invalid retry.maxRetries value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.baseBackoffMs")) {
            try {
                properties.getEngine().getRetry().setBaseBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.engine.retry.baseBackoffMs")));
            } catch (NumberFormatException e) {
                addError("Invalid retry.baseBackoffMs value", e);
            }
        }
        if (xmlConfig.containsKey("logx.oss.engine.retry.maxBackoffMs")) {
            try {
                properties.getEngine().getRetry().setMaxBackoffMs(Long.parseLong(xmlConfig.get("logx.oss.engine.retry.maxBackoffMs")));
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

    /**
     * 预先解析xmlConfig中的所有占位符
     * 支持 ${ENV_VAR:-default} 和 ${ENV_VAR:default} 语法
     */
    private void resolveXmlConfigPlaceholders() {
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : xmlConfig.entrySet()) {
            String resolvedValue = configManager.resolvePlaceholders(entry.getValue());
            resolved.put(entry.getKey(), resolvedValue);
        }
        xmlConfig.clear();
        xmlConfig.putAll(resolved);
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