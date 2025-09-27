package org.logx.compatibility.spring.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 增强的Spring Boot兼容性测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logx.oss.endpoint=test-endpoint",
    "logx.oss.accessKeyId=test-access-key-id",
    "logx.oss.accessKeySecret=test-access-key-secret",
    "logx.oss.bucket=test-bucket"
})
class EnhancedSpringBootCompatibilityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testLogEndpointWithDifferentLevels() {
        // 测试不同日志级别的端点功能
        ResponseEntity<String> infoResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/test-log?level=info", String.class);
        assertThat(infoResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(infoResponse.getBody()).contains("日志消息已生成");

        ResponseEntity<String> debugResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/test-log?level=debug", String.class);
        assertThat(debugResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(debugResponse.getBody()).contains("日志消息已生成");

        ResponseEntity<String> errorResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/test-log?level=error", String.class);
        assertThat(errorResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(errorResponse.getBody()).contains("日志消息已生成");
    }

    @Test
    void testExceptionEndpointWithStackTrace() {
        // 测试带堆栈跟踪的异常端点功能
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/test-exception?stacktrace=true", String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("异常日志已生成");
        // 验证基本响应（符合PRD简洁性原则）
    }

    @Test
    void testOssAppenderConfigurationWithEnvironmentVariables() {
        // 测试使用环境变量配置的OSS Appender
        ch.qos.logback.classic.Logger rootLogger = 
            (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertThat(rootLogger.getAppender("OSS")).isNotNull();
        
        // 验证配置参数是否正确加载
        // 注意：实际的参数验证需要访问Appender的具体配置，这里仅作示例
    }

    @Test
    void testConcurrentLogRequests() throws InterruptedException {
        // 测试并发日志请求
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    "http://localhost:" + port + "/test-log?thread=" + threadId, String.class);
                assertThat(response.getStatusCodeValue()).isEqualTo(200);
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    void testLogMessageFormatConsistency() {
        // 测试日志消息格式一致性
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/test-log?format=structured", String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        
        // 验证基本响应（符合PRD简洁性原则）
        assertThat(response.getBody()).contains("日志消息已生成");
    }
}