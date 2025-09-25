# S3 Logback OSS Appender

高性能 Logback Appender，将应用日志实时上传到S3兼容对象存储（All-in-One包）。

## 🌟 特性

- **🌐 S3兼容**：支持AWS S3、阿里云OSS、腾讯云COS、MinIO等S3兼容存储
- **🚀 极致性能**：异步批处理、gzip压缩、无锁队列
- **💾 不落盘**：日志直接入内存队列并异步上传
- **🔒 不丢日志**：可配置为生产侧阻塞等待，确保写入
- **🛠️ 可调优**：所有性能参数可按需调整
- **📦 一体化包**：包含所有必需依赖，简化引入

## 🚀 快速开始

### 1) 引入依赖（Maven）

```xml
<!-- S3 Logback All-in-One包 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>s3-logback-oss-appender</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2) 最简配置（以阿里云OSS为例）

```xml
<configuration>
  <appender name="OSS" class="org.logx.logback.LogbackOSSAppender">
    <endpoint>${LOG_OSS_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}</endpoint>
    <accessKeyId>${LOG_OSS_ACCESS_KEY_ID}</accessKeyId>
    <accessKeySecret>${LOG_OSS_ACCESS_KEY_SECRET}</accessKeySecret>
    <bucket>${LOG_OSS_BUCKET}</bucket>
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
      <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="INFO"><appender-ref ref="OSS"/></root>
</configuration>
```

### 3) 环境变量配置

```bash
export LOG_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOG_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOG_OSS_BUCKET="your-bucket-name"
```

## 📄 许可证

Apache-2.0