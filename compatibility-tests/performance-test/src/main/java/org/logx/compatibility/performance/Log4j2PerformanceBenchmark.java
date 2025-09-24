package org.logx.compatibility.performance;

import org.openjdk.jmh.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Log4j2性能基准测试
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 10)
public class Log4j2PerformanceBenchmark {

    private static final Logger logger = LogManager.getLogger(Log4j2PerformanceBenchmark.class);

    @Benchmark
    public void testInfoLogging() {
        logger.info("这是一条INFO级别的日志消息");
    }

    @Benchmark
    public void testDebugLogging() {
        logger.debug("这是一条DEBUG级别的日志消息");
    }

    @Benchmark
    public void testErrorLogging() {
        logger.error("这是一条ERROR级别的日志消息");
    }
}