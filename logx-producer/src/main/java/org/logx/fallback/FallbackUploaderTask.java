package org.logx.fallback;

import org.logx.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 兜底文件上传任务
 * <p>
 * 负责定时扫描兜底目录并重新上传文件到云存储
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class FallbackUploaderTask implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackUploaderTask.class);
    private static final String FALLBACK_FILE_SUFFIX = "_fallback.log";
    private static final int UPLOAD_TIMEOUT_SECONDS = 30;
    
    private final StorageService storageService;
    private final String fallbackPath;
    private final String absoluteFallbackPath;
    private final ObjectNameGenerator nameGenerator;
    private final int retentionDays;
    
    public FallbackUploaderTask(StorageService storageService, String fallbackPath, String fileName, int retentionDays) {
        this.storageService = storageService;
        this.fallbackPath = fallbackPath;
        this.absoluteFallbackPath = FallbackPathResolver.resolveAbsolutePath(fallbackPath);
        this.nameGenerator = new ObjectNameGenerator(fileName);
        this.retentionDays = retentionDays;
    }
    
    @Override
    public void run() {
        try {
            // 首先清理过期文件
            cleanupExpiredFiles();
            
            // 然后重传现有的兜底文件
            retryUploadFiles();
        } catch (Exception e) {
            logger.error("Failed to execute fallback upload task", e);
        }
    }
    
    /**
     * 清理过期的兜底文件
     */
    private void cleanupExpiredFiles() {
        try {
            FallbackFileCleaner.cleanupExpiredFiles(fallbackPath, retentionDays);
        } catch (Exception e) {
            logger.warn("Failed to cleanup expired fallback files", e);
        }
    }
    
    /**
     * 重传兜底文件
     */
    private void retryUploadFiles() {
        try {
            Path fallbackDir = Paths.get(absoluteFallbackPath);
            
            if (!Files.exists(fallbackDir) || !Files.isDirectory(fallbackDir)) {
                logger.warn("Fallback directory does not exist or is not a directory: {}", absoluteFallbackPath);
                return;
            }
            
            // 遍历兜底目录中的所有文件
            try (Stream<Path> files = Files.walk(fallbackDir)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(FALLBACK_FILE_SUFFIX))
                     .forEach(this::retryUpload);
            }
        } catch (IOException e) {
            logger.error("Failed to scan fallback directory: {}", absoluteFallbackPath, e);
        }
    }
    
    private void retryUpload(Path file) {
        try {
            // 从文件路径重建对象名
            String relativePath = getRelativePath(file);
            String retryObjectName = nameGenerator.generateRetryObjectName(relativePath);
            
            byte[] data = Files.readAllBytes(file);
            
            // 上传到存储服务
            CompletableFuture<Void> future = storageService.putObject(retryObjectName, data);
            future.get(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS); // 30秒超时
            
            // 上传成功后删除本地文件
            Files.delete(file);
            logger.info("Successfully resent fallback file as: {}", retryObjectName);
        } catch (Exception e) {
            logger.error("Failed to retry upload for file: {}", file.getFileName(), e);
        }
    }
    
    private String getRelativePath(Path file) {
        try {
            String fullPath = file.toString();
            
            if (fullPath.startsWith(absoluteFallbackPath)) {
                return fullPath.substring(absoluteFallbackPath.length() + 1);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract relative path for file: {}", file.getFileName(), e);
        }
        return file.getFileName().toString();
    }
}