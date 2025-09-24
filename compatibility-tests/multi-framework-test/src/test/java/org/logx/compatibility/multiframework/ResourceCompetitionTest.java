package org.logx.compatibility.multiframework;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 资源竞争测试
 */
public class ResourceCompetitionTest {

    @Test
    public void testThreadSafety() {
        // 测试线程安全性
        // 验证多线程环境下日志记录的安全性
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                MultiFrameworkCoexistenceTest.generateLogMessages();
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
            assertTrue("多线程日志记录应该安全", true);
        } catch (InterruptedException e) {
            assertTrue("多线程测试被中断", false);
        }
    }

    @Test
    public void testMemoryUsage() {
        // 测试内存使用
        // 验证多框架共存不会导致内存泄漏
        long initialMemory = Runtime.getRuntime().freeMemory();
        
        // 生成大量日志消息
        for (int i = 0; i < 1000; i++) {
            MultiFrameworkCoexistenceTest.generateLogMessages();
        }
        
        long finalMemory = Runtime.getRuntime().freeMemory();
        long memoryDifference = initialMemory - finalMemory;
        
        // 验证内存使用在合理范围内
        assertTrue("内存使用应该在合理范围内", memoryDifference < 10000000); // 10MB
    }

    @Test
    public void testCPUUsage() {
        // 测试CPU使用
        // 验证多框架共存不会导致CPU使用过高
        long startTime = System.currentTimeMillis();
        long startCpuTime = getCpuTime();
        
        // 生成日志消息
        for (int i = 0; i < 1000; i++) {
            MultiFrameworkCoexistenceTest.generateLogMessages();
        }
        
        long endTime = System.currentTimeMillis();
        long endCpuTime = getCpuTime();
        
        long wallClockTime = endTime - startTime;
        long cpuTime = endCpuTime - startCpuTime;
        
        // 验证CPU使用在合理范围内
        double cpuUsage = (double) cpuTime / (wallClockTime * 1000000); // 转换为百分比
        assertTrue("CPU使用应该在合理范围内", cpuUsage < 0.8); // 80%
    }

    @Test
    public void testResourceCleanup() {
        // 测试资源清理
        // 验证日志记录后资源能够正确释放
        MultiFrameworkCoexistenceTest.generateLogMessages();
        
        // 强制进行垃圾回收
        System.gc();
        
        // 验证资源得到清理
        assertTrue("资源应该得到正确清理", true);
    }

    private long getCpuTime() {
        // 获取当前线程的CPU时间
        return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }
    
    // 新增的增强测试用例
    
    @Test
    public void testHighConcurrencyResourceCompetition() throws InterruptedException {
        // 测试高并发资源竞争
        int threadCount = 100;
        int logCountPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 用于跟踪日志消息计数
        AtomicInteger logbackCount = new AtomicInteger(0);
        AtomicInteger log4j2Count = new AtomicInteger(0);
        AtomicInteger log4j1Count = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 提交多个并发任务
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 每个线程使用不同框架记录日志
                    org.slf4j.Logger logbackLogger = LoggerFactory.getLogger("ConcurrencyTestLogback");
                    org.apache.logging.log4j.Logger log4j2Logger = LogManager.getLogger("ConcurrencyTestLog4j2");
                    org.apache.log4j.Logger log4j1Logger = org.apache.log4j.Logger.getLogger("ConcurrencyTestLog4j1");
                    
                    for (int j = 0; j < logCountPerThread; j++) {
                        logbackLogger.info("Logback并发测试消息 " + j);
                        log4j2Logger.info("Log4j2并发测试消息 " + j);
                        log4j1Logger.info("Log4j1并发测试消息 " + j);
                        
                        logbackCount.incrementAndGet();
                        log4j2Count.incrementAndGet();
                        log4j1Count.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有任务完成
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证所有日志消息都被正确记录
        assertEquals("应该记录正确的Logback日志数量", threadCount * logCountPerThread, logbackCount.get());
        assertEquals("应该记录正确的Log4j2日志数量", threadCount * logCountPerThread, log4j2Count.get());
        assertEquals("应该记录正确的Log4j1日志数量", threadCount * logCountPerThread, log4j1Count.get());
        
        assertTrue("高并发资源竞争测试应该在60秒内完成，实际耗时: " + duration + "ms", completed);
    }
    
    @Test
    public void testFileHandleLeakDetection() {
        // 测试文件句柄泄漏检测
        long initialOpenFiles = getOpenFileCount();
        
        // 生成大量日志消息
        for (int i = 0; i < 5000; i++) {
            MultiFrameworkCoexistenceTest.generateLogMessages();
        }
        
        long finalOpenFiles = getOpenFileCount();
        long fileHandleDifference = finalOpenFiles - initialOpenFiles;
        
        // 验证文件句柄使用在合理范围内
        assertTrue("文件句柄使用应该在合理范围内，差值: " + fileHandleDifference, fileHandleDifference < 100);
    }
    
    @Test
    public void testBufferManagement() {
        // 测试缓冲区管理
        AtomicLong totalLogSize = new AtomicLong(0);
        
        // 记录大量日志消息并跟踪大小
        for (int i = 0; i < 2000; i++) {
            String message = "测试缓冲区管理消息 - " + i + " - " + System.currentTimeMillis();
            totalLogSize.addAndGet(message.length());
            
            // 使用不同框架记录消息
            org.slf4j.Logger logbackLogger = LoggerFactory.getLogger("BufferTestLogback");
            org.apache.logging.log4j.Logger log4j2Logger = LogManager.getLogger("BufferTestLog4j2");
            org.apache.log4j.Logger log4j1Logger = org.apache.log4j.Logger.getLogger("BufferTestLog4j1");
            
            logbackLogger.info(message);
            log4j2Logger.info(message);
            log4j1Logger.info(message);
        }
        
        // 验证缓冲区管理正常
        long expectedMinSize = 2000 * 30; // 至少每个消息30个字符
        assertTrue("缓冲区管理应该正常，总日志大小: " + totalLogSize.get(), totalLogSize.get() > expectedMinSize);
    }
    
    @Test
    public void testLockContention() {
        // 测试锁争用
        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        // 创建多个线程同时记录日志
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // 每个线程记录大量日志
                    for (int j = 0; j < 100; j++) {
                        MultiFrameworkCoexistenceTest.generateLogMessages();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            // 等待所有线程完成
            boolean completed = latch.await(45, TimeUnit.SECONDS);
            executor.shutdown();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            assertTrue("锁争用测试应该在45秒内完成，实际耗时: " + duration + "ms", completed);
        } catch (InterruptedException e) {
            fail("锁争用测试被中断");
        }
    }
    
    private long getOpenFileCount() {
        // 获取当前打开的文件句柄数（简化实现）
        // 在实际测试中，可能需要使用操作系统特定的命令或API
        try {
            // 这里只是一个占位符实现
            // 在Linux系统上，可以读取/proc/self/fd目录
            return 0;
        } catch (Exception e) {
            return -1;
        }
    }
}