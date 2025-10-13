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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 兜底机制集成测试类
 * <p>
 * 验证兜底机制的完整流程，包括文件存储、重传和清理
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    public void testFallbackMechanismIntegration() throws IOException {
        // 设置测试环境
        String fallbackPath = tempDir.resolve("fallback").toString();
        String fileName = "integration-test";
        
        // 创建兜底管理器
        FallbackManager manager = new FallbackManager(fallbackPath, fileName);
        
        // 1. 测试兜底文件写入
        byte[] testData = "integration test log data".getBytes();
        boolean writeResult = manager.writeFallbackFile(testData);
        assertTrue(writeResult, "Fallback file should be written successfully");
        
        // 验证文件已创建
        String absolutePath = FallbackPathResolver.resolveAbsolutePath(fallbackPath);
        Path fallbackDir = Paths.get(absolutePath);
        assertTrue(Files.exists(fallbackDir), "Fallback directory should exist");
        
        // 检查是否有兜底文件被创建
        long fallbackFileCount = Files.walk(fallbackDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("_fallback.log.gz"))
                .count();
        assertEquals(1, fallbackFileCount, "One fallback file should be created");
        
        // 2. 测试文件重传命名
        Path fallbackFile = Files.walk(fallbackDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("_fallback.log.gz"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fallback file not found"));
                
        ObjectNameGenerator generator = new ObjectNameGenerator(fileName);
        String relativePath = fallbackFile.getFileName().toString();
        String retryObjectName = generator.generateRetryObjectName(relativePath);
        
        assertNotNull(retryObjectName, "Retry object name should be generated");
        assertTrue(retryObjectName.endsWith("_retried.log.gz"), "Retry object name should end with _retried.log.gz");
        
        // 3. 测试文件清理机制
        // 创建一个过期文件（7天前创建）
        Path expiredFile = fallbackDir.resolve("expired_fallback.log");
        Files.write(expiredFile, "expired test data".getBytes());
        
        // 设置文件的最后修改时间为7天前
        FileTime expiredTime = FileTime.from(Instant.now().minus(7, ChronoUnit.DAYS));
        Files.setLastModifiedTime(expiredFile, expiredTime);

        // 创建一个未过期文件（1天前创建）
        Path recentFile = fallbackDir.resolve("recent_fallback.log");
        Files.write(recentFile, "recent test data".getBytes());
        
        // 设置文件的最后修改时间为1天前
        FileTime recentTime = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Files.setLastModifiedTime(recentFile, recentTime);
        
        // 执行清理操作（保留3天）
        FallbackFileCleaner.cleanupExpiredFiles(fallbackDir.toString(), 3);

        // 验证过期文件已被删除
        assertFalse(Files.exists(expiredFile), "Expired file should be deleted");
        
        // 验证未过期文件仍然存在
        assertTrue(Files.exists(recentFile), "Recent file should not be deleted");
        
        // 验证最初创建的兜底文件仍然存在（因为它不是过期的）
        assertTrue(Files.exists(fallbackFile), "Original fallback file should still exist");
    }
}