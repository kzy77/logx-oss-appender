# logx-producer

核心日志处理引擎，提供高性能异步队列、批处理和存储抽象接口。作为LogX OSS Appender的核心模块，为各种日志框架适配器提供统一的基础设施。

## 功能

- **高性能异步队列** - 基于LMAX Disruptor实现的无锁环形缓冲区，支持纳秒级延迟
- **智能批处理** - 自动合并日志数据，优化网络传输和存储性能
- **资源保护机制** - 固定线程池、低优先级调度，确保不影响业务系统性能
- **统一存储抽象** - 提供标准存储服务接口，支持运行时动态加载具体实现
- **配置管理** - 统一配置参数管理，支持环境变量和多种配置源
- **错误处理** - 完善的错误处理和重试机制，确保数据可靠性

## 模块架构

logx-producer作为核心模块，不直接依赖任何具体的云存储SDK。存储适配器通过Java SPI机制动态加载：

- **logx-s3-adapter** - S3兼容存储适配器（支持AWS S3、阿里云OSS、腾讯云COS等）
- **logx-sf-oss-adapter** - SF OSS存储适配器

## 核心组件

### 队列引擎层
- `DisruptorBatchingQueue` - 高性能异步队列管理
- `ResourceProtectedThreadPool` - 资源保护线程池
- `BatchProcessor` - 智能批处理引擎

### 存储抽象层
- `StorageService` - 统一存储服务接口
- `StorageConfig` - 存储配置管理
- `StorageServiceFactory` - 存储服务工厂

### 配置管理层
- `CommonConfig` - 通用配置参数定义
- `ConfigManager` - 配置管理器

### 错误处理层
- `UnifiedErrorHandler` - 统一错误处理器
- `ErrorContext` - 错误上下文

## 使用方式

### Maven依赖

```xml
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>logx-producer</artifactId>
  <version>0.1.0</version>
</dependency>
```

注意：需要额外引入具体的存储适配器模块：
```xml
<!-- S3兼容存储 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>logx-s3-adapter</artifactId>
  <version>0.1.0</version>
</dependency>

<!-- 或 SF OSS存储 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>logx-sf-oss-adapter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 基本使用示例

```java
// 创建存储配置
StorageConfig config = new StorageConfig.Builder<>()
    .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
    .region("cn-hangzhou")
    .accessKeyId("your-access-key-id")
    .accessKeySecret("your-access-key-secret")
    .bucket("your-bucket-name")
    .keyPrefix("logs/app/")
    .build();

// 通过工厂创建存储服务（自动加载适配器）
StorageService storageService = StorageServiceFactory.createStorageService(config);

// 创建异步引擎
AsyncEngine engine = AsyncEngine.create(storageService);

// 启动引擎
engine.start();

// 发送日志数据
engine.put("Hello, LogX!".getBytes(StandardCharsets.UTF_8));

// 关闭引擎
engine.stop(30, TimeUnit.SECONDS);
```

## 许可证

Apache-2.0