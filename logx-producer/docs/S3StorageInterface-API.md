# S3StorageInterface API 文档

## 概述

`S3StorageInterface` 是 OSS Appender 的核心存储抽象接口，提供统一的 S3 兼容存储操作能力。该接口支持多种 S3 兼容存储后端，包括 AWS S3、阿里云 OSS、腾讯云 COS、华为云 OBS、MinIO 等。

## 核心特性

- **统一接口**: 提供标准化的 S3 操作接口，隐藏不同存储后端的实现细节
- **异步操作**: 所有方法都是非阻塞的，使用 `CompletableFuture` 提供异步支持
- **线程安全**: 接口实现保证并发操作的安全性
- **错误处理**: 统一的异常处理和重试策略
- **批量操作**: 支持单个和批量对象上传

## 接口定义

### 核心方法

#### putObject
```java
CompletableFuture<Void> putObject(String key, byte[] data)
```

**功能**: 异步上传单个对象到 S3 存储

**参数**:
- `key`: 存储对象的键（路径），不能为空或空字符串
- `data`: 要上传的数据内容，不能为空

**返回值**: `CompletableFuture<Void>` - 异步操作结果

**异常**:
- `IllegalArgumentException`: 参数无效时抛出
- `StorageException`: 存储操作失败时抛出

#### putObjects
```java
CompletableFuture<Void> putObjects(Map<String, byte[]> objects)
```

**功能**: 异步批量上传多个对象到 S3 存储

**参数**:
- `objects`: 要上传的对象映射，key 为存储路径，value 为数据内容

**返回值**: `CompletableFuture<Void>` - 异步操作结果

#### healthCheck
```java
CompletableFuture<Boolean> healthCheck()
```

**功能**: 检查存储连接的健康状态

**返回值**: `CompletableFuture<Boolean>` - 健康检查结果

### 信息方法

#### getBackendType
```java
String getBackendType()
```

**功能**: 获取存储后端的类型标识

**返回值**: 存储后端类型字符串（如 "AWS_S3", "ALIYUN_OSS" 等）

#### getBucketName
```java
String getBucketName()
```

**功能**: 获取当前使用的存储桶名称

**返回值**: 存储桶名称

## 配置系统

### StorageConfig

抽象配置基类，定义所有 S3 兼容存储的通用配置参数。

#### 必需配置

- **endpoint**: S3 服务端点 URL
- **region**: 存储区域标识
- **accessKeyId**: 访问密钥 ID
- **accessKeySecret**: 访问密钥 Secret
- **bucket**: 存储桶名称

#### 可选配置

- **pathStyleAccess**: 是否使用路径风格访问（默认：false）
- **connectTimeout**: 连接超时时间（默认：30秒）
- **readTimeout**: 读取超时时间（默认：60秒）
- **maxConnections**: 最大连接数（默认：50）
- **enableSsl**: 是否启用 SSL（默认：true）

#### Builder 模式使用

```java
// 创建具体的配置实现
public class MyS3Config extends StorageConfig {
    public MyS3Config(Builder builder) {
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
        public MyS3Config build() {
            return new MyS3Config(this);
        }
    }
}

// 使用 Builder 创建配置
MyS3Config config = MyS3Config.builder()
    .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
    .region("cn-hangzhou")
    .accessKeyId("your-access-key")
    .accessKeySecret("your-secret-key")
    .bucket("your-bucket")
    .pathStyleAccess(true)
    .connectTimeout(Duration.ofSeconds(30))
    .build();
```

## 存储后端支持

### StorageBackend 枚举

支持的存储后端类型：

- **ALIYUN_OSS**: 阿里云对象存储 OSS
- **AWS_S3**: Amazon Web Services S3
- **MINIO**: MinIO 开源对象存储
- **TENCENT_COS**: 腾讯云对象存储 COS
- **HUAWEI_OBS**: 华为云对象存储 OBS
- **GENERIC_S3**: 通用 S3 兼容存储

### 自动检测

系统支持基于端点 URL 和区域信息自动检测存储后端类型：

```java
// 基于端点检测
StorageBackend backend = StorageBackend.detectFromEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
// 结果: ALIYUN_OSS

// 基于配置检测
StorageBackend backend = StorageBackend.detectFromConfig(endpoint, region);
```

## 工厂模式

### S3StorageFactory

提供统一的适配器创建接口：

```java
// 指定后端类型创建
S3StorageInterface storage = S3StorageFactory.createAdapter(StorageBackend.ALIYUN_OSS, config);

// 自动检测后端类型创建
S3StorageInterface storage = S3StorageFactory.createAdapter(config);

// 检测后端类型
StorageBackend backend = S3StorageFactory.detectBackend(config);

// 验证兼容性
boolean compatible = S3StorageFactory.isCompatible(StorageBackend.AWS_S3, config);
```

## 错误处理

### StorageException

统一的存储异常类型，支持错误分类：

```java
public enum ErrorType {
    NETWORK_ERROR,          // 网络连接错误
    AUTHENTICATION_ERROR,   // 认证失败错误
    SERVER_ERROR,          // 服务器内部错误
    CLIENT_ERROR,          // 客户端请求错误
    CONFIGURATION_ERROR,   // 配置错误
    UNKNOWN_ERROR          // 未知错误
}
```

### 便捷创建方法

```java
// 网络错误
StorageException.networkError("Connection timeout", cause);

// 认证错误
StorageException.authenticationError("Invalid credentials", "AUTH_001");

// 服务器错误
StorageException.serverError("Internal server error", "SERVER_500", cause);

// 客户端错误
StorageException.clientError("Bad request", "CLIENT_400");

// 配置错误
StorageException.configurationError("Invalid endpoint URL");
```

## 重试策略

### RetryStrategy 接口

定义重试逻辑的抽象接口：

```java
public interface RetryStrategy {
    boolean shouldRetry(StorageException exception, int attemptNumber);
    Duration calculateDelay(int attemptNumber);
    int getMaxRetries();
    String getStrategyName();
}
```

### ExponentialBackoffRetry

指数退避重试策略实现：

```java
// 使用默认策略
ExponentialBackoffRetry strategy = ExponentialBackoffRetry.defaultStrategy();

// 快速重试策略
ExponentialBackoffRetry strategy = ExponentialBackoffRetry.fastRetry();

// 保守重试策略
ExponentialBackoffRetry strategy = ExponentialBackoffRetry.conservativeRetry();

// 自定义策略
ExponentialBackoffRetry strategy = new ExponentialBackoffRetry(
    5,                          // 最大重试次数
    Duration.ofMillis(100),     // 初始延迟
    2.0,                        // 延迟倍数
    Duration.ofSeconds(30),     // 最大延迟
    0.1                         // 抖动因子
);
```

## 使用场景

### 日志上传场景

```java
// 配置存储
MyS3Config config = MyS3Config.builder()
    .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
    .region("cn-hangzhou")
    .accessKeyId(System.getenv("OSS_ACCESS_KEY"))
    .accessKeySecret(System.getenv("OSS_SECRET_KEY"))
    .bucket("app-logs")
    .build();

// 创建存储适配器
S3StorageInterface storage = S3StorageFactory.createAdapter(config);

// 上传日志文件
String logKey = "logs/2024/01/15/app.log";
byte[] logData = "log content".getBytes();

storage.putObject(logKey, logData)
    .thenRun(() -> System.out.println("Log uploaded successfully"))
    .exceptionally(throwable -> {
        System.err.println("Upload failed: " + throwable.getMessage());
        return null;
    });
```

### 批量上传场景

```java
// 准备批量数据
Map<String, byte[]> logFiles = new HashMap<>();
logFiles.put("logs/app1.log", "app1 log content".getBytes());
logFiles.put("logs/app2.log", "app2 log content".getBytes());
logFiles.put("logs/app3.log", "app3 log content".getBytes());

// 批量上传
storage.putObjects(logFiles)
    .thenRun(() -> System.out.println("Batch upload completed"))
    .exceptionally(throwable -> {
        System.err.println("Batch upload failed: " + throwable.getMessage());
        return null;
    });
```

### 健康检查场景

```java
// 定期健康检查
storage.healthCheck()
    .thenAccept(healthy -> {
        if (healthy) {
            System.out.println("Storage is healthy");
        } else {
            System.err.println("Storage health check failed");
        }
    });
```

## 最佳实践

1. **配置管理**: 使用环境变量或配置文件管理敏感信息
2. **异常处理**: 根据异常类型实现不同的处理策略
3. **重试策略**: 选择合适的重试策略，避免过度重试
4. **资源管理**: 合理设置连接池大小和超时时间
5. **监控告警**: 实现存储操作的监控和告警机制
6. **测试验证**: 使用模拟适配器进行单元测试

## 版本兼容性

- **Java**: 最低 Java 8，向上兼容 Java 11/17/21
- **S3 API**: 兼容 S3 v4 签名标准
- **存储服务**: 支持主流 S3 兼容存储服务

## 后续扩展

当前接口为模拟实现，具体的存储适配器将在后续故事中逐步实现：

- 故事 1.3: 阿里云 OSS 适配器实现
- 故事 1.4: AWS S3 适配器实现
- 后续故事: MinIO、腾讯云 COS、华为云 OBS 适配器实现