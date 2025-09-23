package org.logx.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogbackOSSAppender配置测试
 * <p>
 * 验证Logback OSS Appender的配置参数设置和验证功能。
 */
class LogbackOSSAppenderTest {

    @Test
    void testAppenderConfigurationWithRegion() {
        // Given
        LogbackOSSAppender appender = new LogbackOSSAppender();
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        String region = "cn-hangzhou";
        String accessKeyId = "test-access-key";
        String accessKeySecret = "test-secret-key";
        String bucket = "test-bucket";

        // When
        appender.setEndpoint(endpoint);
        appender.setRegion(region);
        appender.setAccessKeyId(accessKeyId);
        appender.setAccessKeySecret(accessKeySecret);
        appender.setBucket(bucket);

        // Then
        assertThat(appender.getEndpoint()).isEqualTo(endpoint);
        assertThat(appender.getRegion()).isEqualTo(region);
        assertThat(appender.getAccessKeyId()).isEqualTo(accessKeyId);
        assertThat(appender.getAccessKeySecret()).isEqualTo(accessKeySecret);
        assertThat(appender.getBucket()).isEqualTo(bucket);
    }

    @Test
    void testAppenderConfigurationWithDefaultValues() {
        // Given
        LogbackOSSAppender appender = new LogbackOSSAppender();

        // Then
        assertThat(appender.getKeyPrefix()).isEqualTo("logs/");
        assertThat(appender.getMaxQueueSize()).isEqualTo(65536);
        assertThat(appender.getMaxBatchCount()).isEqualTo(1000);
        assertThat(appender.getMaxBatchBytes()).isEqualTo(4 * 1024 * 1024);
        assertThat(appender.getFlushIntervalMs()).isEqualTo(2000L);
        assertThat(appender.isDropWhenQueueFull()).isFalse();
        assertThat(appender.isMultiProducer()).isFalse();
        assertThat(appender.getMaxRetries()).isEqualTo(5);
        assertThat(appender.getBaseBackoffMs()).isEqualTo(200L);
        assertThat(appender.getMaxBackoffMs()).isEqualTo(10000L);
    }

    @Test
    void testAppenderConfigurationWithCustomValues() {
        // Given
        LogbackOSSAppender appender = new LogbackOSSAppender();
        String keyPrefix = "custom-logs/";
        int maxQueueSize = 50000;
        int maxBatchCount = 1500;
        int maxBatchBytes = 3 * 1024 * 1024;
        long flushIntervalMs = 3000L;
        boolean dropWhenQueueFull = true;
        boolean multiProducer = true;
        int maxRetries = 4;
        long baseBackoffMs = 150L;
        long maxBackoffMs = 8000L;

        // When
        appender.setKeyPrefix(keyPrefix);
        appender.setMaxQueueSize(maxQueueSize);
        appender.setMaxBatchCount(maxBatchCount);
        appender.setMaxBatchBytes(maxBatchBytes);
        appender.setFlushIntervalMs(flushIntervalMs);
        appender.setDropWhenQueueFull(dropWhenQueueFull);
        appender.setMultiProducer(multiProducer);
        appender.setMaxRetries(maxRetries);
        appender.setBaseBackoffMs(baseBackoffMs);
        appender.setMaxBackoffMs(maxBackoffMs);

        // Then
        assertThat(appender.getKeyPrefix()).isEqualTo(keyPrefix);
        assertThat(appender.getMaxQueueSize()).isEqualTo(maxQueueSize);
        assertThat(appender.getMaxBatchCount()).isEqualTo(maxBatchCount);
        assertThat(appender.getMaxBatchBytes()).isEqualTo(maxBatchBytes);
        assertThat(appender.getFlushIntervalMs()).isEqualTo(flushIntervalMs);
        assertThat(appender.isDropWhenQueueFull()).isTrue();
        assertThat(appender.isMultiProducer()).isTrue();
        assertThat(appender.getMaxRetries()).isEqualTo(maxRetries);
        assertThat(appender.getBaseBackoffMs()).isEqualTo(baseBackoffMs);
        assertThat(appender.getMaxBackoffMs()).isEqualTo(maxBackoffMs);
    }

    @Test
    void testAppenderWithoutEncoder() {
        // Given
        LogbackOSSAppender appender = new LogbackOSSAppender();
        appender.setContext(new LoggerContext());
        appender.setName("TestOSSAppender");
        appender.setAccessKeyId("test-access-key");
        appender.setAccessKeySecret("test-secret-key");
        appender.setBucket("test-bucket");

        // When
        appender.start();

        // Then
        // Appender should not start without encoder
        assertThat(appender.isStarted()).isFalse();
    }
}