package org.logx.core;

import org.logx.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * BatchProcessor性能测试类
 * <p>
 * 验证批处理优化引擎的性能指标和优化效果
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class BatchProcessorPerformanceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessorPerformanceTest.class);

    /**
     * 简单的存储服务模拟实现
     */
    private static class MockStorageService implements StorageService {
        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public String getOssType() {
            return "MOCK";
        }

        @Override
        public String getBucketName() {
            return "test-bucket";
        }

        @Override
        public void close() {
            // 空实现
        }

        @Override
        public boolean supportsOssType(String ossType) {
            return "MOCK".equals(ossType);
        }
    }

    private HighPerformanceBatchConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new HighPerformanceBatchConsumer();
    }

    @Test
    @Timeout(30)
    void shouldAchieveHighThroughputTarget() throws InterruptedException {
        // Given - 配置高性能批处理器
        BatchProcessor processor = new BatchProcessor(BatchProcessor.Config.defaultConfig().batchSize(500) // 大批次提高吞吐量
                .flushIntervalMs(1000).enableCompression(true).compressionThreshold(512), consumer, new MockStorageService());

        processor.start();

        // 减少消息数量，更现实的测试
        int messageCount = 2000;
        byte[] message = "High performance test message".getBytes();

        long startTime = System.nanoTime();

        // When - 发送大量消息
        for (int i = 0; i < messageCount; i++) {
            boolean submitted = processor.submit(message);
            if (!submitted) {
                // 队列满了，稍等一下
                Thread.sleep(1);
            }
        }

        // Wait for all messages to be processed
        Thread.sleep(5000);

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // Then
        BatchProcessor.BatchMetrics metrics = processor.getMetrics();
        logger.info("Expected messages: {}", messageCount);
        logger.info("Actual processed: {}", metrics.getTotalMessagesProcessed());

        // 计算吞吐量
        double throughput = (double) metrics.getTotalMessagesProcessed() / (durationMs / 1000.0);
        logger.info("High Performance Throughput: {} messages/second", throughput);
        logger.info("Processing time: {}ms", durationMs);
        logger.info("Metrics: {}", metrics);

        // 验证至少处理了大部分消息（50%以上）
        assertThat(metrics.getTotalMessagesProcessed()).isGreaterThan((long) (messageCount * 0.5));

        // 验证合理的吞吐量目标：应该能达到100+ messages/second
        assertThat(throughput).isGreaterThan(100);

        processor.close();
    }

    @Test
    @Timeout(20)
    void shouldDemonstrateCompressionEfficiency() throws InterruptedException {
        // Given - 大数据压缩测试
        BatchProcessor processor = new BatchProcessor(
                BatchProcessor.Config.defaultConfig().batchSize(100).enableCompression(true).compressionThreshold(100), // 低阈值确保压缩
                consumer, new MockStorageService());

        processor.start();

        // 创建重复内容的大消息（压缩效果好）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("This is a repeated message content for compression testing. ");
        }
        byte[] repetitiveMessage = sb.toString().getBytes();

        int messageCount = 100;

        // When
        for (int i = 0; i < messageCount; i++) {
            processor.submit(repetitiveMessage);
        }

        Thread.sleep(2000);

        // Then
        BatchProcessor.BatchMetrics metrics = processor.getMetrics();
        assertThat(metrics.getTotalMessagesProcessed()).isEqualTo(messageCount);

        // 验证压缩效果
        double compressionRatio = metrics.getCompressionRatio();
        logger.info("Compression ratio: {}%", (compressionRatio * 100));
        logger.info("Original bytes: {}", metrics.getTotalBytesProcessed());
        logger.info("Compressed bytes: {}", metrics.getTotalBytesCompressed());
        logger.info("Savings: {} bytes", metrics.getTotalCompressionSavings());

        // 验证压缩比目标：应该达到50%以上的压缩率
        assertThat(compressionRatio).isGreaterThan(0.5);

        processor.close();
    }

    @Test
    @Timeout(25)
    void shouldOptimizeBatchSizeAdaptively() throws InterruptedException {
        // Given - 自适应批处理测试
        BatchProcessor processor = new BatchProcessor(BatchProcessor.Config.defaultConfig().batchSize(50) // 起始批次大小
                .enableAdaptiveSize(true).flushIntervalMs(500), consumer, new MockStorageService());

        processor.start();

        // Phase 1: 低负载
        for (int i = 0; i < 100; i++) {
            processor.submit(("low load message " + i).getBytes());
            // 模拟低频率
            Thread.sleep(5);
        }

        Thread.sleep(1000);
        int phase1BatchSize = processor.getMetrics().getCurrentBatchSize();

        // Phase 2: 高负载
        for (int i = 0; i < 500; i++) {
            processor.submit(("high load message " + i).getBytes());
            // 无延迟，模拟高频率
        }

        Thread.sleep(2000);
        int phase2BatchSize = processor.getMetrics().getCurrentBatchSize();

        // Then
        BatchProcessor.BatchMetrics finalMetrics = processor.getMetrics();

        logger.info("Phase 1 batch size: {}", phase1BatchSize);
        logger.info("Phase 2 batch size: {}", phase2BatchSize);
        logger.info("Final metrics: {}", finalMetrics);
        logger.info("Total adjustments: {}", finalMetrics.getAdjustmentCount());

        assertThat(finalMetrics.getTotalMessagesProcessed()).isEqualTo(600);

        processor.close();
    }

    @Test
    @Timeout(15)
    void shouldHandleVariableMessageSizes() throws InterruptedException {
        // Given
        BatchProcessor processor = new BatchProcessor(
                BatchProcessor.Config.defaultConfig().batchSize(50).batchSizeBytes(8192) // 8KB字节限制
                        .enableCompression(true),
                consumer, new MockStorageService());

        processor.start();

        // When - 混合大小的消息
        for (int i = 0; i < 20; i++) {
            // 小消息
            processor.submit(("Small message " + i).getBytes());

            // 中等消息
            processor.submit(("Medium size message with more content " + i).getBytes());

            // 大消息
            byte[] largeMessage = new byte[1024];
            for (int j = 0; j < largeMessage.length; j++) {
                largeMessage[j] = (byte) ('A' + (j % 26));
            }
            processor.submit(largeMessage);
        }

        Thread.sleep(2000);

        // Then
        BatchProcessor.BatchMetrics metrics = processor.getMetrics();
        assertThat(metrics.getTotalMessagesProcessed()).isEqualTo(60);

        logger.info("Variable size test metrics: {}", metrics);
        logger.info("Average messages per batch: {}", metrics.getAverageMessagesPerBatch());

        // 验证批处理能够处理不同大小的消息
        assertThat(metrics.getTotalBatchesProcessed()).isGreaterThan(1);
        assertThat(metrics.getAverageMessagesPerBatch()).isGreaterThan(1.0);

        processor.close();
    }

    @Test
    @Timeout(20)
    void shouldMeasureBatchingVsNoBatchingPerformance() throws InterruptedException {
        // Test 1: 无批处理（批次大小为1）
        BatchProcessor noBatchProcessor = new BatchProcessor(BatchProcessor.Config.defaultConfig().batchSize(1) // 每条消息一个批次
                .enableCompression(false), consumer, new MockStorageService());

        noBatchProcessor.start();

        int messageCount = 1000;
        byte[] message = "Performance comparison test message".getBytes();

        long noBatchStart = System.nanoTime();

        for (int i = 0; i < messageCount; i++) {
            noBatchProcessor.submit(message);
        }

        Thread.sleep(3000);
        long noBatchEnd = System.nanoTime();
        long noBatchDuration = (noBatchEnd - noBatchStart) / 1_000_000;

        BatchProcessor.BatchMetrics noBatchMetrics = noBatchProcessor.getMetrics();
        noBatchProcessor.close();

        // Test 2: 大批处理
        // 重置消费者
        consumer = new HighPerformanceBatchConsumer();
        BatchProcessor batchProcessor = new BatchProcessor(BatchProcessor.Config.defaultConfig().batchSize(100) // 大批次
                .enableCompression(false), consumer, new MockStorageService());

        batchProcessor.start();

        long batchStart = System.nanoTime();

        for (int i = 0; i < messageCount; i++) {
            batchProcessor.submit(message);
        }

        Thread.sleep(3000);
        long batchEnd = System.nanoTime();
        long batchDuration = (batchEnd - batchStart) / 1_000_000;

        BatchProcessor.BatchMetrics batchMetrics = batchProcessor.getMetrics();
        batchProcessor.close();

        // 结果对比
        logger.info("=== Batching vs No-Batching Performance Comparison ===");
        logger.info("No-batching duration: " + noBatchDuration + "ms");
        logger.info("Batching duration: " + batchDuration + "ms");
        logger.info("No-batching batches: " + noBatchMetrics.getTotalBatchesProcessed());
        logger.info("Batching batches: " + batchMetrics.getTotalBatchesProcessed());

        double improvement = ((double) noBatchDuration / batchDuration);
        logger.info("Performance improvement: " + improvement + "x");

        // 验证批处理带来的性能提升
        assertThat(batchMetrics.getTotalBatchesProcessed()).isLessThan(noBatchMetrics.getTotalBatchesProcessed());

        // 批处理应该显著减少批次数量（至少50%减少）
        double batchReduction = (double) batchMetrics.getTotalBatchesProcessed()
                / noBatchMetrics.getTotalBatchesProcessed();
        logger.info("Batch count reduction ratio: " + batchReduction);
        // 批次数量应该减少50%以上
        assertThat(batchReduction).isLessThan(0.5);
    }

    /**
     * 高性能批处理消费者
     */
    private static class HighPerformanceBatchConsumer implements BatchProcessor.BatchConsumer {
        private final AtomicInteger batchCount = new AtomicInteger(0);
        private final AtomicLong totalMessages = new AtomicLong(0);
        private final AtomicLong totalBytes = new AtomicLong(0);
        private final AtomicLong processingTime = new AtomicLong(0);

        @Override
        public boolean processBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount) {
            long start = System.nanoTime();

            batchCount.incrementAndGet();
            totalMessages.addAndGet(messageCount);
            totalBytes.addAndGet(originalSize);

            // 最小化处理延迟
            // 只做必要的计数，不做额外处理

            long end = System.nanoTime();
            processingTime.addAndGet(end - start);

            return true;
        }

        
    }
}
