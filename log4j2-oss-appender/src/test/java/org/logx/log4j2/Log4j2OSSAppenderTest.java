package org.logx.log4j2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Log4j2OSSAppender配置测试
 * <p>
 * 验证Log4j2 OSS Appender的配置参数设置和验证功能。
 */
class Log4j2OSSAppenderTest {

    @Test
    void testAppenderFactoryWithRegion() {
        // Given
        String name = "TestOSSAppender";
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        String region = "cn-hangzhou";
        String accessKeyId = "test-access-key";
        String accessKeySecret = "test-secret-key";
        String bucket = "test-bucket";

        // When
        Log4j2OSSAppender appender = Log4j2OSSAppender.createAppender(
                name,
                null, // layout
                null, // filter
                endpoint,
                region,
                accessKeyId,
                accessKeySecret,
                bucket,
                true // ignoreExceptions
        );

        // Then
        // Appender should be created successfully (we can't easily test the actual values without reflection)
        assertThat(appender).isNotNull();
    }

    @Test
    void testAppenderFactoryWithoutRequiredParameters() {
        // Given
        String name = "TestOSSAppender";
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        String region = "cn-hangzhou";
        String accessKeyId = null; // Missing required parameter
        String accessKeySecret = "test-secret-key";
        String bucket = "test-bucket";

        // When
        Log4j2OSSAppender appender = Log4j2OSSAppender.createAppender(
                name,
                null, // layout
                null, // filter
                endpoint,
                region,
                accessKeyId,
                accessKeySecret,
                bucket,
                true // ignoreExceptions
        );

        // Then
        // Appender should not be created due to missing required parameter
        assertThat(appender).isNull();
    }
}