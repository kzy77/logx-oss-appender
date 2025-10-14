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
 * 测试shutdown时的日志上传行为
 */
class ShutdownTest {

    // 记录上传次数的存储服务实现
    private static class CountingStorageService implements StorageService {
        private final AtomicInteger uploadCount = new AtomicInteger(0);
        private final CountDownLatch latch;

        CountingStorageService(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            uploadCount.incrementAndGet();
            System.out.println("上传日志: " + key + ", 数据长度: " + data.length + ", 总计上传次数: " + uploadCount.get());
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
            // 空实现
        }

        @Override
        public boolean supportsProtocol(ProtocolType protocol) {
            return protocol == ProtocolType.S3;
        }

        public int getUploadCount() {
            return uploadCount.get();
        }
    }

    @Test
    void shouldUploadAllPendingLogsOnShutdown() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountingStorageService storageService = new CountingStorageService(latch);

        // 创建AsyncEngine
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(100)  // 设置较大的批次，确保不会自动触发
                .batchMaxBytes(1024 * 1024)
                .maxMessageAgeMs(60_000);

        AsyncEngineImpl engine = new AsyncEngineImpl(storageService, config);
        engine.start();

        // 添加一些日志
        engine.put("日志消息1".getBytes());
        engine.put("日志消息2".getBytes());
        engine.put("日志消息3".getBytes());

        // 等待短暂时间确认不会自动上传
        Thread.sleep(100);
        assertThat(storageService.getUploadCount()).isEqualTo(0);

        // 关闭引擎，应该触发强制上传
        engine.close();

        // 等待上传完成
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(storageService.getUploadCount()).isGreaterThan(0);

        System.out.println("测试完成，共上传了 " + storageService.getUploadCount() + " 次");
    }
}