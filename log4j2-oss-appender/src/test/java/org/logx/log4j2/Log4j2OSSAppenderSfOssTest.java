package org.logx.log4j2;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class Log4j2OSSAppenderSfOssTest {

    @Test
    void testAppenderFactoryWithSfOssEndpoint() {
        String name = "TestOSSAppender";
        String endpoint = "https://sf-oss-cn-north-1.sf-oss.com";
        String region = "cn-north-1";
        String accessKeyId = "test-access-key";
        String accessKeySecret = "test-secret-key";
        String bucket = "test-bucket";

        Layout<?> layout = PatternLayout.newBuilder().withPattern("%m%n").build();

        Log4j2OSSAppender appender = Log4j2OSSAppender.createAppender(
                name,
                layout,
                null,
                endpoint,
                region,
                accessKeyId,
                accessKeySecret,
                bucket,
                "SF_OSS",
                "logs/",
                "65536",
                "4096",
                "4194304",
                "600000",
                "false",
                "5",
                "200",
                "10000",
                null,
                true
        );

        assertThat(appender).isNotNull();
    }
}
