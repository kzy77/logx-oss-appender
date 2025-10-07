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

/**
 * S3兼容对象存储 Log4j2 Appender
 * 支持AWS S3、阿里云OSS、腾讯云COS、MinIO、Cloudflare R2等所有S3兼容存储
 *
 * <p>批处理配置参数（maxBatchCount、maxQueueSize等）通过系统属性或环境变量传递给底层AsyncEngine：
 * - logx.oss.maxBatchCount: 批处理大小（默认：4096）
 * - logx.oss.maxQueueSize: 队列容量（默认：65536）
 * - logx.oss.maxMessageAgeMs: 最早消息年龄阈值（默认：600000ms即10分钟）
 * - logx.oss.dropWhenQueueFull: 队列满时是否丢弃（默认：false）
 * - logx.oss.maxRetries: 最大重试次数（默认：5）
 *
 * <p><b>注意</b>：由于Log4j2 @PluginAttribute注解限制，默认值必须硬编码在注解中。
 * 这些默认值来自{@link org.logx.config.CommonConfig.Defaults}，如需修改默认值，请同步更新两处：
 * <ul>
 *   <li>CommonConfig.Defaults中的常量定义</li>
 *   <li>本类createAppender方法中的@PluginAttribute默认值</li>
 * </ul>
 *
 * @see org.logx.config.CommonConfig.Defaults 统一的默认值定义
 */
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
            @PluginAttribute("keyPrefix") final String keyPrefix,
            @PluginAttribute(value = "maxQueueSize", defaultInt = 65536) final int maxQueueSize,
            @PluginAttribute(value = "maxBatchCount", defaultInt = 4096) final int maxBatchCount,
            @PluginAttribute(value = "maxBatchBytes", defaultInt = 4194304) final int maxBatchBytes,
            @PluginAttribute(value = "maxMessageAgeMs", defaultLong = 600000L) final long maxMessageAgeMs,
            @PluginAttribute(value = "dropWhenQueueFull", defaultBoolean = false) final boolean dropWhenQueueFull,
            @PluginAttribute(value = "multiProducer", defaultBoolean = false) final boolean multiProducer,
            @PluginAttribute(value = "maxRetries", defaultInt = 5) final int maxRetries,
            @PluginAttribute(value = "baseBackoffMs", defaultLong = 200L) final long baseBackoffMs,
            @PluginAttribute(value = "maxBackoffMs", defaultLong = 10000L) final long maxBackoffMs,
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

        String finalKeyPrefix = keyPrefix != null && !keyPrefix.isEmpty() ? keyPrefix : "logs/";

        // 构建存储配置
        StorageConfig adapterConfig = new StorageConfigBuilder()
            .ossType(finalOssType)
            .endpoint(finalEndpoint)
            .region(finalRegion)
            .accessKeyId(finalAccessKeyId)
            .accessKeySecret(finalAccessKeySecret)
            .bucket(finalBucket)
            .keyPrefix(finalKeyPrefix)
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
