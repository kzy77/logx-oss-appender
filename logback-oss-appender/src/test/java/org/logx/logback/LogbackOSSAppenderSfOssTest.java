package org.logx.logback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogbackOSSAppender SF OSS配置测试
 * <p>
 * 验证Logback OSS Appender的SF OSS配置支持。
 */
class LogbackOSSAppenderSfOssTest {

    @Test
    void testAppenderConfigurationWithSfOssEndpoint() {
        // Given
        LogbackOSSAppender appender = new LogbackOSSAppender();
        String endpoint = "https://sf-oss-cn-north-1.sf-oss.com";
        String region = "cn-north-1";
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
}