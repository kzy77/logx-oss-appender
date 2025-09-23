# 编码标准 - OSS Appender

## 概述

本文档定义了OSS Appender项目的编码标准和规范，确保代码的一致性、可维护性和高质量。

## Java编码规范

### 基础规范
- **JDK版本**: Java 8+ (兼容性优先)
- **编码格式**: UTF-8
- **行结束符**: LF (Unix风格)
- **缩进**: 4个空格，禁用Tab
- **行长度**: 120字符

### 命名规范

#### 包命名
```java
// 统一包命名前缀
org.logx.*                      // 核心包
org.logx.logback.*              // Logback适配器
org.logx.log4j2.*               // Log4j/Log4j2适配器
org.logx.adapter.*              // 适配器实现包
```

#### 类命名
```java
// 适配器命名规范
Log4j1xBridge                   // Log4j 1.x桥接器
Log4j2Bridge                    // Log4j2桥接器
LogbackBridge                   // Logback桥接器

// 核心组件命名
DisruptorBatchingQueue          // 队列管理
S3StorageInterface              // 存储接口
BatchProcessor                  // 批处理器
```

#### 配置Key统一规范
```java
// 所有框架使用统一的配置前缀和命名
public static final String S3_BUCKET = "s3.bucket";
public static final String S3_KEY_PREFIX = "s3.keyPrefix";
public static final String S3_REGION = "s3.region";
public static final String BATCH_SIZE = "batch.size";
public static final String FLUSH_INTERVAL = "batch.flushInterval";
```

### 代码组织

#### 依赖注入原则
```java
// 优先使用构造器注入
public class LogbackBridge {
    private final S3StorageInterface s3Storage;
    private final DisruptorBatchingQueue queue;

    public LogbackBridge(S3StorageInterface s3Storage,
                        DisruptorBatchingQueue queue) {
        this.s3Storage = requireNonNull(s3Storage);
        this.queue = requireNonNull(queue);
    }
}
```

#### 错误处理规范
```java
// 统一错误处理模式
public class BatchProcessor {
    private static final int MAX_RETRIES = 3;

    public void process(LogEvent event) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                doProcess(event);
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    // 记录失败并丢弃，保护系统稳定
                    logFailure(event, e);
                    return;
                }
                backoff(attempt);
            }
        }
    }
}
```

### 性能编码规范

#### 内存管理
```java
// 避免不必要的对象创建
public class LogEventPool {
    private final ThreadLocal<StringBuilder> stringBuilder =
        ThreadLocal.withInitial(() -> new StringBuilder(1024));

    public String format(LogEvent event) {
        StringBuilder sb = stringBuilder.get();
        sb.setLength(0); // 重用StringBuilder
        // 格式化逻辑
        return sb.toString();
    }
}
```

#### 线程安全规范
```java
// 使用final字段和不可变对象
public final class S3Configuration {
    private final String bucket;
    private final String keyPrefix;
    private final int batchSize;

    // 不可变对象模式
    public S3Configuration(Builder builder) {
        this.bucket = requireNonNull(builder.bucket);
        this.keyPrefix = builder.keyPrefix;
        this.batchSize = builder.batchSize;
    }
}
```

## 资源保护编码规范

### 线程池管理
```java
// 固定线程池，防止资源无限扩张
public class ThreadPoolManager {
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;

    private final ExecutorService executor = new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAX_POOL_SIZE,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "logx-oss-appender-worker");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY); // 低优先级
                return t;
            }
        }
    );
}
```

### CPU让出机制
```java
// 在密集操作中主动让出CPU
public class BatchProcessor {
    public void processBatch(List<LogEvent> events) {
        for (int i = 0; i < events.size(); i++) {
            processEvent(events.get(i));

            // 每处理10个事件让出CPU
            if (i % 10 == 0) {
                Thread.yield();
            }
        }
    }
}
```

## 测试编码规范

### 单元测试规范
```java
// 测试类命名：ClassName + Test
public class LogbackBridgeTest {

    @Test
    public void shouldProcessLogEventSuccessfully() {
        // Given - 使用Builder模式构建测试数据
        LogEvent event = LogEvent.builder()
            .message("test message")
            .timestamp(System.currentTimeMillis())
            .build();

        // When
        adapter.append(event);

        // Then - 使用AssertJ进行断言
        assertThat(queue.size()).isEqualTo(1);
    }
}
```

### 集成测试规范
```java
// 集成测试使用TestContainers
@Testcontainers
public class S3IntegrationTest {

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    ).withServices(LocalStackContainer.Service.S3);

    @Test
    public void shouldUploadLogsToS3() {
        // 测试完整的S3上传流程
    }
}
```

## 文档规范

### JavaDoc规范
```java
/**
 * S3兼容存储的统一接口抽象
 *
 * <p>支持AWS S3、阿里云OSS、MinIO等S3兼容存储服务，
 * 通过统一接口提供日志数据的异步上传能力。
 *
 * <p>实现类必须保证线程安全，支持并发操作。
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public interface S3StorageInterface {

    /**
     * 异步上传日志数据到指定的S3存储位置
     *
     * @param key 存储键，不能为空
     * @param data 日志数据，不能为空
     * @return CompletableFuture 异步上传结果
     * @throws IllegalArgumentException 如果参数为空
     */
    CompletableFuture<Void> uploadAsync(String key, byte[] data);
}
```

## 版本控制规范

### Git提交规范
```bash
# 提交信息格式
feat(core): 实现S3存储接口抽象
fix(log4j): 修复配置解析空指针异常
docs(readme): 更新安装指南
test(integration): 添加S3集成测试
refactor(queue): 优化Disruptor队列性能
```

### 分支管理规范
- `main`: 主分支，生产就绪代码
- `develop`: 开发分支，集成最新功能
- `feature/*`: 功能分支
- `hotfix/*`: 热修复分支
- `release/*`: 发布分支

## 性能监控代码规范

### 指标收集
```java
// 统一的性能指标收集
public class PerformanceMetrics {
    private final Counter processedEvents = Counter.build()
        .name("oss_appender_events_processed_total")
        .help("Total processed log events")
        .register();

    private final Histogram processingLatency = Histogram.build()
        .name("oss_appender_processing_latency_seconds")
        .help("Log event processing latency")
        .register();
}
```

这些编码标准确保OSS Appender项目的**简洁性、高性能和可切换性**目标得以实现，同时保持代码的一致性和可维护性。