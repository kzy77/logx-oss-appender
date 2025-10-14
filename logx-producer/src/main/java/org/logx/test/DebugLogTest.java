package org.logx.test;

import org.logx.core.EnhancedDisruptorBatchingQueue;
import org.logx.core.EnhancedDisruptorBatchingQueue.Config;
import org.logx.storage.StorageService;
import org.logx.storage.ProtocolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class DebugLogTest {
    private static final Logger logger = LoggerFactory.getLogger(DebugLogTest.class);
    
    public static void main(String[] args) throws InterruptedException {
        logger.info("开始测试debug日志功能...");
        
        // 创建一个模拟的存储服务
        StorageService mockStorageService = new StorageService() {
            @Override
            public CompletableFuture<Void> putObject(String key, byte[] data) {
                // 模拟上传延迟
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public ProtocolType getProtocolType() {
                return ProtocolType.S3;
            }

            @Override
            public String getBucketName() {
                return "mock-bucket";
            }

            @Override
            public void close() {
            }

            @Override
            public boolean supportsProtocol(ProtocolType protocol) {
                return protocol == ProtocolType.S3;
            }
        };
        
        // 创建配置，使用较小的批处理大小和较短的消息年龄时间以便测试
        Config config = Config.defaultConfig()
            .queueCapacity(1024)
            .batchMaxMessages(3)
            .batchMaxBytes(1024 * 1024)
            .maxMessageAgeMs(2000); // 2秒
        
        logger.info("创建EnhancedDisruptorBatchingQueue以测试debug日志...");
        logger.info("配置参数: batchMaxMessages={}, maxMessageAgeMs={}", 
                   config.getBatchMaxMessages(), config.getMaxMessageAgeMs());
        
        // 创建批处理队列
        EnhancedDisruptorBatchingQueue queue = new EnhancedDisruptorBatchingQueue(config, 
            (batchData, originalSize, compressed, messageCount) -> {
                logger.info("[批次处理器] 处理批次: 消息数量={}", messageCount);
                try {
                    mockStorageService.putObject("test-batch-" + System.currentTimeMillis() + ".log", batchData).get();
                    return true;
                } catch (Exception e) {
                    logger.error("处理批次时发生错误", e);
                    return false;
                }
            }, 
            mockStorageService);
        
        // 启动队列
        queue.start();
        
        // 提交一些测试消息以触发不同的条件
        logger.info("提交测试消息以触发批处理条件...");
        for (int i = 0; i < 5; i++) {
            String message = "测试消息 #" + i + " - " + System.currentTimeMillis() + "\n";
            boolean success = queue.submit(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            logger.info("提交消息 #{} {}", i, (success ? "[成功]" : "[失败]"));
            Thread.sleep(100);
        }
        
        // 等待消息年龄触发
        logger.info("等待消息年龄条件触发...");
        Thread.sleep(3000);
        
        // 关闭队列
        queue.close();
        
        logger.info("测试完成。你应该能在控制台看到详细的debug日志。");
    }
}