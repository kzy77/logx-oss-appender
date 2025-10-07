# SF Log4j OSS Appender

SF OSS存储服务的Log4j 1.x All-in-One包，包含所有必需依赖，简化引入。

有关详细特性说明，请参考 [根目录文档](../README.md)。

## 🚀 快速开始

### Maven依赖

```xml
<dependency>
  <groupId>org.logx</groupId>
  <artifactId>sf-log4j-oss-appender</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 配置示例

```xml
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">
  <appender name="oss" class="org.logx.log4j.Log4jOSSAppender">
    <param name="endpoint" value="${LOGX_OSS_ENDPOINT:-https://sf-oss-cn-north-1.sf-oss.com}"/>
    <param name="region" value="${LOGX_OSS_REGION:-cn-north-1}"/>
    <param name="accessKeyId" value="${sys:LOGX_OSS_ACCESS_KEY_ID}"/>
    <param name="accessKeySecret" value="${sys:LOGX_OSS_ACCESS_KEY_SECRET}"/>
    <param name="bucket" value="your-bucket"/>
    <layout class="org.apache.log4j.PatternLayout">
      <param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p %c{1.} - %m%ex{full}"/>
    </layout>
  </appender>
  <root>
    <priority value="info"/>
    <appender-ref ref="oss"/>
  </root>
</log4j:configuration>
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