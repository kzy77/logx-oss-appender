package org.logx.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;

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
        System.setProperty("logx.oss.storage.endpoint", "https://oss-cn-hangzhou.aliyuncs.com");
        System.setProperty("logx.oss.storage.region", "cn-hangzhou");
        System.setProperty("logx.oss.storage.accessKeyId", "test-access-key");
        System.setProperty("logx.oss.storage.accessKeySecret", "test-secret-key");
        System.setProperty("logx.oss.storage.bucket", "test-bucket");

        // When
        ConfigManager configManager = new ConfigManager();
        LogxOssProperties properties = configManager.getLogxOssProperties();

        // Then
        assertThat(properties.getStorage().getEndpoint()).isEqualTo("https://oss-cn-hangzhou.aliyuncs.com");
        assertThat(properties.getStorage().getRegion()).isEqualTo("cn-hangzhou");
        assertThat(properties.getStorage().getAccessKeyId()).isEqualTo("test-access-key");
        assertThat(properties.getStorage().getAccessKeySecret()).isEqualTo("test-secret-key");
        assertThat(properties.getStorage().getBucket()).isEqualTo("test-bucket");
    }

    @Test
    void testAppenderConfigurationWithDefaultValues() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        LogxOssProperties properties = configManager.getLogxOssProperties();

        // Then
        assertThat(properties.getStorage().getKeyPrefix()).isEqualTo("logx/");
        assertThat(properties.getQueue().getCapacity()).isEqualTo(524288);
        assertThat(properties.getBatch().getCount()).isEqualTo(8192);
        assertThat(properties.getBatch().getBytes()).isEqualTo(10 * 1024 * 1024);
        assertThat(properties.getQueue().isDropWhenFull()).isFalse();
        assertThat(properties.getRetry().getMaxRetries()).isEqualTo(3);
        assertThat(properties.getRetry().getBaseBackoffMs()).isEqualTo(200L);
        assertThat(properties.getRetry().getMaxBackoffMs()).isEqualTo(10000L);
    }

    @Test
    void testAppenderConfigurationWithCustomValues() {
        // Given
        System.setProperty("logx.oss.storage.keyPrefix", "custom-logs/");
        System.setProperty("logx.oss.queue.capacity", "100000");
        System.setProperty("logx.oss.batch.count", "2000");
        System.setProperty("logx.oss.batch.bytes", "2097152");
        System.setProperty("logx.oss.queue.dropWhenFull", "true");
        System.setProperty("logx.oss.retry.maxRetries", "5");
        System.setProperty("logx.oss.retry.baseBackoffMs", "100");
        System.setProperty("logx.oss.retry.maxBackoffMs", "5000");

        // When
        ConfigManager configManager = new ConfigManager();
        LogxOssProperties properties = configManager.getLogxOssProperties();

        // Then
        assertThat(properties.getStorage().getKeyPrefix()).isEqualTo("custom-logs/");
        assertThat(properties.getQueue().getCapacity()).isEqualTo(100000);
        assertThat(properties.getBatch().getCount()).isEqualTo(2000);
        assertThat(properties.getBatch().getBytes()).isEqualTo(2097152);
        assertThat(properties.getQueue().isDropWhenFull()).isTrue();
        assertThat(properties.getRetry().getMaxRetries()).isEqualTo(5);
        assertThat(properties.getRetry().getBaseBackoffMs()).isEqualTo(100L);
        assertThat(properties.getRetry().getMaxBackoffMs()).isEqualTo(5000L);
    }

    @Test
    void testAppenderWithoutEncoder() {
        // Given
        LogbackOSSAppender appender = new LogbackOSSAppender();
        appender.setContext(new LoggerContext());
        appender.setName("TestOSSAppender");

        // When
        appender.start();

        // Then
        // Appender should not start without encoder
        assertThat(appender.isStarted()).isFalse();
    }
}
