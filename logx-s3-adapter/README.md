# S3 适配器模块

## 模块介绍

S3 适配器模块为 LogX OSS Appender 提供对 AWS S3、阿里云OSS、腾讯云COS 等 S3 兼容存储服务的支持。该模块实现了统一的存储服务接口，将日志数据上传到 S3 兼容的存储桶中。

## 架构变更说明 (2025-09-24)

根据最新的架构设计，S3 适配器的职责已简化：
- **只负责具体的上传实现**
- **不再处理数据分片逻辑**
- **依赖核心层的数据分片处理**
- **不再提供putObjects方法，只提供putObject方法**

这种设计使得适配器更加简洁，同时将数据分片的控制权集中在核心层，确保所有存储后端的行为一致性。

## 核心组件

### S3StorageService
实现了 `StorageService` 接口，提供 S3 兼容存储服务的具体实现。

### S3StorageAdapter
S3 存储适配器，基于 AWS SDK 实现的存储适配器，支持所有 S3 兼容的存储服务。

## 使用方式

在项目中引入该模块依赖：

```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>logx-s3-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

通过统一的配置方式来设置 S3 兼容存储参数：

```properties
# 存储后端类型
logx.storage.backend=S3

# S3 访问配置
logx.storage.endpoint=https://s3.cn-hangzhou.aliyuncs.com
logx.storage.region=cn-hangzhou
logx.storage.accessKeyId=your-access-key-id
logx.storage.accessKeySecret=your-access-key-secret
logx.storage.bucket=your-bucket-name
```

## 设计特点

1. **低耦合**：适配器只关注具体的上传实现，不处理数据分片等通用逻辑
2. **高内聚**：所有 S3 相关的实现都集中在该模块中
3. **易扩展**：通过实现统一的存储服务接口，可以轻松扩展支持其他存储后端
4. **简化实现**：移除了复杂的数据分片逻辑，只提供单对象上传方法
5. **兼容性强**：基于 AWS SDK，支持所有 S3 兼容的存储服务