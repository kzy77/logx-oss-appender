package org.logx.test;

import org.logx.core.EnhancedDisruptorBatchingQueue;
import org.logx.core.EnhancedDisruptorBatchingQueue.Config;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class SimpleQueueTest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleQueueTest.class);
    
    public static void main(String[] args) throws InterruptedException {
        logger.info("开始测试EnhancedDisruptorBatchingQueue功能...");
        
        // 创建一个模拟的存储服务
        StorageService mockStorageService = new StorageService() {
            @Override
            public CompletableFuture<Void> putObject(String key, byte[] data) {
                logger.info("模拟上传对象: {}, 数据大小: {} 字节", key, data.length);
                // 模拟上传延迟
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public String getOssType() {
                return "MOCK";
            }
            
            @Override
            public String getBucketName() {
                return "mock-bucket";
            }
            
            @Override
            public void close() {
                logger.info("关闭模拟存储服务");
            }
            
            @Override
            public boolean supportsOssType(String ossType) {
                return "MOCK".equals(ossType);
            }
        };
        
        // 创建配置
        Config config = Config.defaultConfig()
            .queueCapacity(1024)
            .batchMaxMessages(5)  // 设置较小的批处理大小以便测试
            .batchMaxBytes(1024 * 1024)
            .maxMessageAgeMs(5000); // 5秒
        
        logger.info("创建EnhancedDisruptorBatchingQueue，配置参数: batchMaxMessages={}, maxMessageAgeMs={}", 
                   config.getBatchMaxMessages(), config.getMaxMessageAgeMs());
        
        // 创建批处理队列
        EnhancedDisruptorBatchingQueue queue = new EnhancedDisruptorBatchingQueue(config, 
            (batchData, originalSize, compressed, messageCount) -> {
                logger.info("处理批次: 消息数量={}, 原始大小={} 字节", messageCount, originalSize);
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
        
        // 提交一些测试消息，验证批处理
        logger.info("提交测试消息以验证批处理...");
        for (int i = 0; i < 12; i++) {
            String message = "测试日志消息 #" + i + " - " + System.currentTimeMillis() + "\n";
            queue.submit(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            logger.info("提交消息 #{}", i);
        }
        
        // 等待一段时间让批处理完成
        logger.info("等待批处理完成...");
        Thread.sleep(2000);
        
        // 获取统计信息
        EnhancedDisruptorBatchingQueue.BatchMetrics metrics = queue.getMetrics();
        logger.info("批处理统计信息: {}", metrics.toString());
        
        // 验证批处理是否按预期工作
        if (metrics.getTotalBatchesProcessed() >= 3) {
            logger.info("✓ 批处理功能正常工作");
        } else {
            logger.warn("✗ 批处理功能可能有问题");
        }
        
        // 测试消息年龄触发条件
        logger.info("测试消息年龄触发条件...");
        // 提交一个消息然后等待超过maxMessageAgeMs
        String message = "测试年龄触发消息 - " + System.currentTimeMillis() + "\n";
        queue.submit(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        logger.info("提交测试消息，等待触发...");
        Thread.sleep(6000); // 等待超过5秒
        
        // 获取更新后的统计信息
        EnhancedDisruptorBatchingQueue.BatchMetrics metrics2 = queue.getMetrics();
        logger.info("更新后的批处理统计信息: {}", metrics2.toString());
        
        // 关闭队列
        queue.close();
        
        logger.info("测试完成。");
    }
}
