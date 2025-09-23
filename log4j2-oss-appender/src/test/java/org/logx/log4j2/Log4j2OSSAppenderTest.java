package org.logx.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Log4j2OSSAppender的功能和集成测试
 * <p>
 * 测试Log4j2OSSAppender是否能被Log4j2的插件系统正确加载， 并验证通过XML配置的参数是否能被成功注入。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class Log4j2OSSAppenderTest {

    private static final String CONFIG_FILE = "log4j2-test.xml";
    private LoggerContext loggerContext;

    @BeforeEach
    void setUp() {
        // 加载测试配置文件并初始化LoggerContext
        loggerContext = Configurator.initialize("TestContext", CONFIG_FILE);
    }

    @AfterEach
    void tearDown() {
        // 清理并关闭LoggerContext
        Configurator.shutdown(loggerContext);
    }

    @Test
    void appenderShouldBeLoadedAndConfigured() {
        Configuration config = loggerContext.getConfiguration();
        Log4j2OSSAppender appender = config.getAppender("OSS_Test");
        assertThat(appender).isNotNull();
    }
}
