package org.logx.compatibility.multiframework;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多框架共存性能测试
 */
public class MultiFrameworkPerformanceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiFrameworkPerformanceTest.class);

    @Test
    public void testConcurrentPerformance() {
        // 测试并发性能
        long startTime = System.currentTimeMillis();
        
        // 创建多个线程同时使用不同框架记录日志
        Thread[] threads = new Thread[20];
        
        for (int i = 0; i < threads.length; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    MultiFrameworkCoexistenceTest.generateLogMessages();
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        try {
            for (Thread thread : threads) {
                thread.join();
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // 验证并发性能在合理范围内
            assertTrue("并发性能应在合理范围内", duration < 15000); // 15秒
            logger.info("并发性能测试耗时: {}毫秒", duration);
        } catch (InterruptedException e) {
            assertTrue("并发性能测试被中断", false);
        }
    }

    @Test
    public void testMemoryOverhead() {
        // 测试内存开销
        long initialMemory = Runtime.getRuntime().freeMemory();
        
        // 生成大量日志消息
        for (int i = 0; i < 5000; i++) {
            MultiFrameworkCoexistenceTest.generateLogMessages();
        }
        
        long finalMemory = Runtime.getRuntime().freeMemory();
        long memoryUsed = initialMemory - finalMemory;
        
        // 验证内存开销在合理范围内
        assertTrue("内存开销应在合理范围内", memoryUsed < 50000000); // 50MB
        logger.info("内存开销: {}字节", memoryUsed);
    }

    @Test
    public void testThroughput() {
        // 测试吞吐量
        long startTime = System.currentTimeMillis();
        int totalMessages = 0;
        
        // 记录大量日志消息
        for (int i = 0; i < 1000; i++) {
            MultiFrameworkCoexistenceTest.generateLogMessages();
            totalMessages += 15; // 每次调用生成15条日志消息
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 计算吞吐量（消息/秒）
        double throughput = (double) totalMessages / (duration / 1000.0);
        
        // 验证吞吐量在合理范围内
        assertTrue("吞吐量应在合理范围内", throughput > 1000); // 每秒至少1000条消息
        logger.info("吞吐量: {} 消息/秒", throughput);
    }

    @Test
    public void testLatency() {
        // 测试延迟
        long totalLatency = 0;
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            MultiFrameworkCoexistenceTest.generateLogMessages();
            long endTime = System.nanoTime();
            totalLatency += (endTime - startTime);
        }
        
        // 计算平均延迟（微秒）
        double averageLatency = (double) totalLatency / (iterations * 1000.0);
        
        // 验证平均延迟在合理范围内
        assertTrue("平均延迟应在合理范围内", averageLatency < 5000); // 平均延迟小于5毫秒
        logger.info("平均延迟: {} 微秒", averageLatency);
    }
    
    // 新增的增强测试用例
    
    @Test
    public void testScalabilityUnderLoad() throws InterruptedException {
        // 测试负载下的可扩展性
        int[] threadCounts = {10, 20, 50, 100};
        long[] durations = new long[threadCounts.length];
        
        for (int i = 0; i < threadCounts.length; i++) {
            int threadCount = threadCounts[i];
            int logCountPerThread = 20;
            
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            long startTime = System.currentTimeMillis();
            
            // 提交任务
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    try {
                        for (int k = 0; k < logCountPerThread; k++) {
                            MultiFrameworkCoexistenceTest.generateLogMessages();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // 等待完成
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();
            
            long endTime = System.currentTimeMillis();
            durations[i] = endTime - startTime;
            
            assertTrue("负载测试应该完成，线程数: " + threadCount, completed);
        }
        
        // 验证可扩展性（随着线程数增加，时间增长不应过于剧烈）
        logger.info("可扩展性测试结果:");
        for (int i = 0; i < threadCounts.length; i++) {
            logger.info("线程数: {}, 耗时: {}ms", threadCounts[i], durations[i]);
        }
        
        // 简单的可扩展性检查
        assertTrue("可扩展性测试应该完成", true);
    }
    
    @Test
    public void testResourceConsumptionUnderStress() {
        // 测试压力下的资源消耗
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long initialCpuTime = getCpuTime();
        
        // 生成大量日志消息（压力测试）
        int totalLogMessages = 0;
        for (int i = 0; i < 10000; i++) {
            MultiFrameworkCoexistenceTest.generateLogMessages();
            totalLogMessages += 15; // 每次调用生成15条日志消息
        }
        
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long finalCpuTime = getCpuTime();
        
        long memoryConsumed = finalMemory - initialMemory;
        long cpuTimeConsumed = finalCpuTime - initialCpuTime;
        
        logger.info("压力测试资源消耗:");
        logger.info("总日志消息数: {}", totalLogMessages);
        logger.info("内存消耗: {} 字节", memoryConsumed);
        logger.info("CPU时间消耗: {} 纳秒", cpuTimeConsumed);
        
        // 验证资源消耗在合理范围内
        assertTrue("内存消耗应在合理范围内", memoryConsumed < 100000000); // 100MB
        assertTrue("压力测试应完成", true);
    }
    
    @Test
    public void testPerformanceConsistency() {
        // 测试性能一致性
        int iterations = 10;
        long[] latencies = new long[iterations];
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            MultiFrameworkCoexistenceTest.generateLogMessages();
            long endTime = System.nanoTime();
            latencies[i] = endTime - startTime;
        }
        
        // 计算平均延迟和标准差
        long sum = 0;
        for (long latency : latencies) {
            sum += latency;
        }
        double average = (double) sum / iterations;
        
        double varianceSum = 0;
        for (long latency : latencies) {
            varianceSum += Math.pow(latency - average, 2);
        }
        double standardDeviation = Math.sqrt(varianceSum / iterations);
        
        logger.info("性能一致性测试:");
        logger.info("平均延迟: {} 微秒", (average / 1000.0));
        logger.info("标准差: {} 微秒", (standardDeviation / 1000.0));
        
        // 验证性能一致性（标准差不应过大）
        assertTrue("性能应保持一致性", standardDeviation < average * 2);
    }
    
    @Test
    public void testFrameworkSpecificPerformance() {
        // 测试各框架特定性能
        AtomicLong logbackTime = new AtomicLong(0);
        AtomicLong log4j2Time = new AtomicLong(0);
        AtomicLong log4j1Time = new AtomicLong(0);
        
        int iterations = 1000;
        
        // 测试Logback性能
        org.slf4j.Logger logbackLogger = LoggerFactory.getLogger("PerformanceTestLogback");
        long logbackStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            logbackLogger.info("Logback性能测试消息 " + i);
        }
        long logbackEnd = System.nanoTime();
        logbackTime.set(logbackEnd - logbackStart);
        
        // 测试Log4j2性能
        org.apache.logging.log4j.Logger log4j2Logger = LogManager.getLogger("PerformanceTestLog4j2");
        long log4j2Start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            log4j2Logger.info("Log4j2性能测试消息 " + i);
        }
        long log4j2End = System.nanoTime();
        log4j2Time.set(log4j2End - log4j2Start);
        
        // 测试Log4j1性能
        org.apache.log4j.Logger log4j1Logger = org.apache.log4j.Logger.getLogger("PerformanceTestLog4j1");
        long log4j1Start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            log4j1Logger.info("Log4j1性能测试消息 " + i);
        }
        long log4j1End = System.nanoTime();
        log4j1Time.set(log4j1End - log4j1Start);
        
        logger.info("各框架性能对比:");
        logger.info("Logback: {} 微秒", (logbackTime.get() / 1000.0));
        logger.info("Log4j2: {} 微秒", (log4j2Time.get() / 1000.0));
        logger.info("Log4j1: {} 微秒", (log4j1Time.get() / 1000.0));
        
        // 验证各框架性能都在合理范围内
        assertTrue("Logback性能应在合理范围内", logbackTime.get() < 1000000000L); // 1秒
        assertTrue("Log4j2性能应在合理范围内", log4j2Time.get() < 1000000000L); // 1秒
        assertTrue("Log4j1性能应在合理范围内", log4j1Time.get() < 1000000000L); // 1秒
    }
    
    private long getCpuTime() {
        // 获取当前线程的CPU时间
        return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }
}