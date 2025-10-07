# Log4j2 OSS Appender

Log4j2框架的OSS Appender，用于将日志异步上传到S3兼容对象存储服务。

有关详细特性说明，请参考 [根目录文档](../README.md)。

## 🚀 快速开始

为简化依赖管理，推荐使用All-in-One包：

### Maven依赖

```xml
<!-- S3兼容存储服务 -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>s3-log4j2-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- 或SF OSS存储服务 -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-log4j2-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 📋 配置说明

有关详细配置说明，请参考 [根目录文档](../README.md#可选参数)。

以下为Log4j2特定配置参数：

## 🏗️ 架构设计

有关详细架构设计说明，请参考 [根目录文档](../README.md)。

### S3 兼容性
- **标准协议**：基于 AWS SDK v2，遵循 S3 API 标准
- **自动适配**：根据 endpoint 自动检测云厂商特性
- **统一接口**：一套代码支持所有 S3 兼容存储

### 高性能架构
- **NDJSON格式**：每行一条JSON记录，支持流式处理，容错性强
- **gzip压缩**：默认启用，典型压缩率70-80%，显著节省存储成本
- **批处理聚合**：按条数/字节/时间窗口智能聚合，平衡延迟与吞吐
- **指数退避重试**：网络异常时自动重试，避免雪崩效应
- **异步队列**：基于高性能队列，无锁设计，超低延迟

## 🔧 最佳实践

有关详细最佳实践说明，请参考 [根目录文档](../README.md)。

### 生产环境推荐配置
```xml
<!-- 高吞吐量场景 -->
<OSS name="oss" ...
     maxQueueSize="131072" maxBatchCount="2000" maxMessageAgeMs="600000" dropWhenQueueFull="false">
```

<!-- 低延迟场景 -->
<OSS name="oss" ...
     maxQueueSize="16384" maxBatchCount="500" maxMessageAgeMs="600000">
```

## 📄 许可证

Apache-2.0