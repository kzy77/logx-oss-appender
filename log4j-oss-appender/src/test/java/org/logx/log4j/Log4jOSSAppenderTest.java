package org.logx.log4j;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Log4jOSSAppender配置测试
 * <p>
 * 验证Log4j 1.x OSS Appender的配置参数设置和验证功能。
 */
class Log4jOSSAppenderTest {

    @Test
    void testAppenderConfigurationWithRegion() {
        // Given
        Log4jOSSAppender appender = new Log4jOSSAppender();
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
        Log4jOSSAppender appender = new Log4jOSSAppender();

        // Then
        assertThat(appender.getKeyPrefix()).isEqualTo("logs/");
        assertThat(appender.getMaxQueueSize()).isEqualTo(65536);
        assertThat(appender.getMaxBatchCount()).isEqualTo(4096);
        assertThat(appender.getMaxBatchBytes()).isEqualTo(10 * 1024 * 1024);
        assertThat(appender.getMaxMessageAgeMs()).isEqualTo(600000L);
        assertThat(appender.isDropWhenQueueFull()).isFalse();
        assertThat(appender.isMultiProducer()).isFalse();
        assertThat(appender.getMaxRetries()).isEqualTo(3);
        assertThat(appender.getBaseBackoffMs()).isEqualTo(200L);
        assertThat(appender.getMaxBackoffMs()).isEqualTo(10000L);
    }

    @Test
    void testAppenderConfigurationWithCustomValues() {
        // Given
        Log4jOSSAppender appender = new Log4jOSSAppender();
        String keyPrefix = "custom-logs/";
        int maxQueueSize = 100000;
        int maxBatchCount = 2000;
        int maxBatchBytes = 2 * 1024 * 1024;
        long maxMessageAgeMs = 300000L;
        boolean dropWhenQueueFull = true;
        boolean multiProducer = true;
        int maxRetries = 3;
        long baseBackoffMs = 100L;
        long maxBackoffMs = 5000L;

        // When
        appender.setKeyPrefix(keyPrefix);
        appender.setMaxQueueSize(maxQueueSize);
        appender.setMaxBatchCount(maxBatchCount);
        appender.setMaxBatchBytes(maxBatchBytes);
        appender.setMaxMessageAgeMs(maxMessageAgeMs);
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
        assertThat(appender.getMaxMessageAgeMs()).isEqualTo(maxMessageAgeMs);
        assertThat(appender.isDropWhenQueueFull()).isTrue();
        assertThat(appender.isMultiProducer()).isTrue();
        assertThat(appender.getMaxRetries()).isEqualTo(maxRetries);
        assertThat(appender.getBaseBackoffMs()).isEqualTo(baseBackoffMs);
        assertThat(appender.getMaxBackoffMs()).isEqualTo(maxBackoffMs);
    }
}