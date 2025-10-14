package org.logx.test;

import org.logx.core.EnhancedDisruptorBatchingQueue;
import org.logx.core.EnhancedDisruptorBatchingQueue.Config;
import org.logx.core.EnhancedDisruptorBatchingQueue.LogEvent;
import org.logx.storage.StorageService;
import org.logx.storage.ProtocolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LogFormatTest {
    private static final Logger logger = LoggerFactory.getLogger(LogFormatTest.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("开始测试日志格式化功能...");
        
        // 使用反射调用serializeToPatternFormat方法
        Method serializeMethod = EnhancedDisruptorBatchingQueue.class.getDeclaredMethod(
            "serializeToPatternFormat", List.class);
        serializeMethod.setAccessible(true);
        
        // 创建测试事件
        List<LogEvent> events = new ArrayList<>();
        events.add(new LogEvent("2025-10-07 21:00:00.000 [main] INFO  TestLogger - 测试消息1\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), System.currentTimeMillis()));
        events.add(new LogEvent("2025-10-07 21:00:01.000 [main] WARN  TestLogger - 测试消息2\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), System.currentTimeMillis()));
        events.add(new LogEvent("2025-10-07 21:00:02.000 [main] ERROR TestLogger - 测试消息3".getBytes(java.nio.charset.StandardCharsets.UTF_8), System.currentTimeMillis())); // 没有换行符
        
        // 创建队列实例以访问私有方法
        Config config = Config.defaultConfig();
        StorageService mockStorage = new StorageService() {
            public CompletableFuture<Void> putObject(String key, byte[] data) { return CompletableFuture.completedFuture(null); }
            public ProtocolType getProtocolType() { return ProtocolType.S3; }
            public String getBucketName() { return "mock"; }
            public void close() {}
            public boolean supportsProtocol(ProtocolType protocol) { return true; }
        };
        
        EnhancedDisruptorBatchingQueue queue = new EnhancedDisruptorBatchingQueue(config, 
            (data, size, compressed, count) -> true, mockStorage);
        
        // 调用序列化方法
        byte[] result = (byte[]) serializeMethod.invoke(queue, events);
        String formatted = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        
        logger.info("格式化后的日志:");
        logger.info("------------------------------");
        logger.info("{}", formatted);
        logger.info("------------------------------");
        
        // 验证每条日志都以换行符结尾
        String[] lines = formatted.split("\n");
        logger.info("行数: {}", lines.length);
        for (int i = 0; i < lines.length; i++) {
            logger.info("第{}行: {}", (i+1), lines[i]);
        }
        
        logger.info("测试完成。");
    }
}