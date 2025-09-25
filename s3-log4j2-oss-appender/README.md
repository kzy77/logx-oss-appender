# S3 Log4j2 OSS Appender

高性能 Log4j2 Appender，将应用日志实时上传到S3兼容对象存储（All-in-One包）。

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
<!-- S3 Log4j2 All-in-One包 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>s3-log4j2-oss-appender</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2) 最简配置（以阿里云OSS为例）

```xml
<Configuration>
  <Appenders>
    <OSS name="oss" endpoint="https://oss-cn-hangzhou.aliyuncs.com"
                 accessKeyId="${sys:LOGX_OSS_ACCESS_KEY_ID}" accessKeySecret="${sys:LOGX_OSS_ACCESS_KEY_SECRET}"
                 bucket="your-bucket">
      <PatternLayout pattern="%d{ISO8601} %level %logger - %msg%n"/>
    </OSS>
  </Appenders>

  <Loggers>
    <Root level="info">
      <AppenderRef ref="oss"/>
    </Root>
  </Loggers>
</Configuration>
```

### 3) 环境变量配置

```bash
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOG_OSS_BUCKET="your-bucket-name"
```

## 📄 许可证

Apache-2.0