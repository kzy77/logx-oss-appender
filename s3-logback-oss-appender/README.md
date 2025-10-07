# S3 Logback OSS Appender

S3兼容存储的Logback All-in-One包，包含所有必需依赖，简化引入。

有关详细特性说明，请参考 [根目录文档](../README.md)。

## 🚀 快速开始

### Maven依赖

```xml
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>s3-logback-oss-appender</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 配置示例

```xml
<configuration>
  <appender name="OSS" class="org.logx.logback.LogbackOSSAppender">
    <endpoint>${LOGX_OSS_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}</endpoint>
    <accessKeyId>${LOGX_OSS_ACCESS_KEY_ID}</accessKeyId>
    <accessKeySecret>${LOGX_OSS_ACCESS_KEY_SECRET}</accessKeySecret>
    <bucket>${LOGX_OSS_BUCKET}</bucket>
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="INFO"><appender-ref ref="OSS"/></root>
</configuration>
```

### 环境变量配置

```bash
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"
```

有关完整配置选项，请参考 [根目录文档](../README.md#可选参数)。

## 📄 许可证

Apache-2.0