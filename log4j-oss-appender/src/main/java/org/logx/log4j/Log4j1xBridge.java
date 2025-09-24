package org.logx.log4j;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.logx.adapter.AbstractUniversalAdapter;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import org.logx.storage.StorageServiceFactory;
import org.logx.core.AsyncEngine;


/**
 * Log4j 1.x 桥接器
 * 实现通用适配器接口，处理Log4j 1.x特定的逻辑
 */
public class Log4j1xBridge extends AbstractUniversalAdapter {
    private Layout layout;
    
    public Log4j1xBridge(StorageConfig config) {
        // 使用存储服务工厂创建存储服务
        StorageService storageService = StorageServiceFactory.createStorageService(config);
        
        // 从存储服务获取存储接口
        this.s3Storage = storageService;
        
        // 创建异步引擎
        this.asyncEngine = AsyncEngine.create(storageService);
    }
    
    public void setLayout(Layout layout) {
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
     * 将Log4j 1.x事件转换为字符串
     */
    private String convertEvent(Object event) {
        if (!(event instanceof LoggingEvent)) {
            return null;
        }
        
        LoggingEvent loggingEvent = (LoggingEvent) event;
        
        // 使用Layout格式化日志
        if (layout != null) {
            StringBuilder sb = new StringBuilder(layout.format(loggingEvent));
            
            // 如果Layout要求异常信息且事件包含异常
            if (layout.ignoresThrowable() && loggingEvent.getThrowableInformation() != null) {
                String[] stackTrace = loggingEvent.getThrowableStrRep();
                if (stackTrace != null) {
                    for (String line : stackTrace) {
                        sb.append(line).append(Layout.LINE_SEP);
                    }
                }
            }
            
            return sb.toString();
        } else {
            // 默认格式
            return loggingEvent.getRenderedMessage() + Layout.LINE_SEP;
        }
    }
}