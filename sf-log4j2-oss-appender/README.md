# SF Log4j2 OSS Appender

SF OSS存储服务的Log4j2 All-in-One包，包含所有必需依赖，简化引入。

有关详细特性说明，请参考 [根目录文档](../README.md)。

## 🚀 快速开始

### Maven依赖

```xml
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>sf-log4j2-oss-appender</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 配置示例

```xml
<Configuration>
  <Appenders>
    <OSS name="oss" endpoint="https://sf-oss-cn-north-1.sf-oss.com"
                 region="${sys:LOGX_OSS_REGION:-cn-north-1}"
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

### 环境变量配置

```bash
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"
export LOGX_OSS_REGION="cn-north-1"

有关完整配置选项，请参考 [根目录文档](../README.md#可选参数)。

## 📄 许可证

Apache-2.0