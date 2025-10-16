package org.logx.test;

import org.logx.core.AsyncEngineConfig;
import org.logx.core.AsyncEngineImpl;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.logx.storage.ProtocolType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class FallbackFormatTest {
    private static final Logger logger = LoggerFactory.getLogger(FallbackFormatTest.class);
    
    public static void main(String[] args) throws InterruptedException {
        logger.info("开始测试兜底文件格式化功能...");
        
        try {
            // 创建一个模拟的存储服务，模拟上传失败的情况
            AtomicInteger uploadCount = new AtomicInteger(0);
            
            // 创建AsyncEngineImpl实例
            AsyncEngineConfig config = AsyncEngineConfig.defaultConfig();
            config.setStorageConfig(new StorageConfig(new org.logx.config.properties.LogxOssProperties()));
            AsyncEngineImpl engine = new AsyncEngineImpl(config);
            engine.start();
            
            // 提交一些测试日志数据
            logger.info("提交测试日志数据...");
            for (int i = 0; i < 5; i++) {
                String logMessage = String.format("测试日志消息 #%d - %s%n", i, java.time.LocalDateTime.now());
                engine.put(logMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                Thread.sleep(100);
            }
            
            // 等待一段时间让处理完成
            logger.info("等待处理完成...");
            Thread.sleep(3000);
            
            // 关闭引擎
            engine.close();
            
            logger.info("测试完成。检查生成的兜底文件是否使用了正确的格式。");
            
        } catch (Exception e) {
            logger.error("测试过程中发生错误", e);
        }
    }
}