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

        // 应用XML参数到properties（XML属性具有最高优先级，覆盖环境变量和配置文件）

        // 存储配置（对应 logx.oss.storage.* 配置键）
        if (endpoint != null) {
            String value = configManager.resolvePlaceholders(endpoint);
            properties.getStorage().setEndpoint(value);
        }
        if (region != null) {
            String value = configManager.resolvePlaceholders(region);
            properties.getStorage().setRegion(value);
        }
        if (accessKeyId != null) {
            String value = configManager.resolvePlaceholders(accessKeyId);
            properties.getStorage().setAccessKeyId(value);
        }
        if (accessKeySecret != null) {
            String value = configManager.resolvePlaceholders(accessKeySecret);
            properties.getStorage().setAccessKeySecret(value);
        }
        if (bucket != null) {
            String value = configManager.resolvePlaceholders(bucket);
            properties.getStorage().setBucket(value);
        }
        if (ossType != null) {
            String value = configManager.resolvePlaceholders(ossType);
            properties.getStorage().setOssType(value);
        }
        if (keyPrefix != null) {
            String value = configManager.resolvePlaceholders(keyPrefix);
            properties.getStorage().setKeyPrefix(value);
        }
        if (pathStyleAccess != null) {
            String value = configManager.resolvePlaceholders(pathStyleAccess);
            properties.getStorage().setPathStyleAccess(Boolean.parseBoolean(value));
        }

        // 引擎配置 - 队列（对应 logx.oss.engine.queue.* 配置键）
        if (queueCapacity != null) {
            try {
                String value = configManager.resolvePlaceholders(queueCapacity);
                properties.getEngine().getQueue().setCapacity(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid queueCapacity value: {}", queueCapacity, e);
            }
        }
        if (dropWhenQueueFull != null) {
            String value = configManager.resolvePlaceholders(dropWhenQueueFull);
            properties.getEngine().getQueue().setDropWhenFull(Boolean.parseBoolean(value));
        }

        // 引擎配置 - 批处理（对应 logx.oss.engine.batch.* 配置键）
        if (maxBatchCount != null) {
            try {
                String value = configManager.resolvePlaceholders(maxBatchCount);
                properties.getEngine().getBatch().setCount(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid maxBatchCount value: {}", maxBatchCount, e);
            }
        }
        if (maxBatchBytes != null) {
            try {
                String value = configManager.resolvePlaceholders(maxBatchBytes);
                properties.getEngine().getBatch().setBytes(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid maxBatchBytes value: {}", maxBatchBytes, e);
            }
        }
        if (maxMessageAgeMs != null) {
            try {
                String value = configManager.resolvePlaceholders(maxMessageAgeMs);
                properties.getEngine().getBatch().setMaxAgeMs(Long.parseLong(value));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid maxMessageAgeMs value: {}", maxMessageAgeMs, e);
            }
        }

        // 引擎配置 - 重试（对应 logx.oss.engine.retry.* 配置键）
        if (maxRetries != null) {
            try {
                String value = configManager.resolvePlaceholders(maxRetries);
                properties.getEngine().getRetry().setMaxRetries(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid maxRetries value: {}", maxRetries, e);
            }
        }
        if (baseBackoffMs != null) {
            try {
                String value = configManager.resolvePlaceholders(baseBackoffMs);
                properties.getEngine().getRetry().setBaseBackoffMs(Long.parseLong(value));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid baseBackoffMs value: {}", baseBackoffMs, e);
            }
        }
        if (maxBackoffMs != null) {
            try {
                String value = configManager.resolvePlaceholders(maxBackoffMs);
                properties.getEngine().getRetry().setMaxBackoffMs(Long.parseLong(value));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid maxBackoffMs value: {}", maxBackoffMs, e);
            }
        }

        StorageConfig storageConfig = new StorageConfig(properties);

        AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig();
        engineConfig.queueCapacity(properties.getEngine().getQueue().getCapacity());
        engineConfig.batchMaxMessages(properties.getEngine().getBatch().getCount());
        engineConfig.batchMaxBytes(properties.getEngine().getBatch().getBytes());
        engineConfig.maxMessageAgeMs(properties.getEngine().getBatch().getMaxAgeMs());
        engineConfig.blockOnFull(!properties.getEngine().getQueue().isDropWhenFull());

        return new Log4j2OSSAppender(name, filter, layout, ignoreExceptions, null, storageConfig, engineConfig);
    }
}