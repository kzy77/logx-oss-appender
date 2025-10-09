package org.logx.core;

import org.junit.jupiter.api.Test;
import org.logx.storage.StorageService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 性能优化测试 - 验证并行上传和动态批处理
 */
class PerformanceOptimizationTest {

    // 记录上传次数的存储服务实现
    private static class CountingStorageService implements StorageService {
        private final AtomicInteger uploadCount = new AtomicInteger(0);
        private final CountDownLatch latch;
        private final long simulatedDelay;

        CountingStorageService(CountDownLatch latch, long simulatedDelay) {
            this.latch = latch;
            this.simulatedDelay = simulatedDelay;
        }

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            return CompletableFuture.runAsync(() -> {
                try {
                    // 模拟网络延迟
                    Thread.sleep(simulatedDelay);

                    int count = uploadCount.incrementAndGet();
                    System.out.println("并行上传 #" + count + ": " + key +
                                     ", 数据长度: " + data.length +
                                     ", 线程: " + Thread.currentThread().getName());
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        @Override
        public String getOssType() {
            return "test";
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
            return "test".equals(ossType);
        }

        public int getUploadCount() {
            return uploadCount.get();
        }
    }

    @Test
    void shouldHandleHighVolumeLogsWithParallelUpload() throws Exception {
        // 预期会有多个批次上传
        CountDownLatch latch = new CountDownLatch(3);
        CountingStorageService storageService = new CountingStorageService(latch, 100); // 100ms延迟

        // 创建优化配置
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(10)  // 小批次，快速触发
                .batchMaxBytes(1024)
                .maxMessageAgeMs(5000)
                .parallelUploadThreads(3)  // 3个并行线程
                .enableDynamicBatching(true)
                .queuePressureMonitorIntervalMs(200);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        // 快速生产大量日志
        String logTemplate = "高频日志消息 - ID: %d, 时间戳: %d, 内容: 这是一条测试日志";
        for (int i = 0; i < 35; i++) {
            String logMessage = String.format(logTemplate, i, System.currentTimeMillis());
            engine.put(logMessage.getBytes());

            // 适当间隔避免过于密集
            if (i % 5 == 0) {
                Thread.sleep(10);
            }
        }

        // 等待所有上传完成
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(storageService.getUploadCount()).isGreaterThanOrEqualTo(3);

        System.out.println("性能测试完成 - 总计上传: " + storageService.getUploadCount() + " 个批次");

        // 关闭引擎
        engine.close();
    }

    @Test
    void shouldAdaptToQueuePressure() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CountingStorageService storageService = new CountingStorageService(latch, 200); // 较高延迟模拟压力

        // 启用动态批处理配置
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(20)
                .batchMaxBytes(2048)
                .maxMessageAgeMs(3000)
                .parallelUploadThreads(2)
                .enableDynamicBatching(true)
                .queuePressureMonitorIntervalMs(500)
                .highPressureThreshold(0.7)
                .lowPressureThreshold(0.3);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        // 模拟突发日志
        for (int i = 0; i < 50; i++) {
            String logMessage = "突发日志 #" + i + " - " + System.currentTimeMillis();
            engine.put(logMessage.getBytes());
        }

        // 等待压力监控和自适应调整
        Thread.sleep(2000);

        // 等待上传完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        System.out.println("压力自适应测试完成 - 总计上传: " + storageService.getUploadCount() + " 个批次");

        engine.close();
    }
}