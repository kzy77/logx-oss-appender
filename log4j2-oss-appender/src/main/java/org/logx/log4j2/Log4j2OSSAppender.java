package org.logx.log4j2;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;
import org.logx.core.AsyncEngineConfig;
import org.logx.storage.StorageConfig;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Plugin(name = "OSS", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class Log4j2OSSAppender extends AbstractAppender {

    private final Log4j2Bridge adapter;

    private Log4j2OSSAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
                            final boolean ignoreExceptions, final Property[] properties, final StorageConfig adapterConfig,
                            final AsyncEngineConfig engineConfig) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.adapter = new Log4j2Bridge(adapterConfig, engineConfig);
        this.adapter.setLayout(layout);
    }

    @Override
    public void start() {
        super.start();
        if (adapter != null) {
            adapter.start();
        }
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

    @PluginFactory
    public static Log4j2OSSAppender createAppender(@PluginAttribute("name") final String name,
                                                  @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                  @PluginElement("Filter") final Filter filter,
                                                  @PluginAttribute("endpoint") final String endpoint,
                                                  @PluginAttribute("region") final String region,
                                                  @PluginAttribute("accessKeyId") final String accessKeyId,
                                                  @PluginAttribute("accessKeySecret") final String accessKeySecret,
                                                  @PluginAttribute("bucket") final String bucket,
                                                  @PluginAttribute("ossType") final String ossType,
                                                  @PluginAttribute("keyPrefix") final String keyPrefix,
                                                  @PluginAttribute("queueCapacity") final String queueCapacity,
                                                  @PluginAttribute("maxBatchCount") final String maxBatchCount,
                                                  @PluginAttribute("maxBatchBytes") final String maxBatchBytes,
                                                  @PluginAttribute("maxMessageAgeMs") final String maxMessageAgeMs,
                                                  @PluginAttribute("dropWhenQueueFull") final String dropWhenQueueFull,
                                                  @PluginAttribute("maxRetries") final String maxRetries,
                                                  @PluginAttribute("baseBackoffMs") final String baseBackoffMs,
                                                  @PluginAttribute("maxBackoffMs") final String maxBackoffMs,
                                                  @PluginAttribute("pathStyleAccess") final String pathStyleAccess,
                                                  @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {

        if (name == null) {
            LOGGER.error("No name provided for Log4j2OSSAppender");
            return null;
        }

        if (layout == null) {
            LOGGER.error("No layout provided for Log4j2OSSAppender '{}'", name);
            return null;
        }

        ConfigManager configManager = new ConfigManager();
        LogxOssProperties properties = configManager.getLogxOssProperties();

        // 应用XML参数到properties (优先级高于环境变量和配置文件)
        if (endpoint != null) {
            properties.getStorage().setEndpoint(endpoint);
        }
        if (region != null) {
            properties.getStorage().setRegion(region);
        }
        if (accessKeyId != null) {
            properties.getStorage().setAccessKeyId(accessKeyId);
        }
        if (accessKeySecret != null) {
            properties.getStorage().setAccessKeySecret(accessKeySecret);
        }
        if (bucket != null) {
            properties.getStorage().setBucket(bucket);
        }
        if (ossType != null) {
            properties.getStorage().setOssType(ossType);
        }
        if (keyPrefix != null) {
            properties.getStorage().setKeyPrefix(keyPrefix);
        }
        if (pathStyleAccess != null) {
            properties.getStorage().setPathStyleAccess(Boolean.parseBoolean(pathStyleAccess));
        }
        if (queueCapacity != null) {
            properties.getQueue().setCapacity(Integer.parseInt(queueCapacity));
        }
        if (maxBatchCount != null) {
            properties.getBatch().setCount(Integer.parseInt(maxBatchCount));
        }
        if (maxBatchBytes != null) {
            properties.getBatch().setBytes(Integer.parseInt(maxBatchBytes));
        }
        if (maxMessageAgeMs != null) {
            properties.getBatch().setMaxAgeMs(Long.parseLong(maxMessageAgeMs));
        }
        if (dropWhenQueueFull != null) {
            properties.getQueue().setDropWhenFull(Boolean.parseBoolean(dropWhenQueueFull));
        }
        if (maxRetries != null) {
            properties.getRetry().setMaxRetries(Integer.parseInt(maxRetries));
        }
        if (baseBackoffMs != null) {
            properties.getRetry().setBaseBackoffMs(Long.parseLong(baseBackoffMs));
        }
        if (maxBackoffMs != null) {
            properties.getRetry().setMaxBackoffMs(Long.parseLong(maxBackoffMs));
        }

        StorageConfig storageConfig = new StorageConfig(properties);

        AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig();
        engineConfig.queueCapacity(properties.getQueue().getCapacity());
        engineConfig.batchMaxMessages(properties.getBatch().getCount());
        engineConfig.batchMaxBytes(properties.getBatch().getBytes());
        engineConfig.maxMessageAgeMs(properties.getBatch().getMaxAgeMs());
        engineConfig.blockOnFull(!properties.getQueue().isDropWhenFull());

        return new Log4j2OSSAppender(name, filter, layout, ignoreExceptions, null, storageConfig, engineConfig);
    }
}