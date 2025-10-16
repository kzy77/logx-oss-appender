package org.logx.log4j2;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;
import org.logx.core.AsyncEngineConfig;
import org.logx.storage.StorageConfig;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Plugin(name = "OSS", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class Log4j2OSSAppender extends AbstractAppender {

    private Log4j2Bridge adapter;
    private final Map<String, String> xmlConfig = new HashMap<>();

    public Log4j2OSSAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, null);
    }

    @Override
    public void start() {
        if (getLayout() == null) {
            LOGGER.error("No layout set for the appender named \"{}\".", getName());
            return;
        }
        try {
            ConfigManager configManager = new ConfigManager();
            LogxOssProperties properties = configManager.getLogxOssProperties();

            ConfigManager.resolveMapPlaceholders(xmlConfig);

            applyXmlConfig(properties);

            StorageConfig storageConfig = new StorageConfig(properties);

            AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig();
            engineConfig.queueCapacity(properties.getEngine().getQueue().getCapacity());
            engineConfig.batchMaxMessages(properties.getEngine().getBatch().getCount());
            engineConfig.batchMaxBytes(properties.getEngine().getBatch().getBytes());
            engineConfig.maxMessageAgeMs(properties.getEngine().getBatch().getMaxAgeMs());
            engineConfig.blockOnFull(!properties.getEngine().getQueue().isDropWhenFull());

            this.adapter = new Log4j2Bridge(storageConfig, engineConfig);
            this.adapter.setLayout(getLayout());
            this.adapter.start();

            super.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start Log4j2OSSAppender", e);
        }
    }

    private void applyXmlConfig(LogxOssProperties properties) {
        // Storage Config
        xmlConfig.computeIfPresent("logx.oss.storage.endpoint", (k, v) -> { properties.getStorage().setEndpoint(v); return v; });
        xmlConfig.computeIfPresent("logx.oss.storage.region", (k, v) -> { properties.getStorage().setRegion(v); return v; });
        xmlConfig.computeIfPresent("logx.oss.storage.accessKeyId", (k, v) -> { properties.getStorage().setAccessKeyId(v); return v; });
        xmlConfig.computeIfPresent("logx.oss.storage.accessKeySecret", (k, v) -> { properties.getStorage().setAccessKeySecret(v); return v; });
        xmlConfig.computeIfPresent("logx.oss.storage.bucket", (k, v) -> { properties.getStorage().setBucket(v); return v; });
        xmlConfig.computeIfPresent("logx.oss.storage.keyPrefix", (k, v) -> { properties.getStorage().setKeyPrefix(v); return v; });
        xmlConfig.computeIfPresent("logx.oss.storage.ossType", (k, v) -> { properties.getStorage().setOssType(v); return v; });
        xmlConfig.computeIfPresent("logx.oss.storage.pathStyleAccess", (k, v) -> { properties.getStorage().setPathStyleAccess(Boolean.parseBoolean(v)); return v; });

        // Engine Batch Config
        xmlConfig.computeIfPresent("logx.oss.engine.batch.count", (k, v) -> { properties.getEngine().getBatch().setCount(Integer.parseInt(v)); return v; });
        xmlConfig.computeIfPresent("logx.oss.engine.batch.bytes", (k, v) -> { properties.getEngine().getBatch().setBytes(Integer.parseInt(v)); return v; });
        xmlConfig.computeIfPresent("logx.oss.engine.batch.maxAgeMs", (k, v) -> { properties.getEngine().getBatch().setMaxAgeMs(Long.parseLong(v)); return v; });

        // Engine Queue Config
        xmlConfig.computeIfPresent("logx.oss.engine.queue.capacity", (k, v) -> { properties.getEngine().getQueue().setCapacity(Integer.parseInt(v)); return v; });
        xmlConfig.computeIfPresent("logx.oss.engine.queue.dropWhenFull", (k, v) -> { properties.getEngine().getQueue().setDropWhenFull(Boolean.parseBoolean(v)); return v; });

        // Engine Retry Config
        xmlConfig.computeIfPresent("logx.oss.engine.retry.maxRetries", (k, v) -> { properties.getEngine().getRetry().setMaxRetries(Integer.parseInt(v)); return v; });
        xmlConfig.computeIfPresent("logx.oss.engine.retry.baseBackoffMs", (k, v) -> { properties.getEngine().getRetry().setBaseBackoffMs(Long.parseLong(v)); return v; });
        xmlConfig.computeIfPresent("logx.oss.engine.retry.maxBackoffMs", (k, v) -> { properties.getEngine().getRetry().setMaxBackoffMs(Long.parseLong(v)); return v; });
    }

    @Override
    public void append(LogEvent event) {
        if (!isStarted() || adapter == null) {
            return;
        }
        adapter.append(event);
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        if (adapter != null) {
            adapter.stop();
        }
        return super.stop(timeout, timeUnit);
    }

    // Setters for Log4j2 configuration
    public void setEndpoint(String endpoint) { xmlConfig.put("logx.oss.storage.endpoint", endpoint); }
    public void setRegion(String region) { xmlConfig.put("logx.oss.storage.region", region); }
    public void setAccessKeyId(String accessKeyId) { xmlConfig.put("logx.oss.storage.accessKeyId", accessKeyId); }
    public void setAccessKeySecret(String accessKeySecret) { xmlConfig.put("logx.oss.storage.accessKeySecret", accessKeySecret); }
    public void setBucket(String bucket) { xmlConfig.put("logx.oss.storage.bucket", bucket); }
    public void setKeyPrefix(String keyPrefix) { xmlConfig.put("logx.oss.storage.keyPrefix", keyPrefix); }
    public void setOssType(String ossType) { xmlConfig.put("logx.oss.storage.ossType", ossType); }
    public void setPathStyleAccess(String pathStyleAccess) { xmlConfig.put("logx.oss.storage.pathStyleAccess", pathStyleAccess); }
    public void setQueueCapacity(String queueCapacity) { xmlConfig.put("logx.oss.engine.queue.capacity", queueCapacity); }
    public void setMaxBatchCount(String maxBatchCount) { xmlConfig.put("logx.oss.engine.batch.count", maxBatchCount); }
    public void setMaxBatchBytes(String maxBatchBytes) { xmlConfig.put("logx.oss.engine.batch.bytes", maxBatchBytes); }
    public void setMaxMessageAgeMs(String maxMessageAgeMs) { xmlConfig.put("logx.oss.engine.batch.maxAgeMs", maxMessageAgeMs); }
    public void setDropWhenQueueFull(String dropWhenQueueFull) { xmlConfig.put("logx.oss.engine.queue.dropWhenFull", dropWhenQueueFull); }
    public void setMaxRetries(String maxRetries) { xmlConfig.put("logx.oss.engine.retry.maxRetries", maxRetries); }
    public void setBaseBackoffMs(String baseBackoffMs) { xmlConfig.put("logx.oss.engine.retry.baseBackoffMs", baseBackoffMs); }
    public void setMaxBackoffMs(String maxBackoffMs) { xmlConfig.put("logx.oss.engine.retry.maxBackoffMs", maxBackoffMs); }
}
