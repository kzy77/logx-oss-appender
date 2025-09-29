package org.logx.fallback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 兜底文件清理器测试类
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackFileCleanerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testCleanupExpiredFiles() throws IOException {
        // 创建兜底目录
        Path fallbackDir = tempDir.resolve("fallback");
        Files.createDirectories(fallbackDir);

        // 创建一个过期文件（7天前创建）
        Path expiredFile = fallbackDir.resolve("expired_fallback.log");
        Files.write(expiredFile, "test data".getBytes());
        
        // 设置文件的最后修改时间为7天前
        FileTime expiredTime = FileTime.from(Instant.now().minus(7, ChronoUnit.DAYS));
        Files.setLastModifiedTime(expiredFile, expiredTime);

        // 创建一个未过期文件（1天前创建）
        Path recentFile = fallbackDir.resolve("recent_fallback.log");
        Files.write(recentFile, "recent data".getBytes());
        
        // 设置文件的最后修改时间为1天前
        FileTime recentTime = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Files.setLastModifiedTime(recentFile, recentTime);

        // 执行清理操作（保留3天）
        FallbackFileCleaner.cleanupExpiredFiles(fallbackDir.toString(), 3);

        // 验证过期文件已被删除
        assertFalse(Files.exists(expiredFile), "Expired file should be deleted");
        
        // 验证未过期文件仍然存在
        assertTrue(Files.exists(recentFile), "Recent file should not be deleted");
    }

    @Test
    public void testCleanupWithNonExistentDirectory() {
        // 测试不存在的目录
        Path nonExistentDir = tempDir.resolve("non-existent");
        FallbackFileCleaner.cleanupExpiredFiles(nonExistentDir.toString(), 3);
        
        // 应该不会抛出异常
        assertTrue(true, "Should not throw exception for non-existent directory");
    }

    @Test
    public void testCleanupWithZeroRetentionDays() throws IOException {
        // 创建兜底目录
        Path fallbackDir = tempDir.resolve("fallback");
        Files.createDirectories(fallbackDir);

        // 创建一个文件
        Path testFile = fallbackDir.resolve("test_fallback.log");
        Files.write(testFile, "test data".getBytes());
        
        // 设置文件的最后修改时间为1天前，确保它会被删除
        FileTime oldTime = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Files.setLastModifiedTime(testFile, oldTime);

        // 使用0天数执行清理操作（应该删除所有文件）
        FallbackFileCleaner.cleanupExpiredFiles(fallbackDir.toString(), 0);

        // 文件应该被删除
        assertFalse(Files.exists(testFile), "File should be deleted with 0 retention days");
    }
}