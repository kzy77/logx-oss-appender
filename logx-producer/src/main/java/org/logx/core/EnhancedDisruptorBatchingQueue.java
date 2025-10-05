package org.logx.core;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.logx.config.CommonConfig;
import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

/**
 * 增强的Disruptor批处理队列
 * <p>
 * 集成了以下功能：
 * <ul>
 * <li>高性能环形队列（LMAX Disruptor）</li>
 * <li>智能批处理聚合（三个触发条件：消息数、字节数、消息年龄）</li>
 * <li>NDJSON序列化</li>
 * <li>GZIP压缩</li>
 * <li>数据分片处理</li>
 * <li>完整的性能统计指标</li>
 * </ul>
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public final class EnhancedDisruptorBatchingQueue implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDisruptorBatchingQueue.class);

    /**
     * 单条日志事件
     */
    public static class LogEvent {
        public final byte[] payload;
        public final long timestampMs;

        public LogEvent(byte[] payload, long timestampMs) {
            this.payload = payload;
            this.timestampMs = timestampMs;
        }
    }

    /**
     * 批次消费接口
     */
    public interface BatchConsumer {
        /**
         * 处理批次数据
         *
         * @param batchData
         *            批次数据（已序列化，可能已压缩）
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
     * 事件载体，避免频繁分配
     */
    private static final class LogEventHolder {
        byte[] payload;
        long timestampMs;

        void set(byte[] p, long ts) {
            this.payload = p;
            this.timestampMs = ts;
        }

        void clear() {
            this.payload = null;
            this.timestampMs = 0L;
        }
    }

    private final Config config;
    private final BatchConsumer consumer;
    private final StorageService storageService;
    private final Disruptor<LogEventHolder> disruptor;
    private final RingBuffer<LogEventHolder> ringBuffer;

    private volatile boolean started = false;

    /**
     * 统计信息
     */
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesCompressed = new AtomicLong(0);
    private final AtomicLong totalCompressionSavings = new AtomicLong(0);
    private final AtomicLong totalShardsCreated = new AtomicLong(0);

    /**
     * 构造增强批处理队列
     *
     * @param config
     *            队列配置
     * @param consumer
     *            批次消费者
     * @param storageService
     *            存储服务（用于分片上传）
     */
    public EnhancedDisruptorBatchingQueue(Config config, BatchConsumer consumer, StorageService storageService) {
        this.config = config;
        this.consumer = consumer;
        this.storageService = storageService;

        EventFactory<LogEventHolder> factory = LogEventHolder::new;
        ProducerType type = config.multiProducer ? ProducerType.MULTI : ProducerType.SINGLE;

        this.disruptor = new Disruptor<>(
                factory,
                config.queueCapacity,
                r -> {
                    Thread t = new Thread(r, "enhanced-disruptor-consumer");
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                },
                type,
                new YieldingWaitStrategy());

        this.disruptor.handleEventsWith(new BatchEventHandler());
        this.ringBuffer = disruptor.getRingBuffer();
    }

    /**
     * 启动队列
     */
    public synchronized void start() {
        if (started) {
            return;
        }
        disruptor.start();
        started = true;
    }

    /**
     * 提交消息
     *
     * @param payload
     *            消息数据
     *
     * @return 是否成功提交
     */
    public boolean submit(byte[] payload) {
        if (!started) {
            return false;
        }

        long ts = System.currentTimeMillis();
        while (true) {
            if (ringBuffer.hasAvailableCapacity(1)) {
                long seq = ringBuffer.next();
                try {
                    LogEventHolder slot = ringBuffer.get(seq);
                    slot.set(payload, ts);
                } finally {
                    ringBuffer.publish(seq);
                }
                return true;
            }

            if (!config.blockOnFull) {
                logger.error("[DATA_LOSS_ALERT] 队列已满，数据被丢弃。队列容量：{}，当前剩余容量：0",
                        ringBuffer.getBufferSize());
                return false;
            }

            try {
                Thread.sleep(1L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * 获取批处理统计信息
     */
    public BatchMetrics getMetrics() {
        return new BatchMetrics(
                totalBatchesProcessed.get(),
                totalMessagesProcessed.get(),
                totalBytesProcessed.get(),
                totalBytesCompressed.get(),
                totalCompressionSavings.get(),
                config.batchMaxMessages,
                totalShardsCreated.get());
    }

    /**
     * 关闭队列
     */
    @Override
    public synchronized void close() {
        if (!started) {
            return;
        }
        disruptor.shutdown();
        started = false;
    }

    /**
     * 批处理事件处理器
     */
    private class BatchEventHandler implements EventHandler<LogEventHolder> {
        private final List<LogEvent> buffer;
        private int bytes = 0;
        private long oldestMessageTime = 0L;

        BatchEventHandler() {
            this.buffer = new ArrayList<>(config.batchMaxMessages);
        }

        @Override
        public void onEvent(LogEventHolder ev, long sequence, boolean endOfBatch) {
            if (ev.payload != null) {
                if (buffer.isEmpty()) {
                    oldestMessageTime = ev.timestampMs;
                }

                int size = ev.payload.length;
                boolean willExceedCount = buffer.size() + 1 > config.batchMaxMessages;
                boolean willExceedBytes = bytes + size > config.batchMaxBytes;

                if (willExceedCount || willExceedBytes) {
                    if (!buffer.isEmpty()) {
                        processBatch();
                    }
                }

                buffer.add(new LogEvent(ev.payload, ev.timestampMs));
                bytes += size;
                ev.clear();
            }

            long now = System.currentTimeMillis();
            boolean messageAgeExceeded = !buffer.isEmpty() &&
                    (now - oldestMessageTime) >= config.maxMessageAgeMs;

            if (endOfBatch || messageAgeExceeded ||
                    buffer.size() >= config.batchMaxMessages ||
                    bytes >= config.batchMaxBytes) {
                if (!buffer.isEmpty()) {
                    processBatch();
                }
            }
        }

        private void processBatch() {
            try {
                byte[] serializedData = serializeToNDJSON(buffer);
                int originalSize = serializedData.length;

                boolean shouldCompress = config.enableCompression &&
                        serializedData.length >= config.compressionThreshold;

                byte[] finalData = serializedData;
                if (shouldCompress) {
                    finalData = compressData(serializedData);
                    totalBytesCompressed.addAndGet(finalData.length);
                    totalCompressionSavings.addAndGet(originalSize - finalData.length);
                }

                boolean success;
                if (config.enableSharding && originalSize > config.shardingThreshold) {
                    success = processSharding(serializedData);
                } else {
                    success = consumer.processBatch(finalData, originalSize, shouldCompress, buffer.size());
                }

                if (success) {
                    totalBatchesProcessed.incrementAndGet();
                    totalMessagesProcessed.addAndGet(buffer.size());
                    totalBytesProcessed.addAndGet(originalSize);
                }

            } catch (Exception e) {
                logger.error("批处理失败: {}", e.getMessage(), e);
            } finally {
                buffer.clear();
                bytes = 0;
                oldestMessageTime = 0L;
            }
        }
    }

    /**
     * 序列化为NDJSON格式
     */
    private byte[] serializeToNDJSON(List<LogEvent> events) {
        StringBuilder sb = new StringBuilder();
        for (LogEvent event : events) {
            sb.append("{\"timestamp\":").append(event.timestampMs)
                    .append(",\"data\":\"")
                    .append(new String(event.payload, java.nio.charset.StandardCharsets.UTF_8))
                    .append("\"}\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
     * 处理数据分片
     */
    private boolean processSharding(byte[] data) {
        try {
            int shardCount = (int) Math.ceil((double) data.length / config.shardSize);

            if (shardCount <= 1) {
                return consumer.processBatch(data, data.length, false, 1);
            }

            for (int i = 0; i < shardCount; i++) {
                int start = i * config.shardSize;
                int end = Math.min(start + config.shardSize, data.length);
                int length = end - start;

                byte[] shardData = new byte[length];
                System.arraycopy(data, start, shardData, 0, length);

                String shardKey = "logs/batch-" + System.currentTimeMillis() +
                        "_part_" + String.format("%04d", i + 1) + ".log";
                totalShardsCreated.incrementAndGet();

                CompletableFuture<Void> future = storageService.putObject(shardKey, shardData);
                future.get(30, TimeUnit.SECONDS);
            }

            return true;
        } catch (InterruptedException e) {
            logger.error("分片处理被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.error("分片处理失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 队列配置
     */
    public static class Config {

        private int queueCapacity = CommonConfig.Defaults.QUEUE_CAPACITY;
        private int batchMaxMessages = CommonConfig.Defaults.MAX_BATCH_COUNT;
        private int batchMaxBytes = CommonConfig.Defaults.MAX_BATCH_BYTES;
        private long maxMessageAgeMs = CommonConfig.Defaults.MAX_MESSAGE_AGE_MS;
        private boolean blockOnFull = false;
        private boolean multiProducer = true;
        private boolean enableCompression = CommonConfig.Defaults.ENABLE_COMPRESSION;
        private int compressionThreshold = CommonConfig.Defaults.COMPRESSION_THRESHOLD;
        private boolean enableSharding = CommonConfig.Defaults.ENABLE_SHARDING;
        private int shardingThreshold = CommonConfig.Defaults.SHARDING_THRESHOLD;
        private int shardSize = CommonConfig.Defaults.SHARD_SIZE;

        public static Config defaultConfig() {
            return new Config();
        }

        public Config queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Config batchMaxMessages(int batchMaxMessages) {
            this.batchMaxMessages = Math.max(10, Math.min(10000, batchMaxMessages));
            return this;
        }

        public Config batchMaxBytes(int batchMaxBytes) {
            this.batchMaxBytes = batchMaxBytes;
            return this;
        }

        public Config maxMessageAgeMs(long maxMessageAgeMs) {
            this.maxMessageAgeMs = maxMessageAgeMs;
            return this;
        }

        public Config blockOnFull(boolean blockOnFull) {
            this.blockOnFull = blockOnFull;
            return this;
        }

        public Config multiProducer(boolean multiProducer) {
            this.multiProducer = multiProducer;
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

        public Config enableSharding(boolean enableSharding) {
            this.enableSharding = enableSharding;
            return this;
        }

        public Config shardingThreshold(int shardingThreshold) {
            this.shardingThreshold = shardingThreshold;
            return this;
        }

        public Config shardSize(int shardSize) {
            this.shardSize = shardSize;
            return this;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public int getBatchMaxMessages() {
            return batchMaxMessages;
        }

        public int getBatchMaxBytes() {
            return batchMaxBytes;
        }

        public long getMaxMessageAgeMs() {
            return maxMessageAgeMs;
        }

        public boolean isBlockOnFull() {
            return blockOnFull;
        }

        public boolean isMultiProducer() {
            return multiProducer;
        }

        public boolean isEnableCompression() {
            return enableCompression;
        }

        public int getCompressionThreshold() {
            return compressionThreshold;
        }

        public boolean isEnableSharding() {
            return enableSharding;
        }

        public int getShardingThreshold() {
            return shardingThreshold;
        }

        public int getShardSize() {
            return shardSize;
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
        private final long totalShardsCreated;

        public BatchMetrics(long totalBatchesProcessed, long totalMessagesProcessed,
                long totalBytesProcessed, long totalBytesCompressed,
                long totalCompressionSavings, int currentBatchSize,
                long totalShardsCreated) {
            this.totalBatchesProcessed = totalBatchesProcessed;
            this.totalMessagesProcessed = totalMessagesProcessed;
            this.totalBytesProcessed = totalBytesProcessed;
            this.totalBytesCompressed = totalBytesCompressed;
            this.totalCompressionSavings = totalCompressionSavings;
            this.currentBatchSize = currentBatchSize;
            this.totalShardsCreated = totalShardsCreated;
        }

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

        public long getTotalShardsCreated() {
            return totalShardsCreated;
        }

        public double getCompressionRatio() {
            return totalBytesProcessed > 0
                    ? (double) totalCompressionSavings / totalBytesProcessed
                    : 0.0;
        }

        public double getAverageMessagesPerBatch() {
            return totalBatchesProcessed > 0
                    ? (double) totalMessagesProcessed / totalBatchesProcessed
                    : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "BatchMetrics{batches=%d, messages=%d, bytes=%d, compressed=%d, "
                            + "savings=%d (%.1f%%), currentBatchSize=%d, shards=%d}",
                    totalBatchesProcessed, totalMessagesProcessed, totalBytesProcessed,
                    totalBytesCompressed, totalCompressionSavings,
                    getCompressionRatio() * 100, currentBatchSize, totalShardsCreated);
        }
    }
}
