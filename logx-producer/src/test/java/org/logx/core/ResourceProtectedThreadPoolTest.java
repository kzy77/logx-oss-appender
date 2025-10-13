package org.logx.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * ResourceProtectedThreadPool测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class ResourceProtectedThreadPoolTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceProtectedThreadPoolTest.class);

    private ResourceProtectedThreadPool threadPool;

    @BeforeEach
    void setUp() {
        // 使用小容量便于测试
        ResourceProtectedThreadPool.Config config = ResourceProtectedThreadPool.Config.defaultConfig().corePoolSize(2)
                .maximumPoolSize(4).queueCapacity(10).cpuThreshold(0.9) // 设置较高阈值避免测试中触发
                .memoryThreshold(0.95);

        threadPool = new ResourceProtectedThreadPool(config);
    }

    @Test
    void shouldCreateThreadPoolWithDefaultConfig() {
        // When
        ResourceProtectedThreadPool defaultPool = new ResourceProtectedThreadPool();

        // Then
        assertThat(defaultPool).isNotNull();
        ResourceProtectedThreadPool.PoolMetrics metrics = defaultPool.getMetrics();
        assertThat(metrics.getPoolSize()).isGreaterThanOrEqualTo(0);

        defaultPool.close();
    }

    @Test
    void shouldExecuteTasksSuccessfully() throws Exception {
        // Given
        AtomicInteger taskCounter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        // When
        for (int i = 0; i < 3; i++) {
            threadPool.submit(() -> {
                taskCounter.incrementAndGet();
                latch.countDown();
            });
        }

        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(taskCounter.get()).isEqualTo(3);

        ResourceProtectedThreadPool.PoolMetrics metrics = threadPool.getMetrics();
        assertThat(metrics.getTotalSubmitted()).isEqualTo(3);
        assertThat(metrics.getTotalCompleted()).isEqualTo(3);
    }

    @Test
    void shouldExecuteCallableTasksSuccessfully() throws Exception {
        // When
        Future<String> future = threadPool.submit(() -> "test result");

        // Then
        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("test result");
    }

    @Test
    void shouldSetLowThreadPriority() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger threadPriority = new AtomicInteger();

        // When
        threadPool.submit(() -> {
            threadPriority.set(Thread.currentThread().getPriority());
            latch.countDown();
        });

        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(threadPriority.get()).isEqualTo(Thread.MIN_PRIORITY);
    }

    @Test
    void shouldCreateDaemonThreads() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger isDaemon = new AtomicInteger();

        // When
        threadPool.submit(() -> {
            isDaemon.set(Thread.currentThread().isDaemon() ? 1 : 0);
            latch.countDown();
        });

        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(isDaemon.get()).isEqualTo(1);
    }

    @Test
    void shouldProvideMonitoringMetrics() {
        // When
        ResourceProtectedThreadPool.PoolMetrics metrics = threadPool.getMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getPoolSize()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getCurrentCpuUsage()).isGreaterThanOrEqualTo(0.0);
        assertThat(metrics.getCurrentMemoryUsage()).isGreaterThanOrEqualTo(0.0);

        String metricsString = metrics.toString();
        assertThat(metricsString).contains("PoolMetrics");
        assertThat(metricsString).contains("poolSize");
    }

    @Test
    @Timeout(10)
    void shouldHandleQueueOverflow() throws Exception {
        // Given - 创建小容量线程池
        ResourceProtectedThreadPool.Config smallConfig = ResourceProtectedThreadPool.Config.defaultConfig()
                .corePoolSize(1).maximumPoolSize(1).queueCapacity(2);

        ResourceProtectedThreadPool smallPool = new ResourceProtectedThreadPool(smallConfig);

        // When - 提交超过容量的任务
        CountDownLatch blockingLatch = new CountDownLatch(1);
        AtomicInteger completedTasks = new AtomicInteger(0);

        // 提交阻塞任务占用线程
        smallPool.submit(() -> {
            try {
                blockingLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 提交更多任务直到队列满
        for (int i = 0; i < 10; i++) {
            smallPool.submit(() -> completedTasks.incrementAndGet());
        }

        // 释放阻塞，让任务执行
        blockingLatch.countDown();

        // Then
        // 等待任务执行
        Thread.sleep(500);
        ResourceProtectedThreadPool.PoolMetrics metrics = smallPool.getMetrics();

        // 验证有任务被拒绝
        assertThat(metrics.getTotalRejected()).isGreaterThan(0);
        assertThat(metrics.getTotalSubmitted()).isGreaterThan(metrics.getTotalCompleted());

        smallPool.close();
    }

    @Test
    void shouldHandleMemoryProtection() {
        // Given - 创建启用内存保护的线程池
        // 在8GB环境下使用更现实的高阈值来触发内存保护
        ResourceProtectedThreadPool.Config config = ResourceProtectedThreadPool.Config.defaultConfig()
                .memoryThreshold(0.05) // 设置5%的内存阈值，当前使用约7%，应该能触发保护
                .enableMemoryProtection(true);

        ResourceProtectedThreadPool protectedPool = new ResourceProtectedThreadPool(config);

        // 先消耗大量内存以接近阈值
        java.util.List<byte[]> memoryConsumer = new java.util.ArrayList<>();
        try {
            // 使用与getCurrentMemoryUsage()相同的计算方式
            java.lang.management.MemoryMXBean memoryMXBean = java.lang.management.ManagementFactory.getMemoryMXBean();
            long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
            // 超过95%阈值
            long targetMemory = (long) (maxMemory * 0.96);

            logger.info("Max memory: " + maxMemory / (1024 * 1024) + "MB");

            // 消耗内存直到接近95%阈值
            while (memoryMXBean.getHeapMemoryUsage().getUsed() < targetMemory) {
                // 每次分配1MB
                memoryConsumer.add(new byte[1024 * 1024]);
                // 防止无限循环
                if (memoryConsumer.size() > 2000) {
                    break;
                }

                // 每100MB打印一次当前使用情况
                if (memoryConsumer.size() % 100 == 0) {
                    long currentUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
                    double currentUsage = (double) currentUsed / maxMemory;
                    logger.info("Current memory usage: " + (currentUsage * 100) + "%");
                }
            }

            // 验证当前内存使用率
            long currentUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            double currentUsage = (double) currentUsed / maxMemory;
            logger.info("Final memory usage before test: " + (currentUsage * 100) + "%");

            // When & Then - 现在应该触发内存保护
            assertThatThrownBy(() -> {
                protectedPool.submit(() -> {
                    // 这个任务应该不会被执行，因为内存保护会拒绝它
                });
            }).isInstanceOf(ResourceProtectedThreadPool.ResourceProtectionException.class)
                    .hasMessageContaining("Memory pressure too high");

        } finally {
            // 清理内存
            memoryConsumer.clear();
            System.gc();
            protectedPool.close();
        }
    }

    @Test
    void shouldMeasureResourceUsage() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        // When - 提交CPU密集型任务
        threadPool.submit(() -> {
            // 模拟一些工作
            for (int i = 0; i < 1000000; i++) {
                Math.sqrt(i);
            }
            latch.countDown();
        });

        // Wait for task to complete
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        // Then
        ResourceProtectedThreadPool.PoolMetrics metrics = threadPool.getMetrics();
        assertThat(metrics.getTotalCompleted()).isGreaterThan(0);

        // CPU和内存使用率应该是有效值
        assertThat(metrics.getCurrentCpuUsage()).isGreaterThanOrEqualTo(0.0);
        assertThat(metrics.getCurrentMemoryUsage()).isGreaterThan(0.0);
        assertThat(metrics.getCurrentMemoryUsage()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void shouldHandleExceptionsGracefully() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        // When - 提交会抛异常的任务
        threadPool.submit(() -> {
            latch.countDown();
            throw new RuntimeException("测试异常");
        });

        // Then - 异常不应该影响线程池运行
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // 线程池应该仍然可用
        CountDownLatch secondLatch = new CountDownLatch(1);
        threadPool.submit(secondLatch::countDown);
        assertThat(secondLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @Timeout(15)
    void shouldShutdownGracefully() throws Exception {
        // Given
        CountDownLatch taskLatch = new CountDownLatch(2);

        threadPool.submit(() -> {
            try {
                Thread.sleep(100);
                taskLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        threadPool.submit(() -> {
            try {
                Thread.sleep(100);
                taskLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // When
        // 应该等待任务完成
        threadPool.close();

        // Then
        assertThat(taskLatch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void shouldTestYieldMechanism() throws Exception {
        // Given - 创建启用CPU让出的线程池
        ResourceProtectedThreadPool.Config config = ResourceProtectedThreadPool.Config.defaultConfig()
                .cpuThreshold(-0.1) // 设置负阈值，强制触发CPU让出
                .enableCpuYield(true);

        ResourceProtectedThreadPool yieldPool = new ResourceProtectedThreadPool(config);

        CountDownLatch latch = new CountDownLatch(10);

        // When - 提交多个CPU密集型任务
        for (int i = 0; i < 10; i++) {
            yieldPool.submit(() -> {
                // 模拟一些CPU工作以触发yield机制
                for (int j = 0; j < 10000; j++) {
                    Math.sqrt(j);
                }
                latch.countDown();
            });
        }

        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        ResourceProtectedThreadPool.PoolMetrics metrics = yieldPool.getMetrics();
        // 由于CPU阈值设为负值，任何CPU使用率都会触发yield
        assertThat(metrics.getTotalYieldCount()).isGreaterThan(0);

        yieldPool.close();
    }
}
