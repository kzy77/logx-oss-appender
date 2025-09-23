package org.logx.logback;

import org.logx.adapter.AbstractUniversalAdapter;
import org.logx.storage.StorageConfig;
import org.logx.core.AsyncEngine;
import org.logx.storage.s3.S3StorageFactory;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;


/**
 * Logback 桥接器
 * 实现通用适配器接口，处理Logback特定的逻辑
 */
public class LogbackBridge extends AbstractUniversalAdapter {
    private Encoder<ILoggingEvent> encoder;
    
    public LogbackBridge(StorageConfig config) {
        // 创建S3存储适配器
        this.s3Storage = S3StorageFactory.createAdapter(config);
        
        // 创建异步引擎
        this.asyncEngine = AsyncEngine.create(this.s3Storage);
    }
    
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
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