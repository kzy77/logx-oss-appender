package org.logx.core;

import org.logx.config.ConfigManager;

/**
 * 异步引擎配置构建器
 * <p>
 * 从系统属性、环境变量或配置文件中读取配置参数，构建AsyncEngineConfig实例。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class AsyncEngineConfigBuilder {
    
    private static final String CONFIG_PREFIX = "logx.oss.async.";
    
    /**
     * 从配置管理器构建AsyncEngineConfig
     *
     * @return AsyncEngineConfig实例
     */
    public static AsyncEngineConfig buildConfig() {
        return buildConfig(new ConfigManager());
    }
    
    /**
     * 从指定的配置管理器构建AsyncEngineConfig
     *
     * @param configManager 配置管理器
     * @return AsyncEngineConfig实例
     */
    public static AsyncEngineConfig buildConfig(ConfigManager configManager) {
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig();
        
        // 从配置管理器读取配置
        config.queueCapacity(configManager.getIntProperty(CONFIG_PREFIX + "queue.capacity", config.getQueueCapacity()));
        config.batchMaxMessages(configManager.getIntProperty(CONFIG_PREFIX + "batch.max.messages", config.getBatchMaxMessages()));
        config.batchMaxBytes(configManager.getIntProperty(CONFIG_PREFIX + "batch.max.bytes", config.getBatchMaxBytes()));
        config.flushIntervalMs(configManager.getLongProperty(CONFIG_PREFIX + "flush.interval.ms", config.getFlushIntervalMs()));
        config.blockOnFull(configManager.getBooleanProperty(CONFIG_PREFIX + "block.on.full", config.isBlockOnFull()));
        config.multiProducer(configManager.getBooleanProperty(CONFIG_PREFIX + "multi.producer", config.isMultiProducer()));
        config.corePoolSize(configManager.getIntProperty(CONFIG_PREFIX + "threadpool.core.size", config.getCorePoolSize()));
        config.maximumPoolSize(configManager.getIntProperty(CONFIG_PREFIX + "threadpool.max.size", config.getMaximumPoolSize()));
        config.queueCapacityThreadPool(configManager.getIntProperty(CONFIG_PREFIX + "threadpool.queue.capacity", config.getQueueCapacityThreadPool()));
        config.enableCpuYield(configManager.getBooleanProperty(CONFIG_PREFIX + "cpu.yield.enable", config.isEnableCpuYield()));
        config.enableMemoryProtection(configManager.getBooleanProperty(CONFIG_PREFIX + "memory.protection.enable", config.isEnableMemoryProtection()));
        config.maxShutdownWaitMs(configManager.getLongProperty(CONFIG_PREFIX + "shutdown.wait.ms", config.getMaxShutdownWaitMs()));
        config.logFilePrefix(configManager.getProperty(CONFIG_PREFIX + "log.file.prefix", config.getLogFilePrefix()));
        
        return config;
    }
}