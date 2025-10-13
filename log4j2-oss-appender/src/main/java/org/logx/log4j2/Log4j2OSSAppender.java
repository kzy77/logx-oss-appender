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
import org.logx.core.AsyncEngineConfig;

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
            @PluginAttribute(value = "maxQueueSize", defaultInt = 65536) final int maxQueueSize,
            @PluginAttribute(value = "maxBatchCount", defaultInt = 4096) final int maxBatchCount,
            @PluginAttribute(value = "maxBatchBytes", defaultInt = 10485760) final int maxBatchBytes,
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

        // 使用ConfigManager实现完整配置优先级：
        // JVM系统属性 > 环境变量 > 配置文件 > XML配置 > 默认值
        org.logx.config.ConfigManager configManager = new org.logx.config.ConfigManager();

        // 解析所有配置，应用完整的优先级链
        String finalEndpoint = resolveStringConfig(configManager, "logx.oss.endpoint", endpoint);
        String finalRegion = resolveStringConfig(configManager, "logx.oss.region", region);
        String finalAccessKeyId = resolveStringConfig(configManager, "logx.oss.accessKeyId", accessKeyId);
        String finalAccessKeySecret = resolveStringConfig(configManager, "logx.oss.accessKeySecret", accessKeySecret);
        String finalBucket = resolveStringConfig(configManager, "logx.oss.bucket", bucket);
        String finalKeyPrefix = resolveStringConfig(configManager, "logx.oss.keyPrefix", keyPrefix);
        if (finalKeyPrefix == null || finalKeyPrefix.isEmpty()) {
            finalKeyPrefix = org.logx.config.CommonConfig.Defaults.KEY_PREFIX;
        }
        String finalOssType = resolveStringConfig(configManager, "logx.oss.ossType", ossType);
        if (finalOssType == null || finalOssType.isEmpty()) {
            finalOssType = org.logx.config.CommonConfig.Defaults.OSS_TYPE;
        }

        int finalMaxQueueSize = resolveIntConfig(configManager, "logx.oss.queueCapacity", maxQueueSize);
        int finalMaxBatchCount = resolveIntConfig(configManager, "logx.oss.maxBatchCount", maxBatchCount);
        int finalMaxBatchBytes = resolveIntConfig(configManager, "logx.oss.maxBatchBytes", maxBatchBytes);
        long finalMaxMessageAgeMs = resolveLongConfig(configManager, "logx.oss.maxMessageAgeMs", maxMessageAgeMs);
        boolean finalDropWhenQueueFull = resolveBooleanConfig(configManager, "logx.oss.dropWhenQueueFull", dropWhenQueueFull);
        boolean finalMultiProducer = resolveBooleanConfig(configManager, "logx.oss.multiProducer", multiProducer);
        int finalMaxRetries = resolveIntConfig(configManager, "logx.oss.maxRetries", maxRetries);
        long finalBaseBackoffMs = resolveLongConfig(configManager, "logx.oss.baseBackoffMs", baseBackoffMs);
        long finalMaxBackoffMs = resolveLongConfig(configManager, "logx.oss.maxBackoffMs", maxBackoffMs);

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

        // 构建异步引擎配置
        AsyncEngineConfig engineConfig = AsyncEngineConfig.defaultConfig()
            .queueCapacity(finalMaxQueueSize)
            .batchMaxMessages(finalMaxBatchCount)
            .batchMaxBytes(finalMaxBatchBytes)
            .maxMessageAgeMs(finalMaxMessageAgeMs)
            .blockOnFull(finalDropWhenQueueFull) // Note: inverted logic (dropWhenQueueFull vs blockOnFull)
            .multiProducer(finalMultiProducer)
            .logFilePrefix(finalKeyPrefix)
            .logFileName("applogx") // Default log file name
            .fallbackRetentionDays(org.logx.config.CommonConfig.Defaults.FALLBACK_RETENTION_DAYS)
            .fallbackScanIntervalSeconds(org.logx.config.CommonConfig.Defaults.FALLBACK_SCAN_INTERVAL_SECONDS);

        try {
            adapterConfig.validateConfig();
        } catch (IllegalArgumentException e) {
            LOGGER.error("错误：配置验证失败: {}", e.getMessage());
            return null;
        }

        return new Log4j2OSSAppender(name, filter, layout, ignoreExceptions, null, adapterConfig, engineConfig);
    }

    /**
     * 解析字符串配置，应用完整的优先级链
     */
    private static String resolveStringConfig(org.logx.config.ConfigManager configManager, String configKey, String xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return xmlValue;
    }

    /**
     * 解析整数配置，应用完整的优先级链
     */
    private static int resolveIntConfig(org.logx.config.ConfigManager configManager, String configKey, int xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid integer value for {}: {}, using XML value: {}", configKey, value, xmlValue);
                return xmlValue;
            }
        }
        return xmlValue;
    }

    /**
     * 解析长整数配置，应用完整的优先级链
     */
    private static long resolveLongConfig(org.logx.config.ConfigManager configManager, String configKey, long xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid long value for {}: {}, using XML value: {}", configKey, value, xmlValue);
                return xmlValue;
            }
        }
        return xmlValue;
    }

    /**
     * 解析布尔配置，应用完整的优先级链
     */
    private static boolean resolveBooleanConfig(org.logx.config.ConfigManager configManager, String configKey, boolean xmlValue) {
        String value = configManager.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            String trimmedValue = value.trim().toLowerCase(java.util.Locale.ENGLISH);
            return "true".equals(trimmedValue) || "yes".equals(trimmedValue) || "1".equals(trimmedValue);
        }
        return xmlValue;
    }
}