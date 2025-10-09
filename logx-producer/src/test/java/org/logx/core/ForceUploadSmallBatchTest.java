package org.logx.core;

import org.junit.jupiter.api.Test;
import org.logx.storage.StorageService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试shutdown时强制上传少量数据（不满足触发条件）
 */
class ForceUploadSmallBatchTest {

    private static class TrackingStorageService implements StorageService {
        private final AtomicInteger uploadCount = new AtomicInteger(0);
        private final CountDownLatch latch;

        TrackingStorageService(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            int count = uploadCount.incrementAndGet();
            System.out.println("📤 强制上传 #" + count + ": " + key +
                             " (数据长度: " + data.length + " bytes, 线程: " + Thread.currentThread().getName() + ")");

            // 解压并查看内容
            try {
                String content = new String(data);
                if (content.contains("少量日志")) {
                    System.out.println("✅ 成功上传少量日志数据!");
                }
            } catch (Exception e) {
                // 可能是压缩数据，忽略
            }

            latch.countDown();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public String getOssType() { return "test"; }

        @Override
        public String getBucketName() { return "test-bucket"; }

        @Override
        public void close() { }

        @Override
        public boolean supportsOssType(String ossType) { return "test".equals(ossType); }

        public int getUploadCount() { return uploadCount.get(); }
    }

    @Test
    void shouldForceUploadSmallBatchOnShutdown() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        TrackingStorageService storageService = new TrackingStorageService(latch);

        // 配置大的批处理阈值，确保正常情况下不会触发
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(1000)  // 很大的批次，不会触发
                .batchMaxBytes(10 * 1024 * 1024) // 10MB，不会触发
                .maxMessageAgeMs(60000)  // 1分钟，测试中不会触发
                .parallelUploadThreads(1)
                .enableDynamicBatching(false);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        // 只发送少量日志，远少于触发条件
        engine.put("少量日志消息1 - 不满足批处理触发条件".getBytes());
        engine.put("少量日志消息2 - 应该在shutdown时强制上传".getBytes());
        engine.put("少量日志消息3 - 测试强制刷新机制".getBytes());

        System.out.println("📝 已发送3条少量日志，等待200ms确认不会自动上传...");
        Thread.sleep(200);

        // 确认在正常情况下没有上传
        assertThat(storageService.getUploadCount())
            .as("在shutdown前不应该有上传发生")
            .isEqualTo(0);

        System.out.println("🔄 开始shutdown，应该强制上传所有剩余数据...");

        // 关闭引擎，应该触发强制上传
        engine.close();

        // 等待强制上传完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);

        System.out.println("📊 测试结果: 完成=" + completed + ", 上传次数=" + storageService.getUploadCount());

        // 验证结果
        assertThat(completed)
            .as("shutdown时应该完成强制上传")
            .isTrue();

        assertThat(storageService.getUploadCount())
            .as("应该有至少1次上传发生（包含所有3条日志）")
            .isGreaterThanOrEqualTo(1);

        System.out.println("✅ 测试通过：少量数据在shutdown时被强制上传");
    }

    @Test
    void shouldForceUploadEvenSingleMessage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        TrackingStorageService storageService = new TrackingStorageService(latch);

        // 极大的阈值，确保单条消息不会触发
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(10000)
                .batchMaxBytes(50 * 1024 * 1024)
                .maxMessageAgeMs(300000)  // 5分钟
                .parallelUploadThreads(1);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        // 只发送1条日志
        engine.put("单条重要日志 - 必须在shutdown时上传".getBytes());

        System.out.println("📝 已发送1条日志，等待100ms确认不会自动上传...");
        Thread.sleep(100);

        assertThat(storageService.getUploadCount()).isEqualTo(0);

        System.out.println("🔄 shutdown单条消息测试...");
        engine.close();

        boolean completed = latch.await(8, TimeUnit.SECONDS);

        System.out.println("📊 单条消息测试结果: 完成=" + completed + ", 上传次数=" + storageService.getUploadCount());

        assertThat(completed)
            .as("单条消息也应该在shutdown时被上传")
            .isTrue();

        assertThat(storageService.getUploadCount())
            .as("应该上传单条消息")
            .isEqualTo(1);

        System.out.println("✅ 单条消息强制上传测试通过");
    }
}