package org.logx.log4j2;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Log4j2OSSAppender SF OSS配置测试
 * <p>
 * 验证Log4j2 OSS Appender的SF OSS配置支持。
 */
class Log4j2OSSAppenderSfOssTest {

    @Test
    void testAppenderFactoryWithSfOssEndpoint() {
        // Given
        String name = "TestOSSAppender";
        String endpoint = "https://sf-oss-cn-north-1.sf-oss.com";
        String region = "cn-north-1";
        String accessKeyId = "test-access-key";
        String accessKeySecret = "test-secret-key";
        String bucket = "test-bucket";
        
        // 创建一个简单的layout
        Layout<?> layout = PatternLayout.newBuilder().withPattern("%m%n").build();

        // When
        Log4j2OSSAppender appender = Log4j2OSSAppender.createAppender(
                name,
                layout, // layout
                null, // filter
                endpoint,
                region,
                accessKeyId,
                accessKeySecret,
                bucket,
                "SF_OSS", // backendType
                true // ignoreExceptions
        );

        // Then
        // Appender should be created successfully (we can't easily test the actual values without reflection)
        assertThat(appender).isNotNull();
    }
}