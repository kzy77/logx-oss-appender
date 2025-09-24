package org.logx.reliability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.*;

/**
 * 本地缓存管理器测试类
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
class LocalCacheManagerTest {

    @TempDir
    Path tempDir;

    private LocalCacheManager cacheManager;

    @BeforeEach
    void setUp() throws IOException {
        cacheManager = new LocalCacheManager(tempDir.toString(), 10 * 1024 * 1024, 60 * 1000); // 10MB, 1分钟
        cacheManager.initialize();
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.close();
        }
    }

    @Test
    void shouldInitializeSuccessfully() {
        assertThat(cacheManager.isInitialized()).isTrue();
        assertThat(cacheManager.getCacheDir()).isEqualTo(tempDir);
        assertThat(cacheManager.getCurrentCacheSize()).isEqualTo(0);
        assertThat(cacheManager.getCacheFileCount()).isEqualTo(0);
    }

    @Test
    void shouldCacheLogData() throws IOException {
        // Given
        String logData = "Test log message\nAnother line\n";
        byte[] data = logData.getBytes();

        // When
        cacheManager.cacheLogData(data);

        // Then
        assertThat(cacheManager.getCurrentCacheSize()).isGreaterThan(0);
        assertThat(cacheManager.getCacheFileCount()).isEqualTo(1);

        // 验证文件确实存在
        ConcurrentLinkedQueue<Path> cachedFiles = cacheManager.getCachedFiles();
        assertThat(cachedFiles).hasSize(1);

        Path cachedFile = cachedFiles.peek();
        assertThat(cachedFile).isNotNull();
        assertThat(Files.exists(cachedFile)).isTrue();
        assertThat(Files.readAllBytes(cachedFile)).isEqualTo(data);
    }

    @Test
    void shouldHandleMultipleCacheEntries() throws IOException {
        // Given
        byte[] data1 = "First log entry\n".getBytes();
        byte[] data2 = "Second log entry\nWith multiple lines\n".getBytes();
        byte[] data3 = "Third log entry\n".getBytes();

        // When
        cacheManager.cacheLogData(data1);
        cacheManager.cacheLogData(data2);
        cacheManager.cacheLogData(data3);

        // Then
        assertThat(cacheManager.getCacheFileCount()).isEqualTo(3);
        assertThat(cacheManager.getCurrentCacheSize()).isEqualTo(data1.length + data2.length + data3.length);

        ConcurrentLinkedQueue<Path> cachedFiles = cacheManager.getCachedFiles();
        assertThat(cachedFiles).hasSize(3);
    }

    @Test
    void shouldRemoveCachedFile() throws IOException {
        // Given
        byte[] data = "Test log data\n".getBytes();
        cacheManager.cacheLogData(data);

        long initialSize = cacheManager.getCurrentCacheSize();
        int initialCount = cacheManager.getCacheFileCount();

        ConcurrentLinkedQueue<Path> cachedFiles = cacheManager.getCachedFiles();
        Path cachedFile = cachedFiles.peek();

        // When
        cacheManager.removeCachedFile(cachedFile);

        // Then
        assertThat(cacheManager.getCurrentCacheSize()).isEqualTo(initialSize - data.length);
        assertThat(cacheManager.getCacheFileCount()).isEqualTo(initialCount - 1);
        assertThat(Files.exists(cachedFile)).isFalse();
    }

    @Test
    void shouldPerformCleanupWhenSizeLimitExceeded() throws IOException {
        // Given - 创建一个很小的缓存管理器用于测试清理
        LocalCacheManager smallCacheManager = new LocalCacheManager(tempDir.resolve("small").toString(), 1024, 60000);
        smallCacheManager.initialize();

        try {
            // 创建超过限制的数据
            byte[] largeData = new byte[2048]; // 2KB数据
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }

            // When - 缓存数据超过限制
            smallCacheManager.cacheLogData(largeData);

            // 等待一段时间确保清理执行
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then - 应该触发清理
            assertThat(smallCacheManager.getCurrentCacheSize()).isLessThan(2048 * 2); // 应该小于两倍数据大小
        } finally {
            smallCacheManager.close();
        }
    }

    @Test
    void shouldLoadExistingCacheEntries() throws IOException {
        // Given - 创建一些缓存文件
        byte[] data1 = "Existing log 1\n".getBytes();
        byte[] data2 = "Existing log 2\n".getBytes();

        Path file1 = tempDir.resolve("existing-1.log");
        Path file2 = tempDir.resolve("existing-2.log");

        Files.write(file1, data1);
        Files.write(file2, data2);

        // When - 重新初始化缓存管理器
        LocalCacheManager newCacheManager = new LocalCacheManager(tempDir.toString(), 10 * 1024 * 1024, 60000);
        newCacheManager.initialize();

        // Then - 应该加载现有的缓存文件
        assertThat(newCacheManager.getCacheFileCount()).isEqualTo(2);
        assertThat(newCacheManager.getCurrentCacheSize()).isEqualTo(data1.length + data2.length);

        newCacheManager.close();
    }

    @Test
    void shouldHandleNonExistentFileRemoval() throws IOException {
        // Given
        Path nonExistentFile = tempDir.resolve("non-existent.log");

        // When & Then - 不应该抛出异常
        assertThatNoException().isThrownBy(() -> cacheManager.removeCachedFile(nonExistentFile));
    }

    @Test
    void shouldThrowExceptionWhenNotInitialized() {
        // Given
        LocalCacheManager uninitializedManager = new LocalCacheManager();

        // When & Then
        assertThatThrownBy(() -> uninitializedManager.cacheLogData(new byte[0]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LocalCacheManager not initialized");

        assertThatThrownBy(() -> uninitializedManager.getCachedFiles())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LocalCacheManager not initialized");

        assertThatThrownBy(() -> uninitializedManager.removeCachedFile(tempDir.resolve("test.log")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LocalCacheManager not initialized");
    }

    @Test
    void shouldHandleConcurrentCacheOperations() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        // When - 启动多个线程并发缓存数据
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String logData = "Thread-" + threadId + "-Log-" + j + "\n";
                        cacheManager.cacheLogData(logData.getBytes());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        // Then - 验证结果
        assertThat(cacheManager.getCacheFileCount()).isEqualTo(threadCount * operationsPerThread);
        assertThat(cacheManager.getCurrentCacheSize()).isGreaterThan(0);
    }
}