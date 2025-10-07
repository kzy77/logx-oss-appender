package org.logx.compatibility.spring.boot;

import org.logx.core.EnhancedDisruptorBatchingQueue;
import org.logx.core.EnhancedDisruptorBatchingQueue.Config;
import org.logx.storage.StorageService;
import org.logx.storage.s3.S3StorageServiceAdapter;

import java.util.concurrent.CompletableFuture;

public class SimpleTestApp {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("开始测试EnhancedDisruptorBatchingQueue功能...");
        
        // 创建一个模拟的存储服务
        StorageService mockStorageService = new StorageService() {
            @Override
            public CompletableFuture<Void> putObject(String key, byte[] data) {
                System.out.println("模拟上传对象: " + key + ", 数据大小: " + data.length + " 字节");
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
                System.out.println("关闭模拟存储服务");
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
        
        System.out.println("创建EnhancedDisruptorBatchingQueue，配置参数: batchMaxMessages=" + config.getBatchMaxMessages() + 
                          ", maxMessageAgeMs=" + config.getMaxMessageAgeMs());
        
        // 创建批处理队列
        EnhancedDisruptorBatchingQueue queue = new EnhancedDisruptorBatchingQueue(config, 
            (batchData, originalSize, compressed, messageCount) -> {
                System.out.println("处理批次: 消息数量=" + messageCount + ", 原始大小=" + originalSize + " 字节");
                try {
                    mockStorageService.putObject("test-batch-" + System.currentTimeMillis() + ".log", batchData).get();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }, 
            mockStorageService);
        
        // 启动队列
        queue.start();
        
        // 提交一些测试消息
        System.out.println("提交测试消息...");
        for (int i = 0; i < 12; i++) {
            String message = "测试日志消息 #" + i + " - " + System.currentTimeMillis() + "\n";
            queue.submit(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("提交消息 #" + i);
            Thread.sleep(100); // 短暂延迟
        }
        
        // 等待一段时间让批处理完成
        System.out.println("等待批处理完成...");
        Thread.sleep(2000);
        
        // 获取统计信息
        EnhancedDisruptorBatchingQueue.BatchMetrics metrics = queue.getMetrics();
        System.out.println("批处理统计信息: " + metrics.toString());
        
        // 关闭队列
        queue.close();
        
        System.out.println("测试完成。");
    }
}