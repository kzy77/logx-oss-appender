package org.logx.log4j2;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

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
import org.logx.storage.StorageConfig;

@Plugin(name = "OSS", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class Log4j2OSSAppender extends AbstractAppender {

    private final Log4j2Bridge adapter;

    private Log4j2OSSAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, final Property[] properties, final StorageConfig adapterConfig) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.adapter = new Log4j2Bridge(adapterConfig);
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
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {

        if (name == null) {
            LOGGER.error("错误：未提供Log4j2OSSAppender的名称");
            return null;
        }

        if (layout == null) {
            LOGGER.error("错误：未给Appender {} 设置Layout", name);
            return null;
        }

        // 使用ConfigManager支持环境变量覆盖（问题#6修复）
        org.logx.config.ConfigManager configManager = new org.logx.config.ConfigManager();

        // 配置优先级: XML配置 > 环境变量 > 默认值
        String finalOssType = ossType != null && !ossType.isEmpty() ? ossType
            : configManager.getProperty("logx.oss." + org.logx.config.CommonConfig.OSS_TYPE);
        if (finalOssType == null || finalOssType.isEmpty()) {
            finalOssType = org.logx.config.CommonConfig.Defaults.OSS_TYPE;
        }

        String finalEndpoint = endpoint != null && !endpoint.isEmpty() ? endpoint
            : configManager.getProperty("logx.oss." + org.logx.config.CommonConfig.ENDPOINT);

        String finalRegion = region != null && !region.isEmpty() ? region
            : configManager.getProperty("logx.oss." + org.logx.config.CommonConfig.REGION);

        String finalAccessKeyId = accessKeyId != null && !accessKeyId.isEmpty() ? accessKeyId
            : configManager.getProperty("logx.oss." + org.logx.config.CommonConfig.ACCESS_KEY_ID);

        String finalAccessKeySecret = accessKeySecret != null && !accessKeySecret.isEmpty() ? accessKeySecret
            : configManager.getProperty("logx.oss." + org.logx.config.CommonConfig.ACCESS_KEY_SECRET);

        String finalBucket = bucket != null && !bucket.isEmpty() ? bucket
            : configManager.getProperty("logx.oss." + org.logx.config.CommonConfig.BUCKET);

        // 构建存储配置
        StorageConfig adapterConfig = new StorageConfigBuilder()
            .ossType(finalOssType)
            .endpoint(finalEndpoint)
            .region(finalRegion)
            .accessKeyId(finalAccessKeyId)
            .accessKeySecret(finalAccessKeySecret)
            .bucket(finalBucket)
            .build();

        try {
            adapterConfig.validateConfig();
        } catch (IllegalArgumentException e) {
            LOGGER.error("错误：配置验证失败: {}", e.getMessage());
            return null;
        }

        return new Log4j2OSSAppender(name, filter, layout, ignoreExceptions, null, adapterConfig);
    }
}
