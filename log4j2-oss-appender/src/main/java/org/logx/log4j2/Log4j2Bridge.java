package org.logx.log4j2;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.logx.adapter.AbstractUniversalAdapter;
import org.logx.storage.StorageConfig;
import org.logx.core.AsyncEngine;
import org.logx.storage.s3.S3StorageFactory;


import java.io.Serializable;

/**
 * Log4j2 桥接器
 * 实现通用适配器接口，处理Log4j2特定的逻辑
 */
public class Log4j2Bridge extends AbstractUniversalAdapter {
    private Layout<? extends Serializable> layout;
    
    public Log4j2Bridge(StorageConfig config) {
        // 创建S3存储适配器
        this.s3Storage = S3StorageFactory.createAdapter(config);
        
        // 创建异步引擎
        this.asyncEngine = AsyncEngine.create(this.s3Storage);
    }
    
    public void setLayout(Layout<? extends Serializable> layout) {
        this.layout = layout;
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
     * 将Log4j2事件转换为字符串
     */
    private String convertEvent(Object event) {
        if (!(event instanceof LogEvent)) {
            return null;
        }
        
        LogEvent logEvent = (LogEvent) event;
        
        // 使用Layout格式化日志
        if (layout != null) {
            byte[] bytes = layout.toByteArray(logEvent);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            // 默认格式
            return logEvent.getMessage().getFormattedMessage() + "\n";
        }
    }
}