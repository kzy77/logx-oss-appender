package org.logx.adapter;

import org.logx.storage.s3.S3StorageInterface;
import org.logx.core.AsyncEngine;

/**
 * 抽象基类，提供通用适配器的基本实现
 * 处理核心组件的初始化
 */
public abstract class AbstractUniversalAdapter implements UniversalOSSAdapter {
    protected AsyncEngine asyncEngine;
    protected S3StorageInterface s3Storage;
    protected boolean started = false;
    
    @Override
    public void start() {
        if (started) {
            return;
        }
        
        try {
            if (asyncEngine != null) {
                asyncEngine.start();
            }
            
            started = true;
        } catch (Exception e) {
            throw new RuntimeException("Adapter start failed", e);
        }
    }
    
    @Override
    public void stop() {
        if (!started) {
            return;
        }
        
        try {
            if (asyncEngine != null) {
                asyncEngine.stop(5, java.util.concurrent.TimeUnit.SECONDS);
            }
            
            started = false;
        } catch (Exception e) {
            throw new RuntimeException("Adapter stop failed", e);
        }
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
}