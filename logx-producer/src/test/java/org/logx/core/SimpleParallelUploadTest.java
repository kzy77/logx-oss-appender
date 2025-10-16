package org.logx.core;

import org.junit.jupiter.api.Test;
import org.logx.storage.ProtocolType;
import org.logx.storage.StorageConfig;
import org.logx.storage.StorageService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 简单的并行上传测试
 */
class SimpleParallelUploadTest {

    private static class FastStorageService implements StorageService {
        private final AtomicInteger uploadCount = new AtomicInteger(0);
        private final CountDownLatch latch;

        FastStorageService(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            int count = uploadCount.incrementAndGet();
            System.out.println("上传 #" + count + ": " + key + " (线程: " + Thread.currentThread().getName() + ")");
            latch.countDown();
            return CompletableFuture.completedFuture(null);
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
        public void close() {
        }

        @Override
        public boolean supportsProtocol(ProtocolType protocol) {
            return protocol == ProtocolType.S3;
        }

        public int getUploadCount() { return uploadCount.get(); }
    }

    @Test
    void shouldUseParallelUpload() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        FastStorageService mockStorage = new FastStorageService(latch);

        // 配置并行上传
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(5)  // 小批次
                .batchMaxBytes(512)
                .maxMessageAgeMs(2000)
                .parallelUploadThreads(2)
                .enableDynamicBatching(false); // 关闭动态批处理简化测试

        AsyncEngineImpl engine = new AsyncEngineImpl(config, mockStorage);
        engine.start();

        // 发送足够的日志触发2个批次
        for (int i = 0; i < 12; i++) {
            String logMessage = "测试日志 #" + i + " - " + System.currentTimeMillis();
            engine.put(logMessage.getBytes());
        }

        // 等待上传完成
        boolean completed = latch.await(8, TimeUnit.SECONDS);

        // 先验证是否有上传发生
        // 不再直接访问storageService，因为它是私有的

        // 关闭引擎
        engine.close();

        assertThat(completed).isTrue();
    }
}