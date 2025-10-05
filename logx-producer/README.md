# logx-producer

核心日志处理引擎，提供高性能异步队列、批处理和存储抽象接口。

有关详细功能说明和架构设计，请参考 [根目录文档](../README.md)。

## 模块架构

logx-producer作为核心模块，不直接依赖任何具体的云存储SDK。存储适配器通过Java SPI机制动态加载：

- **logx-s3-adapter** - S3兼容存储适配器（支持AWS S3、阿里云OSS、腾讯云COS等）
- **logx-sf-oss-adapter** - SF OSS存储适配器

## 核心组件

### 队列引擎层（2025-10-05架构重构）
- `EnhancedDisruptorBatchingQueue` - 增强的Disruptor批处理队列（合并原DisruptorBatchingQueue和BatchProcessor）
- `ResourceProtectedThreadPool` - 资源保护线程池

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
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

注意：需要额外引入具体的存储适配器模块：
```xml
<!-- S3兼容存储 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>logx-s3-adapter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- 或 SF OSS存储 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>logx-sf-oss-adapter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
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
    .keyPrefix("logs/") // 对象键前缀
    .ossType("SF_OSS") // 存储后端类型，支持SF_OSS、S3等
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

有关完整配置选项，请参考 [根目录文档](../README.md#可选参数)。

## 许可证

Apache-2.0