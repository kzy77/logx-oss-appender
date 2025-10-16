package org.logx.log4j;

import org.junit.jupiter.api.Test;
import org.logx.config.ConfigManager;
import org.logx.config.properties.LogxOssProperties;

import static org.assertj.core.api.Assertions.*;

/**
 * Log4jOSSAppender SF OSS配置测试
 * <p>
 * 验证Log4j 1.x OSS Appender的SF OSS配置支持。
 */
class Log4jOSSAppenderSfOssTest {

    @Test
    void testAppenderConfigurationWithSfOssEndpoint() {
        // Given
        System.setProperty("logx.oss.storage.endpoint", "https://sf-oss-cn-north-1.sf-oss.com");
        System.setProperty("logx.oss.storage.region", "cn-north-1");
        System.setProperty("logx.oss.storage.accessKeyId", "test-access-key");
        System.setProperty("logx.oss.storage.accessKeySecret", "test-secret-key");
        System.setProperty("logx.oss.storage.bucket", "test-bucket");

        // When
        ConfigManager configManager = new ConfigManager();
        LogxOssProperties properties = configManager.getLogxOssProperties();

        // Then
        assertThat(properties.getStorage().getEndpoint()).isEqualTo("https://sf-oss-cn-north-1.sf-oss.com");
        assertThat(properties.getStorage().getRegion()).isEqualTo("cn-north-1");
        assertThat(properties.getStorage().getAccessKeyId()).isEqualTo("test-access-key");
        assertThat(properties.getStorage().getAccessKeySecret()).isEqualTo("test-secret-key");
        assertThat(properties.getStorage().getBucket()).isEqualTo("test-bucket");
    }
}
