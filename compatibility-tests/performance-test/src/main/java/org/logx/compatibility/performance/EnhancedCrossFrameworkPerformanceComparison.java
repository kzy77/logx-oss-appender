package org.logx.compatibility.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.log4j.BasicConfigurator;

import java.util.concurrent.TimeUnit;

/**
 * 增强的跨框架性能一致性测试
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(0) // 在测试环境中不进行fork
@Warmup(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 2, time = 200, timeUnit = TimeUnit.MILLISECONDS)
public class EnhancedCrossFrameworkPerformanceComparison {

    private static final org.slf4j.Logger logbackLogger = LoggerFactory.getLogger("EnhancedLogbackLogger");
    private static final org.apache.logging.log4j.Logger log4j2Logger = LogManager.getLogger("EnhancedLog4j2Logger");
    private static final org.apache.log4j.Logger log4j1Logger = org.apache.log4j.Logger.getLogger("EnhancedLog4j1Logger");

    static {
        // 初始化Log4j1配置
        BasicConfigurator.configure();
    }

    @Benchmark
    public void testLogbackInfoLogging(Blackhole blackhole) {
        logbackLogger.info("这是一条增强的Logback INFO级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2InfoLogging(Blackhole blackhole) {
        log4j2Logger.info("这是一条增强的Log4j2 INFO级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1InfoLogging(Blackhole blackhole) {
        log4j1Logger.info("这是一条增强的Log4j 1.x INFO级别的日志消息");
        blackhole.consume(log4j1Logger);
    }

    @Benchmark
    public void testLogbackDebugLogging(Blackhole blackhole) {
        logbackLogger.debug("这是一条增强的Logback DEBUG级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2DebugLogging(Blackhole blackhole) {
        log4j2Logger.debug("这是一条增强的Log4j2 DEBUG级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1DebugLogging(Blackhole blackhole) {
        log4j1Logger.debug("这是一条增强的Log4j 1.x DEBUG级别的日志消息");
        blackhole.consume(log4j1Logger);
    }

    @Benchmark
    public void testLogbackErrorLogging(Blackhole blackhole) {
        logbackLogger.error("这是一条增强的Logback ERROR级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2ErrorLogging(Blackhole blackhole) {
        log4j2Logger.error("这是一条增强的Log4j2 ERROR级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1ErrorLogging(Blackhole blackhole) {
        log4j1Logger.error("这是一条增强的Log4j 1.x ERROR级别的日志消息");
        blackhole.consume(log4j1Logger);
    }

    @Benchmark
    public void testLogbackWarnLogging(Blackhole blackhole) {
        logbackLogger.warn("这是一条增强的Logback WARN级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2WarnLogging(Blackhole blackhole) {
        log4j2Logger.warn("这是一条增强的Log4j2 WARN级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1WarnLogging(Blackhole blackhole) {
        log4j1Logger.warn("这是一条增强的Log4j 1.x WARN级别的日志消息");
        blackhole.consume(log4j1Logger);
    }

    @Benchmark
    public void testLogbackTraceLogging(Blackhole blackhole) {
        logbackLogger.trace("这是一条增强的Logback TRACE级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2TraceLogging(Blackhole blackhole) {
        log4j2Logger.trace("这是一条增强的Log4j2 TRACE级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1TraceLogging(Blackhole blackhole) {
        log4j1Logger.trace("这是一条增强的Log4j 1.x TRACE级别的日志消息");
        blackhole.consume(log4j1Logger);
    }

    // 增加带参数的日志记录测试

    @Benchmark
    public void testLogbackParameterizedLogging(Blackhole blackhole) {
        String user = "testUser";
        int userId = 12345;
        logbackLogger.info("用户 {} (ID: {}) 执行了操作", user, userId);
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2ParameterizedLogging(Blackhole blackhole) {
        String user = "testUser";
        int userId = 12345;
        log4j2Logger.info("用户 {} (ID: {}) 执行了操作", user, userId);
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1ParameterizedLogging(Blackhole blackhole) {
        String user = "testUser";
        int userId = 12345;
        log4j1Logger.info("用户 " + user + " (ID: " + userId + ") 执行了操作");
        blackhole.consume(log4j1Logger);
    }

    // 增加异常日志记录测试

    @Benchmark
    public void testLogbackExceptionLogging(Blackhole blackhole) {
        try {
            throw new RuntimeException("测试异常");
        } catch (Exception e) {
            logbackLogger.error("处理请求时发生异常", e);
        }
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2ExceptionLogging(Blackhole blackhole) {
        try {
            throw new RuntimeException("测试异常");
        } catch (Exception e) {
            log4j2Logger.error("处理请求时发生异常", e);
        }
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1ExceptionLogging(Blackhole blackhole) {
        try {
            throw new RuntimeException("测试异常");
        } catch (Exception e) {
            log4j1Logger.error("处理请求时发生异常", e);
        }
        blackhole.consume(log4j1Logger);
    }
}