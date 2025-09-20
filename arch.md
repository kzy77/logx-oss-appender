# OSS Appender 架构文档

## 概述

OSS Appender 是一组高性能日志上传组件，支持将应用日志实时上传到阿里云 OSS。目前包含以下实现：

1. **log-java-producer**：基础日志生产者模块。
2. **log4j-oss-appender**：Log4j 版本的 OSS Appender。
3. **log4j2-oss-appender**：Log4j2 版本的 OSS Appender。
4. **logback-oss-appender**：Logback 版本的 OSS Appender。

## 核心特性

- **不落盘**：日志直接进入内存队列并异步上传，避免磁盘 I/O 开销。
- **高吞吐**：支持批处理、压缩和连接复用，提升上传效率。
- **可靠性**：可配置为生产侧阻塞等待，确保日志不丢失。
- **灵活性**：支持多种配置参数，如队列大小、批量条数/字节、刷新间隔、重试与退避策略等。

## 架构设计

### 1. 日志生产者模块 (log-java-producer)

- **功能**：提供基础的日志生产和队列管理功能。
- **依赖**：无特定日志框架依赖，可作为其他模块的基础。

### 2. Log4j OSS Appender (log4j-oss-appender)

- **功能**：将日志通过 Log4j 框架上传到 OSS。
- **配置**：通过 `log4j2.xml` 文件配置，支持动态参数注入（如通过环境变量）。
- **特点**：
  - 支持异步批处理。
  - 提供上传回调监听。

### 3. Log4j2 OSS Appender (log4j2-oss-appender)

- **功能**：将日志通过 Log4j2 框架上传到 OSS。
- **配置**：通过 `log4j2.xml` 文件配置，支持插件自动发现。
- **特点**：
  - 内置 `log4j2.component.properties` 指定包扫描路径。
  - 支持 GZIP 压缩和指数退避重试。

### 4. Logback OSS Appender (logback-oss-appender)

- **功能**：将日志通过 Logback 框架上传到 OSS。
- **配置**：通过 `logback.xml` 文件配置，支持 JSON Lines 格式输出。
- **特点**：
  - 提供 `AsyncBatchSender` 和 `AliyunOssUploader` 组件。
  - 支持动态参数注入（如通过环境变量）。

## 技术实现

### 内存队列

- 所有模块均使用内存队列缓冲日志事件，避免直接磁盘写入。
- 队列满时可配置为阻塞或丢弃日志。

### 批处理与压缩

- 日志按条数、字节数或时间窗口触发批量上传。
- 支持 GZIP 压缩以减少网络传输开销。

### 上传与重试

- 使用阿里云 OSS SDK 进行上传。
- 支持指数退避重试策略，确保在网络波动时仍能完成上传。

## 生产建议

1. **安全性**：
   - 使用 STS 临时密钥和 RAM 最小权限策略。
   - 避免硬编码敏感信息，推荐通过环境变量或 JVM 参数传递。

2. **性能优化**：
   - 选择就近地域的 OSS Endpoint，并开启内网/专线访问（如可用）。
   - 合理配置队列大小和批量参数，避免内存溢出或上传延迟。

3. **日志管理**：
   - 按业务/环境区分 `objectPrefix`，便于检索。
   - 监控队列状态和上传成功率，及时发现异常。

## 许可证

所有模块均采用 Apache-2.0 许可证。
