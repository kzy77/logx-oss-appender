# SF Log4j2 OSS Appender

高性能 Log4j2 Appender，将应用日志实时上传到SF OSS对象存储（All-in-One包）。

## 🌟 特性

- **🌐 SF OSS专用**：专为SF OSS存储服务优化
- **🚀 极致性能**：异步批处理、gzip压缩、无锁队列
- **💾 不落盘**：日志直接入内存队列并异步上传
- **🔒 不丢日志**：可配置为生产侧阻塞等待，确保写入
- **🛠️ 可调优**：所有性能参数可按需调整
- **📦 一体化包**：包含所有必需依赖，简化引入

## 🚀 快速开始

### 1) 引入依赖（Maven）

```xml
<!-- SF Log4j2 All-in-One包 -->
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>sf-log4j2-oss-appender</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2) 最简配置

```xml
<Configuration>
  <Appenders>
    <OSS name="oss" endpoint="https://sf-oss-cn-north-1.sf-oss.com"
                 region="${sys:LOG_OSS_REGION:-cn-north-1}"
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
export LOG_OSS_REGION="cn-north-1"
```

## 📄 许可证

Apache-2.0