package org.logx.core;

import org.junit.jupiter.api.Test;
import org.logx.storage.ProtocolType;
import org.logx.storage.StorageService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试batchMaxMessages默认值为8192的行为
 */
class BatchMaxMessagesTest {

    private static class CountingStorageService implements StorageService {
        private final AtomicInteger uploadCount = new AtomicInteger(0);
        private final CountDownLatch latch;

        CountingStorageService(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            int count = uploadCount.incrementAndGet();
            System.out.println("📊 消息数量触发上传 #" + count + ": " + key +
                             " (数据长度: " + data.length + " bytes, 线程: " + Thread.currentThread().getName() + ")");
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
    void shouldUse8192DefaultBatchMaxMessages() throws Exception {
        // 验证默认配置
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig();

        assertThat(config.getBatchMaxMessages())
            .as("默认batchMaxMessages应该是8192条")
            .isEqualTo(8192);

        System.out.println("✅ 验证通过：默认batchMaxMessages = " + config.getBatchMaxMessages() + "条 (8192条)");
    }

    @Test
    void shouldTriggerUploadAt8192Messages() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountingStorageService storageService = new CountingStorageService(latch);

        // 使用默认配置，但调整其他阈值防止意外触发
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                // batchMaxMessages使用默认值8192条
                .batchMaxBytes(100 * 1024 * 1024)  // 100MB，不会触发
                .maxMessageAgeMs(300000)  // 5分钟，不会触发
                .parallelUploadThreads(1);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        System.out.println("📝 开始发送8192条消息，应该刚好触发批处理...");

        // 发送刚好8192条消息
        for (int i = 0; i < 8192; i++) {
            String logMessage = "测试消息 #" + i;
            engine.put(logMessage.getBytes());

            // 每1000条打印进度
            if ((i + 1) % 1000 == 0) {
                System.out.println("📝 已发送 " + (i + 1) + " 条消息...");
            }
        }

        System.out.println("📝 已发送8192条消息，等待批处理触发...");

        // 等待上传完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);

        System.out.println("📊 测试结果: 完成=" + completed + ", 上传次数=" + storageService.getUploadCount());

        assertThat(completed)
            .as("应该在8192条消息时触发上传")
            .isTrue();

        assertThat(storageService.getUploadCount())
            .as("应该有1次消息数量触发的上传")
            .isEqualTo(1);

        engine.close();
        System.out.println("✅ 8192条消息触发测试通过");
    }

    @Test
    void shouldCustomizeBatchMaxMessages() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountingStorageService storageService = new CountingStorageService(latch);

        // 自定义设置为100条进行快速测试
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(100)  // 自定义100条
                .batchMaxBytes(100 * 1024 * 1024)
                .maxMessageAgeMs(300000)
                .parallelUploadThreads(1);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        System.out.println("📝 自定义100条测试，发送100条消息...");

        // 发送100条消息
        for (int i = 0; i < 100; i++) {
            String logMessage = "自定义测试消息 #" + i;
            engine.put(logMessage.getBytes());
        }

        // 等待上传完成
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        System.out.println("📊 自定义100条测试结果: 完成=" + completed + ", 上传次数=" + storageService.getUploadCount());

        assertThat(completed)
            .as("应该在100条消息时触发上传")
            .isTrue();

        engine.close();
        System.out.println("✅ 自定义100条触发测试通过");
    }

    @Test
    void shouldNotTriggerBelow8192Messages() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountingStorageService storageService = new CountingStorageService(latch);

        // 使用默认配置
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxBytes(100 * 1024 * 1024)
                .maxMessageAgeMs(300000)
                .parallelUploadThreads(1);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        System.out.println("📝 发送8191条消息，应该不会触发批处理...");

        // 发送8191条消息（少1条）
        for (int i = 0; i < 8191; i++) {
            String logMessage = "少量测试消息 #" + i;
            engine.put(logMessage.getBytes());
        }

        System.out.println("📝 已发送8191条消息，等待1秒确认不会触发...");

        // 等待一短时间，应该不会触发
        boolean completed = latch.await(1, TimeUnit.SECONDS);

        System.out.println("📊 8191条测试结果: 完成=" + completed + ", 上传次数=" + storageService.getUploadCount());

        assertThat(completed)
            .as("8191条消息不应该触发上传")
            .isFalse();

        assertThat(storageService.getUploadCount())
            .as("不应该有上传发生")
            .isEqualTo(0);

        engine.close();
        System.out.println("✅ 8191条不触发测试通过");
    }
}