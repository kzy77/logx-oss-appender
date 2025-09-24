package org.logx.compatibility.spring.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot兼容性测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringBootCompatibilityTestApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // 简单的上下文加载测试
    }

    @Test
    void testLogEndpoint() {
        // 测试日志端点功能
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/test-log", String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("日志消息已生成");
    }

    @Test
    void testExceptionEndpoint() {
        // 测试异常端点功能
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/test-exception", String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("异常日志已生成");
    }

    @Test
    void testOssAppenderConfiguration() {
        // 测试OSS Appender配置加载
        // 验证Logback配置是否正确加载
        ch.qos.logback.classic.Logger rootLogger = 
            (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertThat(rootLogger.getAppender("OSS")).isNotNull();
    }
}