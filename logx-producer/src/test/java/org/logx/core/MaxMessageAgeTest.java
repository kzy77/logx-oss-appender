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
 * 测试maxMessageAgeMs默认值为1分钟的行为
 */
class MaxMessageAgeTest {

    private static class TimeTriggeredStorageService implements StorageService {
        private final AtomicInteger uploadCount = new AtomicInteger(0);
        private final CountDownLatch latch;

        TimeTriggeredStorageService(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public CompletableFuture<Void> putObject(String key, byte[] data) {
            int count = uploadCount.incrementAndGet();
            System.out.println("⏰ 时间触发上传 #" + count + ": " + key +
                             " (数据长度: " + data.length + " bytes, 时间: " +
                             java.time.LocalTime.now() + ")");
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
    void shouldUseOneMinuteDefaultMaxMessageAge() throws Exception {
        // 验证默认配置
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig();

        assertThat(config.getMaxMessageAgeMs())
            .as("默认maxMessageAgeMs应该是1分钟(60000毫秒)")
            .isEqualTo(60000L);

        System.out.println("✅ 验证通过：默认maxMessageAgeMs = " + config.getMaxMessageAgeMs() + "ms (1分钟)");
    }

    @Test
    void shouldTriggerUploadAfterOneMinute() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        TimeTriggeredStorageService mockStorage = new TimeTriggeredStorageService(latch);

        // 使用默认配置，但调整其他阈值防止意外触发
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(1000)  // 大批次数，不会触发
                .batchMaxBytes(10 * 1024 * 1024)  // 10MB，不会触发
                // maxMessageAgeMs使用默认值60000ms(1分钟)
                .parallelUploadThreads(1);

        AsyncEngineImpl engine = new AsyncEngineImpl(config, mockStorage);
        engine.start();

        System.out.println("📝 发送少量日志，等待1分钟触发时间条件...");
        System.out.println("⏰ 开始时间: " + java.time.LocalTime.now());

        // 发送少量日志，不会触发数量或大小条件
        engine.put("测试日志1 - 等待时间触发".getBytes());
        engine.put("测试日志2 - 1分钟后应该上传".getBytes());

        // 等待稍微超过1分钟的时间
        boolean completed = latch.await(70, TimeUnit.SECONDS);

        System.out.println("⏰ 结束时间: " + java.time.LocalTime.now());

        assertThat(completed)
            .as("应该在1分钟后触发时间条件上传")
            .isTrue();

        engine.close();
        System.out.println("✅ 1分钟时间触发测试通过");
    }

    @Test
    void shouldCustomizeMaxMessageAge() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        TimeTriggeredStorageService mockStorage = new TimeTriggeredStorageService(latch);

        // 自定义设置为5秒进行快速测试
        AsyncEngineConfig config = AsyncEngineConfig.defaultConfig()
                .batchMaxMessages(1000)
                .batchMaxBytes(10 * 1024 * 1024)
                .maxMessageAgeMs(5000)  // 5秒
                .parallelUploadThreads(1);

        AsyncEngineImpl engine = new AsyncEngineImpl(config, mockStorage);
        engine.start();

        System.out.println("📝 自定义5秒测试 - 开始时间: " + java.time.LocalTime.now());

        engine.put("5秒测试日志".getBytes());

        // 等待6秒
        boolean completed = latch.await(8, TimeUnit.SECONDS);

        System.out.println("⏰ 5秒测试结束时间: " + java.time.LocalTime.now());

        assertThat(completed)
            .as("应该在5秒后触发上传")
            .isTrue();

        engine.close();
        System.out.println("✅ 自定义5秒触发测试通过");
    }
}