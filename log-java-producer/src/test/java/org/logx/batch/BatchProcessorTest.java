package org.logx.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * BatchProcessor测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class BatchProcessorTest {

    private BatchProcessor processor;
    private TestBatchConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TestBatchConsumer();
        processor = new BatchProcessor(BatchProcessor.Config.defaultConfig().batchSize(10).flushIntervalMs(100)
                .enableCompression(true).compressionThreshold(100), consumer);
    }

    @Test
    void shouldCreateBatchProcessorWithDefaultConfig() {
        // When
        BatchProcessor defaultProcessor = new BatchProcessor(consumer);

        // Then
        assertThat(defaultProcessor).isNotNull();
        defaultProcessor.close();
    }

    @Test
    void shouldStartAndStopSuccessfully() {
        // When & Then
        assertThatCode(() -> {
            processor.start();
            processor.close();
        }).doesNotThrowAnyException();
    }

    @Test
    @Timeout(10)
    void shouldProcessSingleMessage() throws InterruptedException {
        // Given
        processor.start();
        byte[] testMessage = "test message".getBytes();

        // When
        boolean submitted = processor.submit(testMessage);

        // Then
        assertThat(submitted).isTrue();

        // Wait for processing
        Thread.sleep(200);

        BatchProcessor.BatchMetrics metrics = processor.getMetrics();
        assertThat(metrics.getTotalMessagesProcessed()).isGreaterThan(0);

        processor.close();
    }

    @Test
    @Timeout(10)
    void shouldBatchMultipleMessages() throws InterruptedException {
        // Given
        processor.start();
        int messageCount = 25; // 超过批次大小10

        // When
        for (int i = 0; i < messageCount; i++) {
            processor.submit(("message " + i).getBytes());
        }

        // Wait for processing
        Thread.sleep(500);

        // Then
        BatchProcessor.BatchMetrics metrics = processor.getMetrics();
        assertThat(metrics.getTotalMessagesProcessed()).isEqualTo(messageCount);
        assertThat(metrics.getTotalBatchesProcessed()).isGreaterThanOrEqualTo(2); // 至少2个批次

        processor.close();
    }

    @Test
    @Timeout(15)
    void shouldTriggerTimeBasedFlush() throws InterruptedException {
        // Given
        BatchProcessor timeBasedProcessor = new BatchProcessor(BatchProcessor.Config.defaultConfig().batchSize(100) // 大批次，不会被数量触发
                .flushIntervalMs(200), // 200ms超时
                consumer);

        timeBasedProcessor.start();

        // When
        timeBasedProcessor.submit("test message".getBytes());

        // Wait for timeout flush
        Thread.sleep(400);

        // Then
        BatchProcessor.BatchMetrics metrics = timeBasedProcessor.getMetrics();
        assertThat(metrics.getTotalMessagesProcessed()).isGreaterThan(0);
        assertThat(metrics.getTotalBatchesProcessed()).isGreaterThan(0);

        timeBasedProcessor.close();
    }

    @Test
    @Timeout(10)
    void shouldCompressLargeMessages() throws InterruptedException {
        // Given
        processor.start();
        byte[] largeMessage = new byte[1024]; // 1KB，超过压缩阈值100字节
        for (int i = 0; i < largeMessage.length; i++) {
            largeMessage[i] = (byte) ('A' + (i % 26));
        }

        // When
        for (int i = 0; i < 5; i++) {
            processor.submit(largeMessage);
        }

        // Wait for processing
        Thread.sleep(500);

        // Then
        BatchProcessor.BatchMetrics metrics = processor.getMetrics();
        assertThat(metrics.getTotalBytesCompressed()).isGreaterThan(0);
        assertThat(metrics.getTotalCompressionSavings()).isGreaterThan(0);
        assertThat(metrics.getCompressionRatio()).isGreaterThan(0.0);

        processor.close();
    }

    @Test
    @Timeout(10)
    void shouldMeasurePerformanceMetrics() throws InterruptedException {
        // Given
        processor.start();
        int messageCount = 100;

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < messageCount; i++) {
            processor.submit(("performance test message " + i).getBytes());
        }

        // Wait for processing
        Thread.sleep(1000);

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        // Then
        BatchProcessor.BatchMetrics metrics = processor.getMetrics();
        assertThat(metrics.getTotalMessagesProcessed()).isEqualTo(messageCount);

        // 验证吞吐量
        double throughput = (double) messageCount / (durationMs / 1000.0);
        System.out.println("BatchProcessor Throughput: " + throughput + " messages/second");
        assertThat(throughput).isGreaterThan(50); // 至少50条/秒

        // 验证批处理效率
        assertThat(metrics.getAverageMessagesPerBatch()).isGreaterThan(1.0);

        processor.close();
    }

    @Test
    @Timeout(10)
    void shouldHandleAdaptiveBatchSizing() throws InterruptedException {
        // Given - 启用自适应批处理
        BatchProcessor adaptiveProcessor = new BatchProcessor(
                BatchProcessor.Config.defaultConfig().batchSize(20).enableAdaptiveSize(true).flushIntervalMs(100),
                consumer);

        adaptiveProcessor.start();

        // When - 提交大量消息触发自适应调整
        for (int i = 0; i < 200; i++) {
            adaptiveProcessor.submit(("adaptive test " + i).getBytes());
            if (i % 50 == 0) {
                Thread.sleep(50); // 间歇性暂停模拟不同负载
            }
        }

        // Wait for processing and adaptive adjustments
        Thread.sleep(2000);

        // Then
        BatchProcessor.BatchMetrics metrics = adaptiveProcessor.getMetrics();
        assertThat(metrics.getTotalMessagesProcessed()).isEqualTo(200);

        System.out.println("Adaptive metrics: " + metrics);
        System.out.println("Current batch size: " + metrics.getCurrentBatchSize());
        System.out.println("Total adjustments: " + metrics.getAdjustmentCount());

        adaptiveProcessor.close();
    }

    @Test
    @Timeout(10)
    void shouldHandleCompressionThreshold() throws InterruptedException {
        // Given - 高压缩阈值，小消息不压缩
        BatchProcessor noCompressionProcessor = new BatchProcessor(
                BatchProcessor.Config.defaultConfig().batchSize(5).enableCompression(true).compressionThreshold(10000), // 10KB阈值，小消息不会压缩
                consumer);

        noCompressionProcessor.start();

        // When
        for (int i = 0; i < 10; i++) {
            noCompressionProcessor.submit("small message".getBytes());
        }

        Thread.sleep(500);

        // Then
        BatchProcessor.BatchMetrics metrics = noCompressionProcessor.getMetrics();
        assertThat(metrics.getTotalBytesCompressed()).isEqualTo(0); // 不应该有压缩
        assertThat(metrics.getTotalCompressionSavings()).isEqualTo(0);

        noCompressionProcessor.close();
    }

    @Test
    @Timeout(10)
    void shouldProvideDetailedMetrics() throws InterruptedException {
        // Given
        processor.start();

        // When
        for (int i = 0; i < 30; i++) {
            processor.submit(("detailed metrics test " + i).getBytes());
        }

        Thread.sleep(500);

        // Then
        BatchProcessor.BatchMetrics metrics = processor.getMetrics();

        // 验证所有指标都有效
        assertThat(metrics.getTotalBatchesProcessed()).isGreaterThan(0);
        assertThat(metrics.getTotalMessagesProcessed()).isEqualTo(30);
        assertThat(metrics.getTotalBytesProcessed()).isGreaterThan(0);
        assertThat(metrics.getCurrentBatchSize()).isGreaterThan(0);

        // 验证计算指标
        assertThat(metrics.getAverageMessagesPerBatch()).isGreaterThan(0.0);

        // 验证toString方法
        String metricsString = metrics.toString();
        assertThat(metricsString).contains("BatchMetrics");
        assertThat(metricsString).contains("batches=");
        assertThat(metricsString).contains("messages=30");

        processor.close();
    }

    @Test
    void shouldValidateBatchSizeConfiguration() {
        // Test boundary values
        BatchProcessor.Config config = BatchProcessor.Config.defaultConfig();

        // When & Then - 测试边界值
        config.batchSize(-10); // 负值应该被修正为最小值
        assertThat(config.getBatchSize()).isEqualTo(10); // MIN_BATCH_SIZE

        config.batchSize(50000); // 超大值应该被修正为最大值
        assertThat(config.getBatchSize()).isEqualTo(10000); // MAX_BATCH_SIZE

        config.batchSize(500); // 正常值
        assertThat(config.getBatchSize()).isEqualTo(500);
    }

    /**
     * 测试用的批处理消费者
     */
    private static class TestBatchConsumer implements BatchProcessor.BatchConsumer {
        private final AtomicInteger batchCount = new AtomicInteger(0);
        private final AtomicLong totalMessages = new AtomicLong(0);
        private final AtomicLong totalBytes = new AtomicLong(0);

        @Override
        public boolean processBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount) {
            batchCount.incrementAndGet();
            totalMessages.addAndGet(messageCount);
            totalBytes.addAndGet(originalSize);

            // 模拟处理延迟
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            return true; // 总是成功
        }

        public int getBatchCount() {
            return batchCount.get();
        }

        public long getTotalMessages() {
            return totalMessages.get();
        }

        public long getTotalBytes() {
            return totalBytes.get();
        }
    }
}
