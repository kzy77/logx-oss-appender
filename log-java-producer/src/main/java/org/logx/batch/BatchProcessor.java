package org.logx.batch;

import org.logx.core.DisruptorBatchingQueue;
import org.logx.core.ResourceProtectedThreadPool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

/**
 * 批处理优化引擎
 * <p>
 * 提供智能的批处理机制，优化网络传输效率和存储性能。 支持可配置的批处理大小、时间触发、动态自适应调整和压缩优化。
 * <p>
 * 主要特性：
 * <ul>
 * <li>可配置批处理大小，默认100条，范围10-10000</li>
 * <li>时间触发机制，超时自动刷新，默认5秒</li>
 * <li>动态批处理大小调整，根据队列深度自适应</li>
 * <li>批处理压缩，减少网络传输数据量</li>
 * <li>完整的性能监控和统计</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class BatchProcessor implements AutoCloseable {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MIN_BATCH_SIZE = 10;
    private static final int MAX_BATCH_SIZE = 10000;
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 5000L; // 5秒
    private static final int DEFAULT_COMPRESSION_THRESHOLD = 1024; // 1KB

    // 批处理配置
    private final Config config;
    private final BatchConsumer consumer;

    // 队列和线程池
    private final DisruptorBatchingQueue queue;
    private final ResourceProtectedThreadPool threadPool;
    private final ScheduledExecutorService scheduler;

    // 自适应批处理大小调整器
    private final AdaptiveBatchSizer adaptiveSizer;

    // 统计信息
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesCompressed = new AtomicLong(0);
    private final AtomicLong totalCompressionSavings = new AtomicLong(0);

    // 运行状态
    private volatile boolean started = false;

    /**
     * 批处理消费接口
     */
    public interface BatchConsumer {
        /**
         * 处理批次数据
         *
         * @param batchData
         *            批次数据（可能已压缩）
         * @param originalSize
         *            原始数据大小
         * @param compressed
         *            是否已压缩
         * @param messageCount
         *            消息数量
         *
         * @return 是否处理成功
         */
        boolean processBatch(byte[] batchData, int originalSize, boolean compressed, int messageCount);
    }

    /**
     * 构造批处理器
     *
     * @param config
     *            批处理配置
     * @param consumer
     *            批次消费者
     */
    public BatchProcessor(Config config, BatchConsumer consumer) {
        this.config = config;
        this.consumer = consumer;

        // 创建线程池
        this.threadPool = new ResourceProtectedThreadPool(
                ResourceProtectedThreadPool.Config.defaultConfig().corePoolSize(1).maximumPoolSize(2));

        // 创建定时器
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "batch-processor-scheduler");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        // 创建自适应调整器
        this.adaptiveSizer = new AdaptiveBatchSizer(config);

        // 创建队列
        this.queue = new DisruptorBatchingQueue(1024, // capacity
                config.batchSize, config.batchSizeBytes, config.flushIntervalMs, false, // blockOnFull
                false, // multiProducer
                new InternalBatchConsumer());
    }

    /**
     * 使用默认配置构造批处理器
     */
    public BatchProcessor(BatchConsumer consumer) {
        this(Config.defaultConfig(), consumer);
    }

    /**
     * 启动批处理器
     */
    public synchronized void start() {
        if (started) {
            return;
        }

        // 启动队列（线程池在创建时已启动）
        queue.start();

        // 启动定时刷新任务
        scheduler.scheduleAtFixedRate(this::periodicFlush, config.flushIntervalMs, config.flushIntervalMs,
                TimeUnit.MILLISECONDS);

        started = true;
    }

    /**
     * 提交消息到批处理器
     *
     * @param message
     *            消息数据
     *
     * @return 是否成功提交
     */
    public boolean submit(byte[] message) {
        if (!started) {
            return false;
        }

        return queue.offer(message);
    }

    /**
     * 定期刷新任务
     */
    private void periodicFlush() {
        // 这里的逻辑由DisruptorBatchingQueue内部的时间触发机制处理
        // 此方法可用于自适应调整和监控
        adaptiveSizer.periodicAdjustment();
    }

    /**
     * 获取批处理统计信息
     */
    public BatchMetrics getMetrics() {
        return new BatchMetrics(totalBatchesProcessed.get(), totalMessagesProcessed.get(), totalBytesProcessed.get(),
                totalBytesCompressed.get(), totalCompressionSavings.get(), adaptiveSizer.getCurrentBatchSize(),
                adaptiveSizer.getAdjustmentCount());
    }

    /**
     * 关闭批处理器
     */
    @Override
    public synchronized void close() {
        if (!started) {
            return;
        }

        started = false;

        // 关闭组件
        scheduler.shutdown();
        queue.close();
        threadPool.close();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 内部批次消费者
     */
    private class InternalBatchConsumer implements DisruptorBatchingQueue.BatchConsumer {

        @Override
        public boolean onBatch(List<DisruptorBatchingQueue.LogEvent> events, int totalBytes) {
            if (events.isEmpty()) {
                return true;
            }

            try {
                // 序列化为NDJSON格式
                byte[] serializedData = serializeToNDJSON(events);

                // 决定是否压缩
                boolean shouldCompress = serializedData.length >= config.compressionThreshold;
                byte[] finalData = serializedData;
                int originalSize = serializedData.length;

                if (shouldCompress && config.enableCompression) {
                    finalData = compressData(serializedData);
                    totalBytesCompressed.addAndGet(finalData.length);
                    totalCompressionSavings.addAndGet(originalSize - finalData.length);
                }

                // 提交到消费者处理
                boolean success = consumer.processBatch(finalData, originalSize, shouldCompress, events.size());

                if (success) {
                    // 更新统计
                    totalBatchesProcessed.incrementAndGet();
                    totalMessagesProcessed.addAndGet(events.size());
                    totalBytesProcessed.addAndGet(originalSize);

                    // 通知自适应调整器
                    adaptiveSizer.onBatchProcessed(events.size(), originalSize, true);
                } else {
                    adaptiveSizer.onBatchProcessed(events.size(), originalSize, false);
                }

                return success;

            } catch (Exception e) {
                System.err.println("批处理失败: " + e.getMessage());
                adaptiveSizer.onBatchProcessed(events.size(), totalBytes, false);
                return false;
            }
        }
    }

    /**
     * 序列化为NDJSON格式
     */
    private byte[] serializeToNDJSON(List<DisruptorBatchingQueue.LogEvent> events) {
        StringBuilder sb = new StringBuilder();

        for (DisruptorBatchingQueue.LogEvent event : events) {
            // 简单的JSON格式化
            sb.append("{\"timestamp\":").append(event.timestampMs).append(",\"data\":\"")
                    .append(new String(event.payload)).append("\"}\n");
        }

        return sb.toString().getBytes();
    }

    /**
     * 压缩数据
     */
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        return baos.toByteArray();
    }

    /**
     * 自适应批处理大小调整器
     */
    private static class AdaptiveBatchSizer {
        private final Config config;
        private final AtomicReference<Integer> currentBatchSize;
        private final AtomicLong adjustmentCount = new AtomicLong(0);

        // 性能指标跟踪
        private volatile long lastAdjustmentTime = System.currentTimeMillis();
        private volatile int successfulBatches = 0;
        private volatile int failedBatches = 0;

        public AdaptiveBatchSizer(Config config) {
            this.config = config;
            this.currentBatchSize = new AtomicReference<>(config.batchSize);
        }

        public void onBatchProcessed(int messageCount, int bytes, boolean success) {
            if (success) {
                successfulBatches++;
            } else {
                failedBatches++;
            }
        }

        public void periodicAdjustment() {
            long now = System.currentTimeMillis();
            if (now - lastAdjustmentTime < 30000) { // 30秒调整一次
                return;
            }

            if (!config.enableAdaptiveSize) {
                return;
            }

            // 简单的自适应逻辑
            int current = currentBatchSize.get();
            if (failedBatches > successfulBatches && current > MIN_BATCH_SIZE) {
                // 失败率高，减小批次
                int newSize = Math.max(MIN_BATCH_SIZE, current - 10);
                if (currentBatchSize.compareAndSet(current, newSize)) {
                    adjustmentCount.incrementAndGet();
                }
            } else if (successfulBatches > failedBatches * 3 && current < MAX_BATCH_SIZE) {
                // 成功率高，增大批次
                int newSize = Math.min(MAX_BATCH_SIZE, current + 10);
                if (currentBatchSize.compareAndSet(current, newSize)) {
                    adjustmentCount.incrementAndGet();
                }
            }

            // 重置计数器
            successfulBatches = 0;
            failedBatches = 0;
            lastAdjustmentTime = now;
        }

        public int getCurrentBatchSize() {
            return currentBatchSize.get();
        }

        public long getAdjustmentCount() {
            return adjustmentCount.get();
        }
    }

    /**
     * 批处理配置类
     */
    public static class Config {
        private int batchSize = DEFAULT_BATCH_SIZE;
        private int batchSizeBytes = 4 * 1024 * 1024; // 4MB
        private long flushIntervalMs = DEFAULT_FLUSH_INTERVAL_MS;
        private boolean enableCompression = true;
        private int compressionThreshold = DEFAULT_COMPRESSION_THRESHOLD;
        private boolean enableAdaptiveSize = true;

        public static Config defaultConfig() {
            return new Config();
        }

        public Config batchSize(int batchSize) {
            this.batchSize = Math.max(MIN_BATCH_SIZE, Math.min(MAX_BATCH_SIZE, batchSize));
            return this;
        }

        public Config batchSizeBytes(int batchSizeBytes) {
            this.batchSizeBytes = batchSizeBytes;
            return this;
        }

        public Config flushIntervalMs(long flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
            return this;
        }

        public Config enableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
            return this;
        }

        public Config compressionThreshold(int compressionThreshold) {
            this.compressionThreshold = compressionThreshold;
            return this;
        }

        public Config enableAdaptiveSize(boolean enableAdaptiveSize) {
            this.enableAdaptiveSize = enableAdaptiveSize;
            return this;
        }

        // Getters
        public int getBatchSize() {
            return batchSize;
        }

        public int getBatchSizeBytes() {
            return batchSizeBytes;
        }

        public long getFlushIntervalMs() {
            return flushIntervalMs;
        }

        public boolean isEnableCompression() {
            return enableCompression;
        }

        public int getCompressionThreshold() {
            return compressionThreshold;
        }

        public boolean isEnableAdaptiveSize() {
            return enableAdaptiveSize;
        }
    }

    /**
     * 批处理统计指标
     */
    public static class BatchMetrics {
        private final long totalBatchesProcessed;
        private final long totalMessagesProcessed;
        private final long totalBytesProcessed;
        private final long totalBytesCompressed;
        private final long totalCompressionSavings;
        private final int currentBatchSize;
        private final long adjustmentCount;

        public BatchMetrics(long totalBatchesProcessed, long totalMessagesProcessed, long totalBytesProcessed,
                long totalBytesCompressed, long totalCompressionSavings, int currentBatchSize, long adjustmentCount) {
            this.totalBatchesProcessed = totalBatchesProcessed;
            this.totalMessagesProcessed = totalMessagesProcessed;
            this.totalBytesProcessed = totalBytesProcessed;
            this.totalBytesCompressed = totalBytesCompressed;
            this.totalCompressionSavings = totalCompressionSavings;
            this.currentBatchSize = currentBatchSize;
            this.adjustmentCount = adjustmentCount;
        }

        // Getters
        public long getTotalBatchesProcessed() {
            return totalBatchesProcessed;
        }

        public long getTotalMessagesProcessed() {
            return totalMessagesProcessed;
        }

        public long getTotalBytesProcessed() {
            return totalBytesProcessed;
        }

        public long getTotalBytesCompressed() {
            return totalBytesCompressed;
        }

        public long getTotalCompressionSavings() {
            return totalCompressionSavings;
        }

        public int getCurrentBatchSize() {
            return currentBatchSize;
        }

        public long getAdjustmentCount() {
            return adjustmentCount;
        }

        public double getCompressionRatio() {
            return totalBytesProcessed > 0 ? (double) totalCompressionSavings / totalBytesProcessed : 0.0;
        }

        public double getAverageMessagesPerBatch() {
            return totalBatchesProcessed > 0 ? (double) totalMessagesProcessed / totalBatchesProcessed : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "BatchMetrics{batches=%d, messages=%d, bytes=%d, compressed=%d, "
                            + "savings=%d (%.1f%%), currentBatchSize=%d, adjustments=%d}",
                    totalBatchesProcessed, totalMessagesProcessed, totalBytesProcessed, totalBytesCompressed,
                    totalCompressionSavings, getCompressionRatio() * 100, currentBatchSize, adjustmentCount);
        }
    }
}
