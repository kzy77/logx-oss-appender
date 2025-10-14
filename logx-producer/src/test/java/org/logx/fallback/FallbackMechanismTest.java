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
 * 兜底机制测试类
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackMechanismTest {

    @TempDir
    Path tempDir;

    @Test
    public void testFallbackPathResolver() {
        // 测试相对路径解析
        String relativePath = "test-fallback";
        String absolutePath = FallbackPathResolver.resolveAbsolutePath(relativePath);
        assertNotNull(absolutePath);
        assertTrue(Paths.get(absolutePath).isAbsolute());

        // 测试绝对路径解析
        String absoluteInput = "/tmp/test-fallback";
        String resolvedAbsolute = FallbackPathResolver.resolveAbsolutePath(absoluteInput);
        assertEquals(absoluteInput, resolvedAbsolute);
    }

    @Test
    public void testObjectNameGenerator() {
        ObjectNameGenerator generator = new ObjectNameGenerator("testapp");

        // 测试对象名生成
        // 格式：yyyy/MM/dd/HHmmssSSS-applog-IP-uniqueId.log.gz
        String objectName = generator.generateObjectName();
        assertNotNull(objectName);
        assertTrue(objectName.contains("-applog-"));
        assertTrue(objectName.endsWith(".log.gz"));

        // 测试兜底对象名生成（与正常上传使用相同规则）
        String fallbackName = generator.generateObjectName();
        assertNotNull(fallbackName);
        assertTrue(fallbackName.contains("-applog-"));
        assertTrue(fallbackName.endsWith(".log.gz"));

        // 测试重传对象名生成（与正常上传使用相同规则）
        String retryName = generator.generateObjectName();
        assertNotNull(retryName);
        assertTrue(retryName.contains("-applog-"));
        assertTrue(retryName.endsWith(".log.gz"));
    }

    @Test
    public void testFallbackManager() throws IOException {
        String fallbackPath = tempDir.resolve("fallback").toString();
        FallbackManager manager = new FallbackManager(fallbackPath, "testapp");

        // 测试兜底文件写入
        byte[] testData = "test log data".getBytes();
        boolean result = manager.writeFallbackFile(testData);
        assertTrue(result);

        // 验证文件是否存在
        String absolutePath = FallbackPathResolver.resolveAbsolutePath(fallbackPath);
        Path fallbackDir = Paths.get(absolutePath);
        assertTrue(Files.exists(fallbackDir));
        
        // 检查是否有文件被创建
        try (java.util.stream.Stream<Path> files = Files.walk(fallbackDir)) {
            long fileCount = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log.gz"))
                    .count();
            assertEquals(1, fileCount);
        }
    }
    
    @Test
    public void testFallbackFileCleaner() throws IOException {
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
}