package org.logx.compatibility.performance;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.TimeUnit;

/**
 * 跨框架性能一致性测试
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 10)
public class CrossFrameworkPerformanceComparison {

    private static final org.slf4j.Logger logbackLogger = LoggerFactory.getLogger("LogbackLogger");
    private static final org.apache.logging.log4j.Logger log4j2Logger = LogManager.getLogger("Log4j2Logger");
    private static final org.apache.log4j.Logger log4j1Logger = org.apache.log4j.Logger.getLogger("Log4j1Logger");

    @Benchmark
    public void testLogbackInfoLogging(Blackhole blackhole) {
        logbackLogger.info("这是一条Logback INFO级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2InfoLogging(Blackhole blackhole) {
        log4j2Logger.info("这是一条Log4j2 INFO级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1InfoLogging(Blackhole blackhole) {
        log4j1Logger.info("这是一条Log4j 1.x INFO级别的日志消息");
        blackhole.consume(log4j1Logger);
    }

    @Benchmark
    public void testLogbackDebugLogging(Blackhole blackhole) {
        logbackLogger.debug("这是一条Logback DEBUG级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2DebugLogging(Blackhole blackhole) {
        log4j2Logger.debug("这是一条Log4j2 DEBUG级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1DebugLogging(Blackhole blackhole) {
        log4j1Logger.debug("这是一条Log4j 1.x DEBUG级别的日志消息");
        blackhole.consume(log4j1Logger);
    }

    @Benchmark
    public void testLogbackErrorLogging(Blackhole blackhole) {
        logbackLogger.error("这是一条Logback ERROR级别的日志消息");
        blackhole.consume(logbackLogger);
    }

    @Benchmark
    public void testLog4j2ErrorLogging(Blackhole blackhole) {
        log4j2Logger.error("这是一条Log4j2 ERROR级别的日志消息");
        blackhole.consume(log4j2Logger);
    }

    @Benchmark
    public void testLog4j1ErrorLogging(Blackhole blackhole) {
        log4j1Logger.error("这是一条Log4j 1.x ERROR级别的日志消息");
        blackhole.consume(log4j1Logger);
    }
}