# 代码使用示例

## 概述

本文档提供 S3StorageInterface 的完整代码使用示例，涵盖常见的使用场景和最佳实践。

## 基础使用示例

### 1. 简单的日志上传

```java
import org.logx.storage.StorageConfig;
import org.logx.storage.s3.S3StorageFactory;
import org.logx.storage.StorageBackend;
import org.logx.s3.S3StorageInterface;

public class SimpleLogUploadExample {

    public static void main(String[] args) {
        // 创建配置
        TestS3Config config = TestS3Config.builder()
            .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
            .region("cn-hangzhou")
            .accessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
            .accessKeySecret(System.getenv("OSS_SECRET_ACCESS_KEY"))
            .bucket("app-logs")
            .build();

        // 创建存储适配器
        S3StorageInterface storage = S3StorageFactory.createAdapter(config);

        // 上传日志
        String logKey = "application/2024/01/15/app.log";
        String logContent = "2024-01-15 10:30:00 INFO Application started successfully";
        byte[] logData = logContent.getBytes();

        storage.putObject(logKey, logData)
            .thenRun(() -> {
                System.out.println("日志上传成功: " + logKey);
            })
            .exceptionally(throwable -> {
                System.err.println("日志上传失败: " + throwable.getMessage());
                return null;
            });
    }

    // 简单配置实现
    static class TestS3Config extends StorageConfig {
        public TestS3Config(Builder builder) {
            super(builder);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder extends StorageConfig.Builder<Builder> {
            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public TestS3Config build() {
                return new TestS3Config(this);
            }
        }
    }
}
```

### 2. 批量日志上传

```java
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BatchLogUploadExample {

    private final S3StorageInterface storage;

    public BatchLogUploadExample(S3StorageInterface storage) {
        this.storage = storage;
    }

    public CompletableFuture<Void> uploadDailyLogs() {
        // 准备一天的日志文件
        Map<String, byte[]> dailyLogs = new HashMap<>();

        // 应用日志
        dailyLogs.put("logs/2024/01/15/application.log",
            generateLogContent("APPLICATION", "Application events"));

        // 错误日志
        dailyLogs.put("logs/2024/01/15/error.log",
            generateLogContent("ERROR", "Error events"));

        // 访问日志
        dailyLogs.put("logs/2024/01/15/access.log",
            generateLogContent("ACCESS", "HTTP access logs"));

        // 性能日志
        dailyLogs.put("logs/2024/01/15/performance.log",
            generateLogContent("PERFORMANCE", "Performance metrics"));

        // 批量上传
        return storage.putObjects(dailyLogs)
            .thenRun(() -> {
                System.out.println("成功上传 " + dailyLogs.size() + " 个日志文件");
            })
            .exceptionally(throwable -> {
                System.err.println("批量上传失败: " + throwable.getMessage());
                return null;
            });
    }

    private byte[] generateLogContent(String type, String description) {
        return String.format("[%s] %s - Sample log content", type, description).getBytes();
    }

    public static void main(String[] args) {
        // 创建存储配置和适配器
        TestS3Config config = TestS3Config.builder()
            .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
            .region("cn-hangzhou")
            .accessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
            .accessKeySecret(System.getenv("OSS_SECRET_ACCESS_KEY"))
            .bucket("batch-logs")
            .build();

        S3StorageInterface storage = S3StorageFactory.createAdapter(config);

        // 执行批量上传
        BatchLogUploadExample example = new BatchLogUploadExample(storage);
        example.uploadDailyLogs().join(); // 等待完成
    }
}
```

## 高级使用示例

### 3. 带重试机制的健壮上传

```java
import org.logx.retry.ExponentialBackoffRetry;
import org.logx.retry.RetryStrategy;
import org.logx.exception.StorageException;
import java.util.concurrent.CompletableFuture;

public class RobustUploadExample {

    private final S3StorageInterface storage;
    private final RetryStrategy retryStrategy;

    public RobustUploadExample(S3StorageInterface storage) {
        this.storage = storage;
        this.retryStrategy = ExponentialBackoffRetry.defaultStrategy();
    }

    public CompletableFuture<Void> uploadWithRetry(String key, byte[] data) {
        return retryUpload(key, data, 1);
    }

    private CompletableFuture<Void> retryUpload(String key, byte[] data, int attempt) {
        return storage.putObject(key, data)
            .handle((result, throwable) -> {
                if (throwable == null) {
                    System.out.println("上传成功: " + key + " (尝试次数: " + attempt + ")");
                    return CompletableFuture.completedFuture(result);
                }

                // 转换为StorageException进行重试判断
                StorageException storageException = convertToStorageException(throwable);

                if (retryStrategy.shouldRetry(storageException, attempt)) {
                    System.out.println("上传失败，准备重试: " + key +
                        " (尝试次数: " + attempt + ", 错误: " + throwable.getMessage() + ")");

                    // 计算延迟时间
                    Duration delay = retryStrategy.calculateDelay(attempt);

                    return CompletableFuture
                        .delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS)
                        .execute(() -> retryUpload(key, data, attempt + 1));
                } else {
                    System.err.println("上传最终失败: " + key +
                        " (总尝试次数: " + attempt + ", 错误: " + throwable.getMessage() + ")");
                    return CompletableFuture.failedFuture(storageException);
                }
            })
            .thenCompose(future -> future);
    }

    private StorageException convertToStorageException(Throwable throwable) {
        if (throwable instanceof StorageException) {
            return (StorageException) throwable;
        }

        // 根据异常类型判断错误类型
        String message = throwable.getMessage().toLowerCase();
        if (message.contains("network") || message.contains("connection")) {
            return StorageException.networkError("Network error during upload", throwable);
        } else if (message.contains("authentication") || message.contains("credential")) {
            return StorageException.authenticationError("Authentication failed", "AUTH_ERROR");
        } else {
            return new StorageException("Upload failed", StorageException.ErrorType.UNKNOWN_ERROR, throwable);
        }
    }

    public static void main(String[] args) {
        // 创建存储适配器
        TestS3Config config = TestS3Config.builder()
            .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
            .region("cn-hangzhou")
            .accessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
            .accessKeySecret(System.getenv("OSS_SECRET_ACCESS_KEY"))
            .bucket("robust-logs")
            .build();

        S3StorageInterface storage = S3StorageFactory.createAdapter(config);
        RobustUploadExample example = new RobustUploadExample(storage);

        // 测试带重试的上传
        String testKey = "test/robust-upload.log";
        byte[] testData = "Test log content for robust upload".getBytes();

        example.uploadWithRetry(testKey, testData)
            .thenRun(() -> System.out.println("健壮上传完成"))
            .exceptionally(throwable -> {
                System.err.println("健壮上传最终失败: " + throwable.getMessage());
                return null;
            })
            .join();
    }
}
```

### 4. 多存储后端适配器管理

```java
import org.logx.storage.StorageBackend;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiBackendStorageManager {

    private final Map<StorageBackend, S3StorageInterface> storageMap;
    private StorageBackend primaryBackend;
    private StorageBackend fallbackBackend;

    public MultiBackendStorageManager() {
        this.storageMap = new ConcurrentHashMap<>();
        this.primaryBackend = StorageBackend.ALIYUN_OSS;
        this.fallbackBackend = StorageBackend.AWS_S3;
    }

    public void registerStorage(StorageBackend backend, StorageConfig config) {
        try {
            S3StorageInterface storage = S3StorageFactory.createAdapter(backend, config);
            storageMap.put(backend, storage);
            System.out.println("注册存储后端: " + backend.getDisplayName());
        } catch (Exception e) {
            System.err.println("注册存储后端失败: " + backend.getDisplayName() +
                ", 错误: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> uploadWithFallback(String key, byte[] data) {
        // 首先尝试主存储
        S3StorageInterface primaryStorage = storageMap.get(primaryBackend);
        if (primaryStorage != null) {
            return primaryStorage.putObject(key, data)
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        System.out.println("主存储上传成功: " + primaryBackend.getDisplayName());
                        return CompletableFuture.completedFuture(result);
                    } else {
                        System.out.println("主存储上传失败，尝试备用存储: " +
                            fallbackBackend.getDisplayName());
                        return uploadToFallback(key, data);
                    }
                })
                .thenCompose(future -> future);
        } else {
            return uploadToFallback(key, data);
        }
    }

    private CompletableFuture<Void> uploadToFallback(String key, byte[] data) {
        S3StorageInterface fallbackStorage = storageMap.get(fallbackBackend);
        if (fallbackStorage != null) {
            return fallbackStorage.putObject(key, data)
                .thenRun(() -> {
                    System.out.println("备用存储上传成功: " + fallbackBackend.getDisplayName());
                })
                .exceptionally(throwable -> {
                    System.err.println("备用存储也上传失败: " + throwable.getMessage());
                    throw new RuntimeException("所有存储后端都上传失败", throwable);
                });
        } else {
            return CompletableFuture.failedFuture(
                new RuntimeException("没有可用的备用存储后端"));
        }
    }

    public CompletableFuture<Map<StorageBackend, Boolean>> healthCheckAll() {
        Map<StorageBackend, CompletableFuture<Boolean>> healthChecks = new HashMap<>();

        for (Map.Entry<StorageBackend, S3StorageInterface> entry : storageMap.entrySet()) {
            StorageBackend backend = entry.getKey();
            S3StorageInterface storage = entry.getValue();

            CompletableFuture<Boolean> healthCheck = storage.healthCheck()
                .exceptionally(throwable -> {
                    System.err.println("健康检查失败: " + backend.getDisplayName() +
                        " - " + throwable.getMessage());
                    return false;
                });

            healthChecks.put(backend, healthCheck);
        }

        // 等待所有健康检查完成
        CompletableFuture<Void> allChecks = CompletableFuture.allOf(
            healthChecks.values().toArray(new CompletableFuture[0])
        );

        return allChecks.thenApply(v -> {
            Map<StorageBackend, Boolean> results = new HashMap<>();
            for (Map.Entry<StorageBackend, CompletableFuture<Boolean>> entry : healthChecks.entrySet()) {
                try {
                    results.put(entry.getKey(), entry.getValue().get());
                } catch (Exception e) {
                    results.put(entry.getKey(), false);
                }
            }
            return results;
        });
    }

    public static void main(String[] args) {
        MultiBackendStorageManager manager = new MultiBackendStorageManager();

        // 注册阿里云OSS
        TestS3Config ossConfig = TestS3Config.builder()
            .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
            .region("cn-hangzhou")
            .accessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
            .accessKeySecret(System.getenv("OSS_SECRET_ACCESS_KEY"))
            .bucket("primary-logs")
            .build();
        manager.registerStorage(StorageBackend.ALIYUN_OSS, ossConfig);

        // 注册AWS S3作为备用
        TestS3Config s3Config = TestS3Config.builder()
            .endpoint("https://s3.us-west-2.amazonaws.com")
            .region("us-west-2")
            .accessKeyId(System.getenv("AWS_ACCESS_KEY_ID"))
            .accessKeySecret(System.getenv("AWS_SECRET_ACCESS_KEY"))
            .bucket("fallback-logs")
            .build();
        manager.registerStorage(StorageBackend.AWS_S3, s3Config);

        // 执行健康检查
        manager.healthCheckAll()
            .thenAccept(results -> {
                System.out.println("存储后端健康状态:");
                results.forEach((backend, healthy) -> {
                    System.out.println("  " + backend.getDisplayName() + ": " +
                        (healthy ? "健康" : "不健康"));
                });
            })
            .join();

        // 测试带容错的上传
        String testKey = "test/multi-backend-upload.log";
        byte[] testData = "Multi-backend upload test".getBytes();

        manager.uploadWithFallback(testKey, testData)
            .thenRun(() -> System.out.println("多后端上传完成"))
            .exceptionally(throwable -> {
                System.err.println("多后端上传失败: " + throwable.getMessage());
                return null;
            })
            .join();
    }
}
```

### 5. 异步日志批处理器

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncLogBatchProcessor {

    private final S3StorageInterface storage;
    private final BlockingQueue<LogEntry> logQueue;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService uploadExecutor;

    private final int batchSize;
    private final Duration flushInterval;
    private final AtomicInteger processedCount;
    private volatile boolean running;

    public AsyncLogBatchProcessor(S3StorageInterface storage,
                                  int batchSize,
                                  Duration flushInterval) {
        this.storage = storage;
        this.batchSize = batchSize;
        this.flushInterval = flushInterval;
        this.logQueue = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.uploadExecutor = Executors.newFixedThreadPool(4);
        this.processedCount = new AtomicInteger(0);
        this.running = false;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;

        // 启动定时刷新任务
        scheduler.scheduleAtFixedRate(
            this::flushBatch,
            flushInterval.toMillis(),
            flushInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );

        // 启动批处理任务
        CompletableFuture.runAsync(this::processBatches, uploadExecutor);

        System.out.println("异步日志批处理器已启动");
    }

    public void stop() {
        running = false;

        // 处理剩余日志
        flushBatch();

        scheduler.shutdown();
        uploadExecutor.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!uploadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                uploadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("异步日志批处理器已停止，总处理数量: " + processedCount.get());
    }

    public void submitLog(String application, String level, String message) {
        if (!running) {
            return;
        }

        LogEntry entry = new LogEntry(
            System.currentTimeMillis(),
            application,
            level,
            message
        );

        try {
            logQueue.offer(entry, 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("提交日志被中断: " + message);
        }
    }

    private void processBatches() {
        List<LogEntry> batch = new ArrayList<>();

        while (running) {
            try {
                // 收集批处理数据
                LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    batch.add(entry);
                }

                // 达到批大小或队列为空时处理批次
                if (batch.size() >= batchSize || (!batch.isEmpty() && logQueue.isEmpty())) {
                    processBatch(new ArrayList<>(batch));
                    batch.clear();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 处理剩余批次
        if (!batch.isEmpty()) {
            processBatch(batch);
        }
    }

    private void flushBatch() {
        if (logQueue.isEmpty()) {
            return;
        }

        List<LogEntry> batch = new ArrayList<>();
        logQueue.drainTo(batch, batchSize);

        if (!batch.isEmpty()) {
            processBatch(batch);
        }
    }

    private void processBatch(List<LogEntry> batch) {
        if (batch.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            Map<String, byte[]> batchData = new HashMap<>();

            for (LogEntry entry : batch) {
                String key = generateLogKey(entry);
                byte[] data = formatLogEntry(entry);
                batchData.put(key, data);
            }

            storage.putObjects(batchData)
                .thenRun(() -> {
                    int processed = processedCount.addAndGet(batch.size());
                    System.out.println("批处理上传成功: " + batch.size() +
                        " 条日志，累计处理: " + processed + " 条");
                })
                .exceptionally(throwable -> {
                    System.err.println("批处理上传失败: " + batch.size() +
                        " 条日志，错误: " + throwable.getMessage());
                    return null;
                });

        }, uploadExecutor);
    }

    private String generateLogKey(LogEntry entry) {
        // 按日期和应用分组
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(entry.timestamp),
            java.time.ZoneOffset.UTC
        );

        return String.format("logs/%s/%04d/%02d/%02d/%s-%d.log",
            entry.application,
            dateTime.getYear(),
            dateTime.getMonthValue(),
            dateTime.getDayOfMonth(),
            entry.level.toLowerCase(),
            entry.timestamp
        );
    }

    private byte[] formatLogEntry(LogEntry entry) {
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(entry.timestamp),
            java.time.ZoneOffset.UTC
        );

        String formattedLog = String.format("[%s] %s %s - %s%n",
            dateTime.toString(),
            entry.level,
            entry.application,
            entry.message
        );

        return formattedLog.getBytes();
    }

    // 日志条目数据类
    private static class LogEntry {
        final long timestamp;
        final String application;
        final String level;
        final String message;

        LogEntry(long timestamp, String application, String level, String message) {
            this.timestamp = timestamp;
            this.application = application;
            this.level = level;
            this.message = message;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 创建存储适配器
        TestS3Config config = TestS3Config.builder()
            .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
            .region("cn-hangzhou")
            .accessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
            .accessKeySecret(System.getenv("OSS_SECRET_ACCESS_KEY"))
            .bucket("async-batch-logs")
            .build();

        S3StorageInterface storage = S3StorageFactory.createAdapter(config);

        // 创建批处理器
        AsyncLogBatchProcessor processor = new AsyncLogBatchProcessor(
            storage,
            10,  // 批大小
            Duration.ofSeconds(5)  // 刷新间隔
        );

        // 启动处理器
        processor.start();

        // 模拟日志产生
        ScheduledExecutorService logGenerator = Executors.newScheduledThreadPool(2);

        // 应用1日志
        logGenerator.scheduleAtFixedRate(() -> {
            processor.submitLog("app1", "INFO", "Application 1 is running normally");
        }, 0, 500, TimeUnit.MILLISECONDS);

        // 应用2日志
        logGenerator.scheduleAtFixedRate(() -> {
            processor.submitLog("app2", "ERROR", "Application 2 encountered an error");
        }, 0, 1000, TimeUnit.MILLISECONDS);

        // 运行30秒
        Thread.sleep(30000);

        // 停止日志生成
        logGenerator.shutdown();

        // 停止批处理器
        processor.stop();

        System.out.println("示例运行完成");
    }
}
```

## 最佳实践总结

### 1. 配置管理

```java
// 推荐：使用配置工厂
S3StorageInterface storage = S3ConfigFactory.createForEnvironment("prod");

// 推荐：环境变量管理敏感信息
.accessKeyId(System.getenv("S3_ACCESS_KEY_ID"))
.accessKeySecret(System.getenv("S3_SECRET_ACCESS_KEY"))
```

### 2. 错误处理

```java
// 推荐：详细的异常处理
storage.putObject(key, data)
    .exceptionally(throwable -> {
        if (throwable instanceof StorageException) {
            StorageException se = (StorageException) throwable;
            switch (se.getErrorType()) {
                case NETWORK_ERROR:
                    // 网络错误处理
                    break;
                case AUTHENTICATION_ERROR:
                    // 认证错误处理
                    break;
                default:
                    // 其他错误处理
            }
        }
        return null;
    });
```

### 3. 性能优化

```java
// 推荐：批量操作
Map<String, byte[]> batchData = collectLogs();
storage.putObjects(batchData);

// 推荐：异步处理
CompletableFuture.allOf(
    storage.putObject(key1, data1),
    storage.putObject(key2, data2),
    storage.putObject(key3, data3)
).thenRun(() -> System.out.println("所有上传完成"));
```

### 4. 监控和日志

```java
// 推荐：添加监控指标
storage.putObject(key, data)
    .thenRun(() -> {
        // 记录成功指标
        metrics.incrementCounter("upload.success");
        logger.info("Upload successful: {}", key);
    })
    .exceptionally(throwable -> {
        // 记录失败指标
        metrics.incrementCounter("upload.failure");
        logger.error("Upload failed: {}", key, throwable);
        return null;
    });
```

这些示例展示了如何在实际项目中使用 S3StorageInterface，从简单的单次上传到复杂的批处理系统。根据具体需求选择合适的模式，并结合错误处理、重试机制和监控来构建健壮的日志存储系统。