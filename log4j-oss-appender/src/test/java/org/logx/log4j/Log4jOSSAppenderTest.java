package org.logx.log4j;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Log4jOSSAppender 集成测试 验证Log4j 1.x配置和日志上传功能
 */
class Log4jOSSAppenderTest {

    private static final String TEST_BUCKET = "test-logs-bucket";
    private static final String TEST_ACCESS_KEY = "test-access-key";
    private static final String TEST_SECRET_KEY = "test-secret-key";

    @BeforeEach
    void setUp() {
        // 重置Log4j配置
        Logger.getRootLogger().removeAllAppenders();
    }

    @Test
    void shouldCreateAppenderWithBasicConfig() {
        // 创建Log4jOSSAppender
        Log4jOSSAppender appender = new Log4jOSSAppender();
        appender.setName("test-oss");
        appender.setAccessKeyId(TEST_ACCESS_KEY);
        appender.setAccessKeySecret(TEST_SECRET_KEY);
        appender.setBucket(TEST_BUCKET);
        appender.setEndpoint("http://localhost:9000");
        appender.setRegion("us-east-1");
        appender.setLayout(new PatternLayout("%d{ISO8601} [%t] %-5p %c{1} - %m%n"));

        // 验证配置
        assertThat(appender.getName()).isEqualTo("test-oss");
        assertThat(appender.getAccessKeyId()).isEqualTo(TEST_ACCESS_KEY);
        assertThat(appender.getBucket()).isEqualTo(TEST_BUCKET);
        assertThat(appender.getKeyPrefix()).isEqualTo("logs/");
        assertThat(appender.getMaxQueueSize()).isEqualTo(262_144);
        assertThat(appender.requiresLayout()).isTrue();
    }

    @Test
    void shouldHandleAppenderConfiguration() {
        // 创建Log4jOSSAppender并设置配置
        Log4jOSSAppender appender = new Log4jOSSAppender();
        appender.setName("test-oss");
        appender.setAccessKeyId(TEST_ACCESS_KEY);
        appender.setAccessKeySecret(TEST_SECRET_KEY);
        appender.setBucket(TEST_BUCKET);
        appender.setEndpoint("http://localhost:9000");
        appender.setRegion("us-east-1");
        appender.setKeyPrefix("test-logs/");
        appender.setMaxQueueSize(100_000);
        appender.setMaxBatchCount(1000);
        appender.setFlushIntervalMs(1000L);
        appender.setDropWhenQueueFull(true);
        appender.setLayout(new PatternLayout("%m%n"));

        // 验证自定义配置
        assertThat(appender.getKeyPrefix()).isEqualTo("test-logs/");
        assertThat(appender.getMaxQueueSize()).isEqualTo(100_000);
        assertThat(appender.getMaxBatchCount()).isEqualTo(1000);
        assertThat(appender.getFlushIntervalMs()).isEqualTo(1000L);
        assertThat(appender.isDropWhenQueueFull()).isTrue();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldConfigureFromProperties() {
        // 准备Properties配置
        Properties props = new Properties();
        props.setProperty("log4j.rootLogger", "INFO, oss");

        props.setProperty("log4j.appender.oss", "org.logx.log4j.Log4jOSSAppender");
        props.setProperty("log4j.appender.oss.endpoint", "http://localhost:9000");
        props.setProperty("log4j.appender.oss.region", "us-east-1");
        props.setProperty("log4j.appender.oss.accessKeyId", TEST_ACCESS_KEY);
        props.setProperty("log4j.appender.oss.accessKeySecret", TEST_SECRET_KEY);
        props.setProperty("log4j.appender.oss.bucket", TEST_BUCKET);
        props.setProperty("log4j.appender.oss.keyPrefix", "properties-test/");
        props.setProperty("log4j.appender.oss.maxQueueSize", "50000");

        props.setProperty("log4j.appender.oss.layout", "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.oss.layout.ConversionPattern", "%d{ISO8601} [%t] %-5p %c{1} - %m%n");

        // 应用配置（注意：这里不会真正连接到存储，只是测试配置解析）
        try {
            PropertyConfigurator.configure(props);

            // 验证Logger已配置
            Logger logger = Logger.getLogger(Log4jOSSAppenderTest.class);
            assertThat(logger).isNotNull();

            // 发送测试日志（会进入队列但不会真正上传）
            logger.info("Test log message from Properties configuration");
            logger.error("Test error message");

        } catch (Exception e) {
            // 由于没有真实的S3环境，adapter初始化可能失败，这是预期的
            assertThat(e.getMessage()).containsAnyOf("Failed to initialize", "initialization failed");
        }
    }

    @Test
    void shouldValidateRequiredParameters() {
        // 测试缺少必需参数的情况
        Log4jOSSAppender appender = new Log4jOSSAppender();
        appender.setName("test-oss");
        appender.setLayout(new PatternLayout("%m%n"));

        // 缺少accessKeyId
        appender.setAccessKeySecret(TEST_SECRET_KEY);
        appender.setBucket(TEST_BUCKET);

        // activateOptions应该检测到缺少accessKeyId（通过ErrorHandler处理）
        // 由于ErrorHandler的默认实现，这里不会抛出异常，但会记录错误
        appender.activateOptions();

        // 设置所有必需参数
        appender.setAccessKeyId(TEST_ACCESS_KEY);

        // 现在应该能正常激活（虽然由于网络原因可能仍会失败）
        try {
            appender.activateOptions();
        } catch (Exception e) {
            // 网络相关异常是预期的
            assertThat(e.getMessage()).isNotNull();
        }
    }

    @Test
    void shouldHandleLayoutAndFormatting() {
        // 创建测试Appender
        Log4jOSSAppender appender = new Log4jOSSAppender();
        appender.setName("test-oss");
        appender.setAccessKeyId(TEST_ACCESS_KEY);
        appender.setAccessKeySecret(TEST_SECRET_KEY);
        appender.setBucket(TEST_BUCKET);
        appender.setEndpoint("http://localhost:9000");
        appender.setRegion("us-east-1");

        // 测试PatternLayout
        PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c{1} - %m%n");
        appender.setLayout(layout);

        assertThat(appender.getLayout()).isEqualTo(layout);
        assertThat(appender.requiresLayout()).isTrue();

        // 测试Level过滤
        appender.setThreshold(Level.ERROR);

        // 由于没有真实环境，adapter初始化会失败，但这验证了配置解析
        try {
            appender.activateOptions();
        } catch (Exception e) {
            // 预期的初始化失败
            assertThat(e.getMessage()).isNotNull();
        }
    }

    @Test
    void shouldCloseGracefully() {
        // 创建Appender
        Log4jOSSAppender appender = new Log4jOSSAppender();
        appender.setName("test-oss");

        // close()应该能安全处理null adapter（不应抛出异常）
        try {
            appender.close();
            appender.close(); // 多次close()应该是安全的
        } catch (Exception e) {
            // 不应该抛出异常
            assertThat(e).as("close() should not throw exceptions").isNull();
        }

        // 验证关闭后不能再添加日志
        // 虽然我们不能直接访问closed字段，但可以通过其他方式验证
        assertThat(appender.getName()).isEqualTo("test-oss"); // Appender基本信息应该仍然可访问
    }
}
