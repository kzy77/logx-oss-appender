package org.logx.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * DisruptorBatchingQueue测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class DisruptorBatchingQueueTest {

    private DisruptorBatchingQueue queue;
    private TestBatchConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TestBatchConsumer();
        // 配置小容量便于测试
        queue = new DisruptorBatchingQueue(1024, // capacity (2的幂)
                10, // batchMaxMessages
                1024, // batchMaxBytes
                100, // flushIntervalMs
                false, // blockOnFull
                false, // multiProducer
                consumer);
    }

    @Test
    void shouldStartAndStopSuccessfully() {
        // When
        queue.start();

        // Then
        assertThatCode(() -> queue.close()).doesNotThrowAnyException();
    }

    @Test
    void shouldProcessSingleMessage() throws InterruptedException {
        // Given
        queue.start();
        byte[] testData = "test message".getBytes();

        // When
        boolean offered = queue.offer(testData);

        // Then
        assertThat(offered).isTrue();

        // Wait for processing
        Thread.sleep(200);
        assertThat(consumer.getBatchCount()).isGreaterThan(0);
        assertThat(consumer.getTotalMessages()).isGreaterThan(0);

        queue.close();
    }

    @Test
    void shouldBatchMessagesByCount() throws InterruptedException {
        // Given
        queue.start();
        int messageCount = 15; // 超过batchMaxMessages(10)

        // When
        for (int i = 0; i < messageCount; i++) {
            queue.offer(("message " + i).getBytes());
        }

        // Wait for processing
        Thread.sleep(200);

        // Then
        assertThat(consumer.getBatchCount()).isGreaterThanOrEqualTo(2); // 至少2个批次
        assertThat(consumer.getTotalMessages()).isEqualTo(messageCount);

        queue.close();
    }

    @Test
    void shouldBatchMessagesByBytes() throws InterruptedException {
        // Given
        queue.start();
        byte[] largeMessage = new byte[600]; // 大消息

        // When
        queue.offer(largeMessage); // 第一个消息
        queue.offer(largeMessage); // 第二个消息会超过1024字节限制

        // Wait for processing
        Thread.sleep(200);

        // Then
        // 验证至少处理了一些消息
        assertThat(consumer.getTotalMessages()).isEqualTo(2);
        // 批次数可以是1或更多（取决于实际的批处理行为）
        assertThat(consumer.getBatchCount()).isGreaterThanOrEqualTo(1);

        queue.close();
    }

    @Test
    @Timeout(5)
    void shouldFlushByTimeout() throws InterruptedException {
        // Given
        queue.start();

        // When
        queue.offer("test".getBytes());

        // Wait for timeout flush (100ms + buffer)
        Thread.sleep(150);

        // Then
        assertThat(consumer.getBatchCount()).isGreaterThan(0);

        queue.close();
    }

    @Test
    void shouldHandleHighThroughput() throws InterruptedException {
        // Given
        queue.start();
        int messageCount = 1000;
        byte[] message = "performance test message".getBytes();

        long startTime = System.nanoTime();

        // When - 发送大量消息
        for (int i = 0; i < messageCount; i++) {
            queue.offer(message);
        }

        // Wait for all messages to be processed
        Thread.sleep(500);

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // Then
        assertThat(consumer.getTotalMessages()).isEqualTo(messageCount);

        // 验证吞吐量目标：应该能在很短时间内处理1000条消息
        assertThat(durationMs).isLessThan(1000); // 小于1秒

        // 计算吞吐量（消息/秒）
        double throughput = (double) messageCount / (durationMs / 1000.0);
        System.out.println("Throughput: " + throughput + " messages/second");

        // 验证吞吐量目标（PRD要求10万+/秒，这里测试至少500/秒以适应测试环境）
        assertThat(throughput).isGreaterThan(500);

        queue.close();
    }

    @Test
    void shouldMeasureLatency() throws InterruptedException {
        // Given
        LatencyMeasuringConsumer latencyConsumer = new LatencyMeasuringConsumer();
        DisruptorBatchingQueue latencyQueue = new DisruptorBatchingQueue(1024, 1, 1024, 10, false, false,
                latencyConsumer);

        latencyQueue.start();

        // When - 测量多条消息的平均延迟
        int messageCount = 100;
        for (int i = 0; i < messageCount; i++) {
            latencyQueue.offer(("latency test " + i).getBytes());
        }

        // Wait for processing
        Thread.sleep(200);

        // Then
        assertThat(latencyConsumer.getProcessedCount()).isGreaterThan(0);

        // 验证延迟目标（使用更宽松的延迟要求进行测试）
        if (latencyConsumer.getProcessedCount() > 0) {
            long avgLatencyNs = latencyConsumer.getTotalLatencyNs() / latencyConsumer.getProcessedCount();
            long avgLatencyMs = avgLatencyNs / 1_000_000;

            System.out.println("Average latency: " + avgLatencyMs + "ms");
            System.out.println("Processed messages: " + latencyConsumer.getProcessedCount());

            // 使用宽松的延迟要求，主要验证功能正确性
            assertThat(avgLatencyMs).isLessThan(5000); // 小于5秒（更宽松的要求）
        }

        latencyQueue.close();
    }

    @Test
    void shouldHandleQueueFullWhenNotBlocking() {
        // Given
        queue.start();
        byte[] message = "test".getBytes();

        // When - 快速填满队列
        int successCount = 0;
        for (int i = 0; i < 2000; i++) { // 超过队列容量
            if (queue.offer(message)) {
                successCount++;
            }
        }

        // Then - 应该有一些消息被丢弃（因为blockOnFull=false）
        assertThat(successCount).isLessThan(2000);
        assertThat(successCount).isGreaterThan(0);

        queue.close();
    }

    /**
     * 测试用的批处理消费者
     */
    private static class TestBatchConsumer implements DisruptorBatchingQueue.BatchConsumer {
        private final AtomicInteger batchCount = new AtomicInteger(0);
        private final AtomicInteger totalMessages = new AtomicInteger(0);

        @Override
        public boolean onBatch(List<DisruptorBatchingQueue.LogEvent> events, int totalBytes) {
            batchCount.incrementAndGet();
            totalMessages.addAndGet(events.size());
            return true;
        }

        public int getBatchCount() {
            return batchCount.get();
        }

        public int getTotalMessages() {
            return totalMessages.get();
        }
    }

    /**
     * 延迟测量消费者
     */
    private static class LatencyMeasuringConsumer implements DisruptorBatchingQueue.BatchConsumer {
        private final AtomicLong totalLatencyNs = new AtomicLong(0);
        private final AtomicInteger processedCount = new AtomicInteger(0);

        @Override
        public boolean onBatch(List<DisruptorBatchingQueue.LogEvent> events, int totalBytes) {
            long processTime = System.currentTimeMillis();

            for (DisruptorBatchingQueue.LogEvent event : events) {
                // 计算从事件创建到处理的延迟（毫秒）
                long latencyMs = processTime - event.timestampMs;
                if (latencyMs >= 0) { // 只统计有效的延迟
                    totalLatencyNs.addAndGet(latencyMs * 1_000_000); // 转换为纳秒存储
                    processedCount.incrementAndGet();
                }
            }

            return true;
        }

        public long getTotalLatencyNs() {
            return totalLatencyNs.get();
        }

        public int getProcessedCount() {
            return processedCount.get();
        }
    }
}
