# 存储后端配置示例

## 概述

本文档提供各种 S3 兼容存储后端的配置示例，帮助开发者快速集成不同的云存储服务。

## 阿里云 OSS 配置

### 基础配置

```java
// 基础 OSS 配置
AliyunOssConfig config = AliyunOssConfig.builder()
    .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
    .region("cn-hangzhou")
    .accessKeyId("YOUR_ACCESS_KEY_ID")
    .accessKeySecret("YOUR_ACCESS_KEY_SECRET")
    .bucket("your-bucket-name")
    .build();
```

### 完整配置

```java
// 完整 OSS 配置（包含可选参数）
AliyunOssConfig config = AliyunOssConfig.builder()
    .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
    .region("cn-hangzhou")
    .accessKeyId(System.getenv("ALIYUN_ACCESS_KEY_ID"))
    .accessKeySecret(System.getenv("ALIYUN_ACCESS_KEY_SECRET"))
    .bucket("app-logs-bucket")
    .pathStyleAccess(true)
    .connectTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(60))
    .maxConnections(100)
    .enableSsl(true)
    .build();
```

### 多地域配置

```java
// 不同地域的 OSS 配置
Map<String, AliyunOssConfig> regionConfigs = Map.of(
    "hangzhou", AliyunOssConfig.builder()
        .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
        .region("cn-hangzhou")
        .build(),

    "beijing", AliyunOssConfig.builder()
        .endpoint("https://oss-cn-beijing.aliyuncs.com")
        .region("cn-beijing")
        .build(),

    "shanghai", AliyunOssConfig.builder()
        .endpoint("https://oss-cn-shanghai.aliyuncs.com")
        .region("cn-shanghai")
        .build()
);
```

## AWS S3 配置

### 基础配置

```java
// 基础 S3 配置
AwsS3Config config = AwsS3Config.builder()
    .endpoint("https://s3.amazonaws.com")
    .region("us-west-2")
    .accessKeyId("YOUR_AWS_ACCESS_KEY_ID")
    .accessKeySecret("YOUR_AWS_SECRET_ACCESS_KEY")
    .bucket("your-s3-bucket")
    .build();
```

### 区域特定配置

```java
// 指定区域的 S3 配置
AwsS3Config config = AwsS3Config.builder()
    .endpoint("https://s3.us-west-2.amazonaws.com")
    .region("us-west-2")
    .accessKeyId(System.getenv("AWS_ACCESS_KEY_ID"))
    .accessKeySecret(System.getenv("AWS_SECRET_ACCESS_KEY"))
    .bucket("application-logs")
    .pathStyleAccess(false)  // S3 默认使用虚拟主机风格
    .connectTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofMinutes(2))
    .maxConnections(50)
    .enableSsl(true)
    .build();
```

### 多区域配置

```java
// 不同区域的 S3 配置
Map<String, AwsS3Config> regionConfigs = Map.of(
    "us-west-2", AwsS3Config.builder()
        .endpoint("https://s3.us-west-2.amazonaws.com")
        .region("us-west-2")
        .build(),

    "us-east-1", AwsS3Config.builder()
        .endpoint("https://s3.us-east-1.amazonaws.com")
        .region("us-east-1")
        .build(),

    "eu-west-1", AwsS3Config.builder()
        .endpoint("https://s3.eu-west-1.amazonaws.com")
        .region("eu-west-1")
        .build()
);
```

## MinIO 配置

### 本地开发配置

```java
// 本地 MinIO 配置
MinioConfig config = MinioConfig.builder()
    .endpoint("http://localhost:9000")
    .region("us-east-1")  // MinIO 默认区域
    .accessKeyId("minioadmin")
    .accessKeySecret("minioadmin")
    .bucket("test-bucket")
    .pathStyleAccess(true)  // MinIO 使用路径风格
    .enableSsl(false)       // 本地开发通常不使用 SSL
    .build();
```

### 生产环境配置

```java
// 生产环境 MinIO 配置
MinioConfig config = MinioConfig.builder()
    .endpoint("https://minio.example.com")
    .region("us-east-1")
    .accessKeyId(System.getenv("MINIO_ACCESS_KEY"))
    .accessKeySecret(System.getenv("MINIO_SECRET_KEY"))
    .bucket("production-logs")
    .pathStyleAccess(true)
    .connectTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(90))
    .maxConnections(25)
    .enableSsl(true)
    .build();
```

### 集群配置

```java
// MinIO 集群配置
List<MinioConfig> clusterConfigs = List.of(
    MinioConfig.builder()
        .endpoint("https://minio1.example.com")
        .bucket("logs-shard-1")
        .build(),

    MinioConfig.builder()
        .endpoint("https://minio2.example.com")
        .bucket("logs-shard-2")
        .build(),

    MinioConfig.builder()
        .endpoint("https://minio3.example.com")
        .bucket("logs-shard-3")
        .build()
);
```

## 腾讯云 COS 配置

### 基础配置

```java
// 基础 COS 配置
TencentCosConfig config = TencentCosConfig.builder()
    .endpoint("https://cos.us.myqcloud.com")
    .region("US")
    .accessKeyId("YOUR_SECRET_ID")
    .accessKeySecret("YOUR_SECRET_KEY")
    .bucket("your-bucket-name")
    .build();
```

### 完整配置

```java
// 完整 COS 配置
TencentCosConfig config = TencentCosConfig.builder()
    .endpoint("https://cos.us.myqcloud.com")
    .region("US")
    .accessKeyId(System.getenv("TENCENT_SECRET_ID"))
    .accessKeySecret(System.getenv("TENCENT_SECRET_KEY"))
    .bucket("application-logs-1234567890")  // COS bucket 包含 APPID
    .pathStyleAccess(true)
    .connectTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(60))
    .maxConnections(50)
    .enableSsl(true)
    .build();
```

## 华为云 OBS 配置

### 基础配置

```java
// 基础 OBS 配置
HuaweiObsConfig config = HuaweiObsConfig.builder()
    .endpoint("https://obs.cn-north-1.myhuaweicloud.com")
    .region("cn-north-1")
    .accessKeyId("YOUR_ACCESS_KEY_ID")
    .accessKeySecret("YOUR_SECRET_ACCESS_KEY")
    .bucket("your-obs-bucket")
    .build();
```

### 完整配置

```java
// 完整 OBS 配置
HuaweiObsConfig config = HuaweiObsConfig.builder()
    .endpoint("https://obs.cn-north-1.myhuaweicloud.com")
    .region("cn-north-1")
    .accessKeyId(System.getenv("HUAWEI_ACCESS_KEY_ID"))
    .accessKeySecret(System.getenv("HUAWEI_SECRET_ACCESS_KEY"))
    .bucket("app-logs-bucket")
    .pathStyleAccess(true)
    .connectTimeout(Duration.ofSeconds(30))
    .readTimeout(Duration.ofSeconds(60))
    .maxConnections(50)
    .enableSsl(true)
    .build();
```

## 通用 S3 配置

### 自定义 S3 服务

```java
// 自定义 S3 兼容服务配置
GenericS3Config config = GenericS3Config.builder()
    .endpoint("https://custom-s3.example.com")
    .region("custom-region")
    .accessKeyId("CUSTOM_ACCESS_KEY")
    .accessKeySecret("CUSTOM_SECRET_KEY")
    .bucket("custom-bucket")
    .pathStyleAccess(true)
    .build();
```

## 环境特定配置

### 开发环境

```java
// 开发环境配置模板
public class DevelopmentConfig {
    public static StorageConfig createConfig() {
        return TestS3Config.builder()
            .endpoint("http://localhost:9000")  // 本地 MinIO
            .region("us-east-1")
            .accessKeyId("minioadmin")
            .accessKeySecret("minioadmin")
            .bucket("dev-logs")
            .pathStyleAccess(true)
            .enableSsl(false)
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .maxConnections(10)
            .build();
    }
}
```

### 测试环境

```java
// 测试环境配置模板
public class TestingConfig {
    public static StorageConfig createConfig() {
        return TestS3Config.builder()
            .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
            .region("cn-hangzhou")
            .accessKeyId(System.getenv("TEST_OSS_ACCESS_KEY"))
            .accessKeySecret(System.getenv("TEST_OSS_SECRET_KEY"))
            .bucket("test-logs-bucket")
            .pathStyleAccess(true)
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(45))
            .maxConnections(20)
            .build();
    }
}
```

### 生产环境

```java
// 生产环境配置模板
public class ProductionConfig {
    public static StorageConfig createConfig() {
        return ProductionS3Config.builder()
            .endpoint(System.getenv("S3_ENDPOINT"))
            .region(System.getenv("S3_REGION"))
            .accessKeyId(System.getenv("S3_ACCESS_KEY_ID"))
            .accessKeySecret(System.getenv("S3_SECRET_ACCESS_KEY"))
            .bucket(System.getenv("S3_BUCKET"))
            .pathStyleAccess(Boolean.parseBoolean(
                System.getenv("S3_PATH_STYLE_ACCESS")))
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(2))
            .maxConnections(100)
            .enableSsl(true)
            .build();
    }
}
```

## 配置最佳实践

### 1. 安全性

```java
// 推荐：使用环境变量
.accessKeyId(System.getenv("S3_ACCESS_KEY_ID"))
.accessKeySecret(System.getenv("S3_SECRET_ACCESS_KEY"))

// 避免：硬编码敏感信息
.accessKeyId("AKIAIOSFODNN7EXAMPLE")  // 不推荐
```

### 2. 配置验证

```java
// 配置创建后立即验证
StorageConfig config = MyS3Config.builder()
    .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
    .region("cn-hangzhou")
    .accessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
    .accessKeySecret(System.getenv("OSS_SECRET_ACCESS_KEY"))
    .bucket("logs-bucket")
    .build();

// 验证配置
try {
    config.validateConfig();
    System.out.println("Configuration is valid");
} catch (IllegalArgumentException e) {
    System.err.println("Invalid configuration: " + e.getMessage());
}
```

### 3. 配置工厂

```java
// 配置工厂模式
public class S3ConfigFactory {

    public static StorageConfig createForEnvironment(String env) {
        switch (env.toLowerCase()) {
            case "dev":
                return createDevelopmentConfig();
            case "test":
                return createTestConfig();
            case "prod":
                return createProductionConfig();
            default:
                throw new IllegalArgumentException("Unknown environment: " + env);
        }
    }

    private static StorageConfig createDevelopmentConfig() {
        return DevS3Config.builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .accessKeyId("minioadmin")
            .accessKeySecret("minioadmin")
            .bucket("dev-logs")
            .build();
    }

    // ... 其他环境配置方法
}
```



## 配置参数参考

| 参数 | 类型 | 必需 | 默认值 | 说明 |
|------|------|------|--------|------|
| endpoint | String | ✓ | - | S3服务端点URL |
| region | String | ✓ | - | 存储区域标识 |
| accessKeyId | String | ✓ | - | 访问密钥ID |
| accessKeySecret | String | ✓ | - | 访问密钥Secret |
| bucket | String | ✓ | - | 存储桶名称 |
| pathStyleAccess | boolean | ✗ | false | 路径风格访问 |
| connectTimeout | Duration | ✗ | 30s | 连接超时时间 |
| readTimeout | Duration | ✗ | 60s | 读取超时时间 |
| maxConnections | int | ✗ | 50 | 最大连接数 |
| enableSsl | boolean | ✗ | true | 启用SSL |

## 故障排查

### 常见配置错误

1. **端点URL格式错误**
   ```java
   // 错误
   .endpoint("oss-cn-hangzhou.aliyuncs.com")  // 缺少协议

   // 正确
   .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
   ```

2. **区域不匹配**
   ```java
   // 错误：端点和区域不匹配
   .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
   .region("cn-beijing")  // 应该是 cn-hangzhou
   ```

3. **路径风格设置错误**
   ```java
   // AWS S3 通常使用虚拟主机风格
   .pathStyleAccess(false)  // AWS S3

   // OSS/MinIO 通常使用路径风格
   .pathStyleAccess(true)   // 阿里云OSS/MinIO
   ```

### 配置调试

```java
// 启用详细日志进行调试
StorageConfig config = MyS3Config.builder()
    .endpoint("https://oss-cn-hangzhou.aliyuncs.com")
    .region("cn-hangzhou")
    .accessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
    .accessKeySecret(System.getenv("OSS_SECRET_ACCESS_KEY"))
    .bucket("test-bucket")
    .build();

// 打印配置信息（注意敏感信息会被掩码）
System.out.println("Config: " + config.toString());

// 验证配置
config.validateConfig();

// 测试连接
S3StorageInterface storage = S3StorageFactory.createAdapter(config);
storage.healthCheck()
    .thenAccept(healthy -> {
        if (healthy) {
            System.out.println("Connection successful");
        } else {
            System.err.println("Connection failed");
        }
    });
```