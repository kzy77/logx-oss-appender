package examples;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.logx.Log4j2OSSAppender;

/**
 * Log4j2 OSS Appender 编程式配置示例
 *
 * 演示如何通过Java代码配置OSS Appender，适用于：
 * - 动态配置场景
 * - 微服务应用
 * - 需要运行时调整配置的场景
 */
public class ProgrammaticConfiguration {

    public static void main(String[] args) {
        // 使用ConfigurationBuilder API配置OSS Appender
        configureThroughBuilder();
    }

    /**
     * 使用Log4j2的ConfigurationBuilder API配置OSS Appender
     */
    public static void configureThroughBuilder() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        // 设置配置名称
        builder.setConfigurationName("ProgrammaticConfig");

        // 添加Console Appender
        AppenderComponentBuilder consoleBuilder = builder.newAppender("Console", "CONSOLE")
                .addAttribute("target", "SYSTEM_OUT");
        consoleBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"));
        builder.add(consoleBuilder);

        // 添加OSS Appender
        AppenderComponentBuilder ossBuilder = builder.newAppender("OSS", "OSS")
                .addAttribute("endpoint", "https://oss-cn-hangzhou.aliyuncs.com")
                .addAttribute("region", "${sys:LOGX_OSS_REGION:-cn-hangzhou}")
                .addAttribute("accessKeyId", "${env:LOGX_OSS_ACCESS_KEY_ID}")
                .addAttribute("accessKeySecret", "${env:LOGX_OSS_ACCESS_KEY_SECRET}")
                .addAttribute("bucket", "my-app-logs")
                .addAttribute("keyPrefix", "logs/programmatic/");

        ossBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"));
        builder.add(ossBuilder);

        // 配置根Logger
        RootLoggerComponentBuilder rootLogger = builder.newRootLogger("INFO");
        rootLogger.add(builder.newAppenderRef("Console"));
        rootLogger.add(builder.newAppenderRef("OSS"));
        builder.add(rootLogger);

        // 应用配置
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = builder.build();
        context.start(config);

        // 测试日志
        Logger logger = LogManager.getLogger(ProgrammaticConfiguration.class);
        logger.info("程序化配置测试 - 使用ConfigurationBuilder API");
        logger.warn("这是一条警告日志");
        logger.error("这是一条错误日志");
    }
}