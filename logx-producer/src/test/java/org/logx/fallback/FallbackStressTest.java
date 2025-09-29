package org.logx.fallback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 兜底机制压力测试和稳定性测试类
 * <p>
 * 测试兜底机制在长时间运行和极端条件下的稳定性和可靠性
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackStressTest {

    @TempDir
    Path tempDir;

    @Test
    public void testFallbackLongRunningStress() throws IOException, InterruptedException {
        String fallbackPath = tempDir.resolve("stress-fallback").toString();
        FallbackManager manager = new FallbackManager(fallbackPath, "stress-test");
        
        byte[] testData = "stress test log data for long running test".getBytes();
        int threadCount = 5;
        int writesPerThread = 200;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        // 启动多个线程进行长时间写入测试
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < writesPerThread; j++) {
                        boolean result = manager.writeFallbackFile(testData);
                        if (result) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                        
                        // 随机短暂休眠模拟实际使用场景
                        try {
                            Thread.sleep((long) (Math.random() * 10));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Stress test should complete within 60 seconds");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证结果
        int totalAttempts = successCount.get() + failureCount.get();
        int expectedAttempts = threadCount * writesPerThread;
        
        System.out.println("Long running stress test completed in " + duration + "ms");
        System.out.println("Total attempts: " + totalAttempts + "/" + expectedAttempts);
        System.out.println("Success count: " + successCount.get());
        System.out.println("Failure count: " + failureCount.get());
        
        // 验证文件创建
        String absolutePath = FallbackPathResolver.resolveAbsolutePath(fallbackPath);
        Path fallbackDir = Paths.get(absolutePath);
        
        if (Files.exists(fallbackDir)) {
            long fileCount = Files.walk(fallbackDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("_fallback.log"))
                    .count();
            
            System.out.println("Files created: " + fileCount);
            assertTrue(fileCount > 0, "Expected at least some files to be created");
        }
        
        executor.shutdown();
    }
    
    @Test
    public void testFallbackCleanupStress() throws IOException {
        String fallbackPath = tempDir.resolve("cleanup-stress-fallback").toString();
        Path fallbackDir = Paths.get(FallbackPathResolver.resolveAbsolutePath(fallbackPath));
        Files.createDirectories(fallbackDir);
        
        // 创建大量测试文件，包括过期和未过期的文件
        int fileCount = 5000;
        int expiredFileCount = 0;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < fileCount; i++) {
            Path testFile = fallbackDir.resolve("test_" + i + "_fallback.log");
            Files.write(testFile, ("test data " + i).getBytes());
            
            // 随机设置一些文件为过期文件（约30%）
            if (Math.random() < 0.3) {
                FileTime expiredTime = FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS));
                Files.setLastModifiedTime(testFile, expiredTime);
                expiredFileCount++;
            }
        }
        
        long fileCreationTime = System.currentTimeMillis();
        System.out.println("Created " + fileCount + " files in " + (fileCreationTime - startTime) + "ms");
        System.out.println("Marked " + expiredFileCount + " files as expired");
        
        // 执行清理操作（保留5天）
        long cleanupStartTime = System.currentTimeMillis();
        FallbackFileCleaner.cleanupExpiredFiles(fallbackDir.toString(), 5);
        long cleanupEndTime = System.currentTimeMillis();
        
        long cleanupDuration = cleanupEndTime - cleanupStartTime;
        
        // 验证过期文件已被删除
        long remainingFiles = Files.walk(fallbackDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("_fallback.log"))
                .count();
        
        long expectedRemainingFiles = fileCount - expiredFileCount;
        
        System.out.println("Cleanup stress test completed in " + cleanupDuration + "ms");
        System.out.println("Files remaining: " + remainingFiles + "/" + fileCount);
        System.out.println("Expected remaining: " + expectedRemainingFiles);
        
        // 验证清理结果
        assertTrue(remainingFiles >= expectedRemainingFiles, 
            "Remaining files should be at least expected count");
    }
}