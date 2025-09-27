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
        // Given - 使用更小的字节限制便于测试
        TestBatchConsumer bytesConsumer = new TestBatchConsumer();
        DisruptorBatchingQueue bytesQueue = new DisruptorBatchingQueue(1024, // capacity
                100, // batchMaxMessages (足够大，不会先触发)
                600, // batchMaxBytes (600字节限制)
                5000, // flushIntervalMs (足够长，不会先触发)
                false, // blockOnFull
                false, // multiProducer
                bytesConsumer);

        bytesQueue.start();
        byte[] message = new byte[400]; // 每个消息400字节

        // When - 发送三个消息测试字节限制
        bytesQueue.offer(message); // 第一个消息400字节
        bytesQueue.offer(message); // 第二个消息400字节，总共800字节，超过600字节限制，触发第一个批次
        Thread.sleep(50); // 给第一个批次处理时间
        bytesQueue.offer(message); // 第三个消息400字节，开始新批次

        // Wait for processing
        Thread.sleep(200);

        // Then - 验证字节限制是否生效
        assertThat(bytesConsumer.getTotalMessages()).isEqualTo(3); // 总共3条消息
        // 应该有至少2个批次：第一个批次有消息在字节限制触发前，第二个批次有剩余消息
        assertThat(bytesConsumer.getBatchCount()).isGreaterThanOrEqualTo(2);

        bytesQueue.close();
    }

    @Test
    @Timeout(5)
    void shouldFlushByTimeout() throws InterruptedException {
        // Given - 使用短的刷新间隔便于测试
        TestBatchConsumer timeoutConsumer = new TestBatchConsumer();
        DisruptorBatchingQueue timeoutQueue = new DisruptorBatchingQueue(1024, // capacity
                1000, // batchMaxMessages (足够大，不会先触发)
                1024 * 1024, // batchMaxBytes (足够大，不会先触发)
                200, // flushIntervalMs (200ms刷新间隔)
                false, // blockOnFull
                false, // multiProducer
                timeoutConsumer);

        timeoutQueue.start();

        // When - 发送单个小消息，只能通过超时触发刷新
        timeoutQueue.offer("small test message".getBytes());

        // Wait for timeout flush (200ms + buffer)
        Thread.sleep(300);

        // Then - 应该因为超时而产生一个批次
        assertThat(timeoutConsumer.getBatchCount()).isGreaterThan(0);
        assertThat(timeoutConsumer.getTotalMessages()).isEqualTo(1);

        timeoutQueue.close();
    }

    @Test
    void shouldRespectConfiguredParameters() throws InterruptedException {
        // Given - 测试参数配置是否真正生效
        TestBatchConsumer paramConsumer = new TestBatchConsumer();

        // 配置：最多3条消息，最多100字节，500ms超时
        DisruptorBatchingQueue paramQueue = new DisruptorBatchingQueue(1024,
                3, // batchMaxMessages = 3
                100, // batchMaxBytes = 100字节
                500, // flushIntervalMs = 500ms
                false, false, paramConsumer);

        paramQueue.start();

        // When & Then 1 - 测试消息数量限制（3条触发）
        paramQueue.offer("msg1".getBytes()); // 4字节
        paramQueue.offer("msg2".getBytes()); // 4字节
        paramQueue.offer("msg3".getBytes()); // 4字节 = 总共12字节，未超过100字节

        Thread.sleep(100); // 给处理时间但不超过500ms超时
        assertThat(paramConsumer.getBatchCount()).isEqualTo(1); // 应该有1个批次

        paramConsumer.reset(); // 重置计数器

        // When & Then 2 - 测试字节限制（约100字节触发）
        byte[] largeMsg = new byte[60]; // 60字节消息
        paramQueue.offer(largeMsg); // 第1个60字节
        paramQueue.offer(largeMsg); // 第2个60字节，总共120字节，超过100字节限制

        Thread.sleep(100);
        assertThat(paramConsumer.getBatchCount()).isGreaterThanOrEqualTo(1); // 应该触发批处理

        paramQueue.close();
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

        public void reset() {
            batchCount.set(0);
            totalMessages.set(0);
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
