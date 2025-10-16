# SF OSS 适配器模块

> **📘 完整文档**: 详细特性、配置说明、使用示例请参考 [项目主文档](../README.md)
> 本文档仅包含logx-sf-oss-adapter模块特定说明

SF OSS 适配器模块为 LogX OSS Appender 提供对 SF OSS 存储服务的支持。

## 架构变更说明 (2025-09-24)

根据最新的架构设计，SF OSS 适配器的职责已简化：
- **只负责具体的上传实现**
- **不再提供putObjects方法，只提供putObject方法**
- **依赖核心层的数据分片处理**

这种设计使得适配器更加简洁，同时将数据分片的控制权集中在核心层，确保所有存储后端的行为一致性。

## 核心组件

### SfOssStorageServiceAdapter
实现了 `StorageService` 接口，提供 SF OSS 存储服务的具体实现。基于 SF OSS 特有 SDK 实现。

## 使用方式

完整的使用指南请参考 [主文档](../README.md)

### Maven依赖

```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>logx-sf-oss-adapter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

详细配置参数请参考 [主文档配置参数章节](../README.md#配置参数说明)

### SF OSS适配器特定说明

- 通过Java SPI机制自动加载（配置文件：`META-INF/services/org.logx.storage.StorageService`）
- 存储类型：`ossType=SF_OSS`
- 默认region：`US`（SF OSS要求）

## 设计特点

1. **低耦合**：适配器只关注具体的上传实现，不处理数据分片等通用逻辑
2. **高内聚**：所有 SF OSS 相关的实现都集中在该模块中
3. **易扩展**：通过实现统一的存储服务接口，可以轻松扩展支持其他存储后端
4. **简化实现**：只提供单对象上传方法，降低了代码复杂度

## 许可证

Apache-2.0
