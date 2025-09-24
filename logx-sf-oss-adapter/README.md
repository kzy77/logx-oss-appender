# SF OSS 适配器模块

## 模块介绍

SF OSS 适配器模块为 LogX OSS Appender 提供对 SF OSS 存储服务的支持。该模块实现了统一的存储服务接口，将日志数据上传到 SF OSS 存储桶中。

## 架构变更说明 (2025-09-24)

根据最新的架构设计，SF OSS 适配器的职责已简化：
- **只负责具体的上传实现**
- **不再提供putObjects方法，只提供putObject方法**
- **依赖核心层的数据分片处理**

这种设计使得适配器更加简洁，同时将数据分片的控制权集中在核心层，确保所有存储后端的行为一致性。

## 核心组件

### SfOssStorageService
实现了 `StorageService` 接口，提供 SF OSS 存储服务的具体实现。

### SfOssStorageAdapter
SF OSS 存储适配器，基于 SF OSS 特有 SDK 实现的存储适配器，支持 SF OSS 服务。

## 使用方式

在项目中引入该模块依赖：

```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>logx-sf-oss-adapter</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 配置说明

通过统一的配置方式来设置 SF OSS 存储参数：

```properties
# 存储后端类型
logx.storage.backend=SF_OSS

# SF OSS 访问配置
logx.storage.endpoint=https://oss-cn-hangzhou.sfcloud.com
logx.storage.region=cn-hangzhou
logx.storage.accessKeyId=your-access-key-id
logx.storage.accessKeySecret=your-access-key-secret
logx.storage.bucket=your-bucket-name
```

## 设计特点

1. **低耦合**：适配器只关注具体的上传实现，不处理数据分片等通用逻辑
2. **高内聚**：所有 SF OSS 相关的实现都集中在该模块中
3. **易扩展**：通过实现统一的存储服务接口，可以轻松扩展支持其他存储后端
4. **简化实现**：只提供单对象上传方法，降低了代码复杂度