# Logback OSS Appender

Logback框架的OSS Appender，用于将日志异步上传到S3兼容对象存储服务。

有关详细特性说明，请参考 [根目录文档](../README.md)。

## 🚀 快速开始

使用两个核心依赖集成：

### Maven依赖

```xml
<dependencies>
    <!-- Logback适配器 -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logback-oss-appender</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- 存储适配器（选择其一） -->
    <dependency>
        <groupId>org.logx</groupId>
        <artifactId>logx-s3-adapter</artifactId>  <!-- S3兼容存储 -->
        <version>1.0.0-SNAPSHOT</version>
        <!-- 或 <artifactId>logx-sf-oss-adapter</artifactId><version>1.0.0-SNAPSHOT</version> SF OSS存储 -->
    </dependency>
</dependencies>
```


## 📋 配置说明

有关详细配置说明，请参考 [根目录文档](../README.md#可选参数)。

以下为Logback特定配置参数：

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
<maxQueueSize>500000</maxQueueSize>
<maxBatchCount>10000</maxBatchCount>
<maxMessageAgeMs>600000</maxMessageAgeMs>
<dropWhenQueueFull>false</dropWhenQueueFull>

<!-- 低延迟场景 -->
<maxQueueSize>50000</maxQueueSize>
<maxBatchCount>1000</maxBatchCount>
<maxMessageAgeMs>600000</maxMessageAgeMs>
```

## 📄 许可证

Apache-2.0