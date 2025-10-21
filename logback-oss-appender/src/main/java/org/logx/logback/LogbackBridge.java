package org.logx.logback;

import org.logx.adapter.AbstractUniversalAdapter;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import org.logx.storage.StorageServiceFactory;
import org.logx.core.AsyncEngine;
import org.logx.core.AsyncEngineConfig;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;


/**
 * Logback 桥接器
 * 实现通用适配器接口，处理Logback特定的逻辑
 */
public class LogbackBridge extends AbstractUniversalAdapter {
    private Encoder<ILoggingEvent> encoder;
    private AsyncEngineConfig engineConfig;
    
    public LogbackBridge(StorageConfig config) {
        this(config, null);
    }
    
    public LogbackBridge(StorageConfig config, AsyncEngineConfig engineConfig) {
        // 使用存储服务工厂创建存储服务
        StorageService storageService = StorageServiceFactory.createStorageService(config);

        // 从存储服务获取存储接口
        this.s3Storage = storageService;

        // 保存引擎配置
        this.engineConfig = engineConfig;

        // 设置StorageConfig到引擎配置
        if (engineConfig != null) {
            engineConfig.setStorageConfig(config);
            this.asyncEngine = AsyncEngine.create(engineConfig);
        } else {
            AsyncEngineConfig defaultConfig = AsyncEngineConfig.defaultConfig();
            defaultConfig.setStorageConfig(config);
            this.asyncEngine = AsyncEngine.create(defaultConfig);
        }
    }
    
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
    
    public void setEngineConfig(AsyncEngineConfig engineConfig) {
        this.engineConfig = engineConfig;
        // Note: In a real implementation, we would need to recreate the asyncEngine
        // when the configuration changes, but for now we're just storing the config
    }

    public AsyncEngineConfig getEngineConfig() {
        return engineConfig;
    }
    
    @Override
    public void append(Object event) {
        if (!isStarted() || asyncEngine == null) {
            return;
        }
        
        try {
            String logLine = convertEvent(event);
            if (logLine != null) {
                asyncEngine.put(logLine.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process log event", e);
        }
    }
    
    /**
     * 将Logback事件转换为字符串
     */
    private String convertEvent(Object event) {
        if (!(event instanceof ILoggingEvent)) {
            return null;
        }
        
        ILoggingEvent loggingEvent = (ILoggingEvent) event;
        
        // 使用Encoder编码日志
        if (encoder != null) {
            try {
                byte[] encoded = encoder.encode(loggingEvent);
                return new String(encoded, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                // 编码失败时返回默认格式
                return loggingEvent.getFormattedMessage() + "\n";
            }
        } else {
            // 默认格式
            return loggingEvent.getFormattedMessage() + "\n";
        }
    }
}