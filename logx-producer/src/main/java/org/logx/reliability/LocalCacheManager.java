package org.logx.reliability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地缓存管理器
 * <p>
 * 网络故障时临时存储日志数据，网络恢复后自动重传。
 * 实现本地缓存的清理和容量管理，确保数据完整性和安全性。
 * <p>
 * 主要特性：
 * <ul>
 * <li>本地文件缓存：网络故障时将日志临时存储到本地文件</li>
 * <li>自动重传：网络恢复后自动重传缓存的日志数据</li>
 * <li>容量管理：限制缓存大小，防止磁盘空间耗尽</li>
 * <li>数据安全：确保本地缓存的数据完整性和安全性</li>
 * </ul>
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class LocalCacheManager {

    private static final long DEFAULT_MAX_CACHE_SIZE_BYTES = 100 * 1024 * 1024; // 100MB
    private static final String DEFAULT_CACHE_DIR = System.getProperty("java.io.tmpdir") + "/oss-appender-cache";
    private static final long DEFAULT_CLEANUP_INTERVAL_MS = 60 * 60 * 1000; // 1小时

    private final Path cacheDir;
    private final long maxCacheSizeBytes;
    private final long cleanupIntervalMs;

    private final AtomicLong currentCacheSize = new AtomicLong(0);
    private final ConcurrentLinkedQueue<CacheEntry> cacheEntries = new ConcurrentLinkedQueue<>();
    private final AtomicLong lastCleanupTime = new AtomicLong(System.currentTimeMillis());

    private volatile boolean initialized = false;

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final String fileName;
        final long sizeBytes;

        CacheEntry(String fileName, long sizeBytes) {
            this.fileName = fileName;
            this.sizeBytes = sizeBytes;
        }
    }

    /**
     * 构造本地缓存管理器
     *
     * @param cacheDir
     *            缓存目录路径
     * @param maxCacheSizeBytes
     *            最大缓存大小（字节）
     * @param cleanupIntervalMs
     *            清理间隔（毫秒）
     */
    public LocalCacheManager(String cacheDir, long maxCacheSizeBytes, long cleanupIntervalMs) {
        this.cacheDir = Paths.get(cacheDir);
        this.maxCacheSizeBytes = maxCacheSizeBytes;
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    /**
     * 使用默认配置构造
     */
    public LocalCacheManager() {
        this(DEFAULT_CACHE_DIR, DEFAULT_MAX_CACHE_SIZE_BYTES, DEFAULT_CLEANUP_INTERVAL_MS);
    }

    /**
     * 初始化缓存管理器
     *
     * @throws IOException
     *             如果初始化失败
     */
    public synchronized void initialize() throws IOException {
        if (initialized) {
            return;
        }

        // 创建缓存目录
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }

        // 检查目录是否可写
        if (!Files.isWritable(cacheDir)) {
            throw new IOException("Cache directory is not writable: " + cacheDir);
        }

        // 加载现有的缓存文件信息
        loadExistingCacheEntries();

        initialized = true;
        System.out.println("LocalCacheManager initialized, cache dir: " + cacheDir + ", current size: "
                + currentCacheSize.get() + " bytes");
    }

    /**
     * 加载现有的缓存文件信息
     */
    private void loadExistingCacheEntries() {
        try {
            Files.list(cacheDir).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".log"))
                    .forEach(path -> {
                        try {
                            // 添加空指针检查
                            java.nio.file.Path fileNamePath = path.getFileName();
                            if (fileNamePath != null) {
                                String fileName = fileNamePath.toString();
                                long size = Files.size(path);

                                cacheEntries.offer(new CacheEntry(fileName, size));
                                currentCacheSize.addAndGet(size);
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to load cache entry: " + path + ", error: " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to list cache directory: " + e.getMessage());
        }
    }

    /**
     * 缓存日志数据
     *
     * @param data
     *            日志数据
     *
     * @throws IOException
     *             如果缓存失败
     */
    public void cacheLogData(byte[] data) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("LocalCacheManager not initialized");
        }

        // 检查是否需要清理
        checkAndCleanup();

        // 生成文件名
        String fileName = "log-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))
                + "-" + Thread.currentThread().getId() + ".log";

        // 写入文件
        Path filePath = cacheDir.resolve(fileName);
        Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.SYNC);

        // 更新缓存信息
        long fileSize = Files.size(filePath);
        cacheEntries.offer(new CacheEntry(fileName, fileSize));
        currentCacheSize.addAndGet(fileSize);

        System.out.println("Log data cached to: " + filePath + ", size: " + fileSize + " bytes");
    }

    /**
     * 获取缓存的数据文件
     *
     * @return 缓存文件路径列表
     */
    public ConcurrentLinkedQueue<Path> getCachedFiles() {
        if (!initialized) {
            throw new IllegalStateException("LocalCacheManager not initialized");
        }

        ConcurrentLinkedQueue<Path> result = new ConcurrentLinkedQueue<>();
        for (CacheEntry entry : cacheEntries) {
            result.offer(cacheDir.resolve(entry.fileName));
        }
        return result;
    }

    /**
     * 删除已处理的缓存文件
     *
     * @param filePath
     *            已处理的文件路径
     *
     * @throws IOException
     *             如果删除失败
     */
    public void removeCachedFile(Path filePath) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("LocalCacheManager not initialized");
        }

        Path fileNamePath = filePath.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("Invalid file path: " + filePath);
        }
        
        String fileName = fileNamePath.toString();

        // 删除文件
        if (Files.exists(filePath)) {
            long fileSize = Files.size(filePath);
            Files.delete(filePath);

            // 更新缓存信息
            cacheEntries.removeIf(entry -> {
                if (entry.fileName.equals(fileName)) {
                    currentCacheSize.addAndGet(-entry.sizeBytes);
                    return true;
                }
                return false;
            });

            System.out.println("Cached file removed: " + filePath + ", size: " + fileSize + " bytes");
        }
    }

    /**
     * 检查并清理缓存
     */
    private void checkAndCleanup() {
        long now = System.currentTimeMillis();
        long lastCleanup = lastCleanupTime.get();

        // 检查是否需要清理
        if (now - lastCleanup > cleanupIntervalMs) {
            if (lastCleanupTime.compareAndSet(lastCleanup, now)) {
                performCleanup();
            }
        }

        // 检查是否超过最大缓存大小
        if (currentCacheSize.get() > maxCacheSizeBytes) {
            performCleanup();
        }
    }

    /**
     * 执行清理操作
     */
    private void performCleanup() {
        long currentSize = currentCacheSize.get();
        if (currentSize <= maxCacheSizeBytes) {
            return;
        }

        System.out.println("Performing cache cleanup, current size: " + currentSize + " bytes, max size: "
                + maxCacheSizeBytes + " bytes");

        // 按时间排序，删除最旧的文件
        long bytesToRemove = currentSize - maxCacheSizeBytes + (maxCacheSizeBytes / 10); // 多清理10%的空间

        long removedBytes = 0;
        while (removedBytes < bytesToRemove && !cacheEntries.isEmpty()) {
            CacheEntry oldestEntry = cacheEntries.poll();
            if (oldestEntry != null) {
                try {
                    Path filePath = cacheDir.resolve(oldestEntry.fileName);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        removedBytes += oldestEntry.sizeBytes;
                        currentCacheSize.addAndGet(-oldestEntry.sizeBytes);
                        System.out.println("Removed old cache file: " + filePath + ", size: " + oldestEntry.sizeBytes
                                + " bytes");
                    }
                } catch (IOException e) {
                    System.err.println("Failed to remove cache file: " + oldestEntry.fileName + ", error: "
                            + e.getMessage());
                }
            }
        }

        System.out.println("Cache cleanup completed, removed " + removedBytes + " bytes");
    }

    /**
     * 获取当前缓存大小
     */
    public long getCurrentCacheSize() {
        return currentCacheSize.get();
    }

    /**
     * 获取缓存文件数量
     */
    public int getCacheFileCount() {
        return cacheEntries.size();
    }

    /**
     * 获取最大缓存大小
     */
    public long getMaxCacheSizeBytes() {
        return maxCacheSizeBytes;
    }

    /**
     * 获取缓存目录
     */
    public Path getCacheDir() {
        return cacheDir;
    }

    /**
     * 关闭缓存管理器
     */
    public synchronized void close() {
        if (!initialized) {
            return;
        }

        System.out.println("LocalCacheManager closed, final cache size: " + currentCacheSize.get() + " bytes");
        initialized = false;
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}