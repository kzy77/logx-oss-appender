package org.logx.core;

import org.logx.storage.StorageService;
import org.logx.storage.ProtocolType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class EnhancedDisruptorBatchingQueueTest {

    private EnhancedDisruptorBatchingQueue queue;
    private TestStorageService testStorageService;
    private TestBatchConsumer testConsumer;

    static class TestStorageService implements StorageService {
        @Override
        public java.util.concurrent.CompletableFuture<Void> putObject(String key, byte[] data) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        @Override
        public ProtocolType getProtocolType() {
            return ProtocolType.S3;
        }

        @Override
        public String getBucketName() {
            return "test-bucket";
        }

        @Override
        public String getKeyPrefix() {
            return "test-prefix";
        }

        @Override
        public void close() {
            // Do nothing
        }

        @Override
        public boolean supportsProtocol(ProtocolType protocol) {
            return protocol == ProtocolType.S3;
        }
    }

    static class TestBatchConsumer implements EnhancedDisruptorBatchingQueue.BatchConsumer {
        private final AtomicBoolean processed = new AtomicBoolean(false);
        private volatile int messageCount = 0;

        @Override
        public boolean processBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount) {
            this.messageCount = messageCount;
            processed.set(true);
            return true;
        }

        public boolean isProcessed() {
            return processed.get();
        }

        public int getMessageCount() {
            return messageCount;
        }
    }

    @BeforeEach
    void setUp() {
        testStorageService = new TestStorageService();
        testConsumer = new TestBatchConsumer();

        EnhancedDisruptorBatchingQueue.Config config = new EnhancedDisruptorBatchingQueue.Config()
                .queueCapacity(1024)
                .batchMaxMessages(10)
                .batchMaxBytes(1024 * 1024) // 1MB
                .maxMessageAgeMs(30000); // 30秒
        queue = new EnhancedDisruptorBatchingQueue(config, testConsumer, testStorageService);
    }

    @Test
    void testQueueInitializationWithBlockingWaitStrategy() {
        // 测试队列使用BlockingWaitStrategy进行初始化
        // 验证没有异常抛出表明类结构正确
        assertNotNull(queue);

        // 启动队列
        queue.start();

        // 简单提交几个消息验证基本功能
        byte[] testData = "test message".getBytes();
        boolean result = queue.submit(testData);
        assertTrue(result, "Should be able to submit message to queue");

        // 验证队列状态信息
        String queueStatus = queue.getQueueStatusInfo();
        assertNotNull(queueStatus);
        assertTrue(queueStatus.contains("Queue capacity"));

        // 获取指标
        EnhancedDisruptorBatchingQueue.BatchMetrics metrics = queue.getMetrics();
        assertNotNull(metrics);
    }

    @Test
    void testBatchProcessingWithBlockingWaitStrategy() throws InterruptedException {
        // 配置一个小批次大小来触发批处理
        TestBatchConsumer testConsumer = new TestBatchConsumer();
        TestStorageService testStorageService = new TestStorageService();

        EnhancedDisruptorBatchingQueue.Config config = new EnhancedDisruptorBatchingQueue.Config()
                .queueCapacity(1024)
                .batchMaxMessages(2)  // 小批次，便于测试
                .batchMaxBytes(1024 * 1024)
                .maxMessageAgeMs(1000); // 1秒
        EnhancedDisruptorBatchingQueue testQueue = new EnhancedDisruptorBatchingQueue(config, testConsumer, testStorageService);

        testQueue.start();

        // 提交两条消息触发批处理
        byte[] msg1 = "test message 1".getBytes();
        byte[] msg2 = "test message 2".getBytes();

        assertTrue(testQueue.submit(msg1));
        assertTrue(testQueue.submit(msg2));

        // 等待批处理完成
        Thread.sleep(1500);

        // 验证批处理被调用
        assertTrue(testConsumer.isProcessed(), "Batch should be processed");
        assertEquals(2, testConsumer.getMessageCount(), "Should have 2 messages in batch");

        testQueue.close();
    }

    @Test
    void testQueueClose() {
        queue.start();

        // 关闭队列
        assertDoesNotThrow(() -> queue.close());
    }
}