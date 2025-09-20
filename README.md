# OSS Appender

一个高性能日志上传组件套件，支持将日志异步批量上传到阿里云OSS和AWS S3兼容的对象存储服务。

## 项目概述

OSS Appender 为Java应用程序提供了一套完整的日志上传解决方案，包含四个核心模块：

- **[log-java-producer](https://github.com/kzy77/log-java-producer)** - 核心基础模块，提供日志生产和队列管理
- **[log4j-oss-appender](https://github.com/kzy77/log4j-oss-appender)** - Log4j 1.x版本的OSS Appender
- **[log4j2-oss-appender](https://github.com/kzy77/log4j2-oss-appender)** - Log4j2版本的OSS Appender
- **[logback-oss-appender](https://github.com/kzy77/logback-oss-appender)** - Logback版本的OSS Appender

## 特性

✅ **高性能异步处理** - 使用LMAX Disruptor实现低延迟队列
✅ **多云支持** - 支持阿里云OSS和AWS S3兼容存储
✅ **多框架支持** - 完整支持Log4j、Log4j2、Logback日志框架
✅ **企业级可靠性** - 全面的错误处理和重试机制
✅ **零性能影响** - 非阻塞设计，不影响应用程序性能

## 快速开始

### 系统要求

- Java 8 或更高版本
- Maven 3.6+

### 构建项目

```bash
# 克隆项目（包含所有子模块）
git clone --recursive https://github.com/kzy77/oss-appender.git
cd oss-appender

# 构建所有模块
mvn clean install

# 构建特定模块
mvn clean install -pl log4j2-oss-appender
```

### 安装依赖

根据你使用的日志框架选择对应的依赖：

#### Maven 依赖

```xml
<!-- Log4j 1.x -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>log4j-oss-appender</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- Log4j2 -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>log4j2-oss-appender</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- Logback -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>logback-oss-appender</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### Gradle 依赖

```groovy
// Log4j 1.x
implementation 'org.logx:log4j-oss-appender:0.1.0'

// Log4j2
implementation 'org.logx:log4j2-oss-appender:0.1.0'

// Logback
implementation 'org.logx:logback-oss-appender:0.1.0'
```

### 基本使用

#### Log4j 1.x 示例

```xml
<!-- log4j.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration>
    <appender name="OSS" class="org.logx.log4j.OSSAppender">
        <param name="endpoint" value="https://oss-cn-hangzhou.aliyuncs.com"/>
        <param name="accessKey" value="${OSS_ACCESS_KEY}"/>
        <param name="secretKey" value="${OSS_SECRET_KEY}"/>
        <param name="bucketName" value="my-log-bucket"/>
        <param name="batchSize" value="100"/>
        <param name="flushInterval" value="5000"/>
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c{1} - %m%n"/>
        </layout>
    </appender>

    <root>
        <level value="INFO"/>
        <appender-ref ref="OSS"/>
    </root>
</log4j:configuration>
```

**Log4j 1.x properties文件配置：**

```properties
# log4j.properties
log4j.rootLogger=INFO, OSS

# OSS Appender配置
log4j.appender.OSS=org.logx.log4j.OSSAppender
log4j.appender.OSS.endpoint=https://oss-cn-hangzhou.aliyuncs.com
log4j.appender.OSS.accessKey=${OSS_ACCESS_KEY}
log4j.appender.OSS.secretKey=${OSS_SECRET_KEY}
log4j.appender.OSS.bucketName=my-log-bucket
log4j.appender.OSS.batchSize=100
log4j.appender.OSS.flushInterval=5000
log4j.appender.OSS.layout=org.apache.log4j.PatternLayout
log4j.appender.OSS.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c{1} - %m%n
```

#### Log4j2 示例

```xml
<!-- log4j2.xml -->
<Configuration>
    <Appenders>
        <OSSAppender name="OSS">
            <endpoint>https://oss-cn-hangzhou.aliyuncs.com</endpoint>
            <accessKey>${env:OSS_ACCESS_KEY}</accessKey>
            <secretKey>${env:OSS_SECRET_KEY}</secretKey>
            <bucketName>my-log-bucket</bucketName>
            <batchSize>100</batchSize>
            <flushInterval>5000</flushInterval>
        </OSSAppender>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="OSS"/>
        </Root>
    </Loggers>
</Configuration>
```

#### Logback 示例

```xml
<!-- logback.xml -->
<configuration>
    <appender name="OSS" class="org.logx.logback.OSSAppender">
        <endpoint>https://oss-cn-hangzhou.aliyuncs.com</endpoint>
        <accessKey>${OSS_ACCESS_KEY}</accessKey>
        <secretKey>${OSS_SECRET_KEY}</secretKey>
        <bucketName>my-log-bucket</bucketName>
        <batchSize>100</batchSize>
        <flushInterval>5000</flushInterval>
    </appender>

    <root level="INFO">
        <appender-ref ref="OSS"/>
    </root>
</configuration>
```

### 配置参数说明

所有Appender都支持以下配置参数：

#### 必需参数

| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| **endpoint** | String | 对象存储服务的访问端点 | `https://oss-cn-hangzhou.aliyuncs.com` |
| **accessKey** | String | 访问密钥ID | `${OSS_ACCESS_KEY}` |
| **secretKey** | String | 访问密钥Secret | `${OSS_SECRET_KEY}` |
| **bucketName** | String | 存储桶名称 | `my-log-bucket` |

#### 可选参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **batchSize** | Integer | 100 | 批量上传的日志条数 |
| **flushInterval** | Integer | 5000 | 强制刷新间隔(毫秒) |
| **keyPrefix** | String | logs/ | 对象存储中的文件路径前缀 |
| **timePattern** | String | yyyy/MM/dd/HH | 时间分区模式 |
| **compression** | Boolean | true | 是否启用GZIP压缩 |
| **maxFileSize** | String | 10MB | 单个文件最大大小 |
| **bufferSize** | Integer | 8192 | 内部缓冲区大小 |
| **connectTimeout** | Integer | 10000 | 连接超时时间(毫秒) |
| **socketTimeout** | Integer | 50000 | Socket超时时间(毫秒) |

#### 云服务商端点示例

```bash
# 阿里云OSS
https://oss-cn-hangzhou.aliyuncs.com    # 杭州
https://oss-cn-beijing.aliyuncs.com     # 北京
https://oss-cn-shanghai.aliyuncs.com    # 上海

# AWS S3
https://s3.us-east-1.amazonaws.com      # 美东
https://s3.eu-west-1.amazonaws.com      # 欧洲

# 腾讯云COS
https://cos.ap-beijing.myqcloud.com     # 北京
https://cos.ap-shanghai.myqcloud.com    # 上海

# MinIO (自建)
http://localhost:9000                    # 本地MinIO
```

### 环境变量配置

建议通过环境变量配置敏感信息：

```bash
# 设置环境变量
export OSS_ACCESS_KEY="your-access-key"
export OSS_SECRET_KEY="your-secret-key"
export OSS_BUCKET_NAME="your-bucket-name"
export OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
```

### Java代码示例

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogExample {
    private static final Logger logger = LoggerFactory.getLogger(LogExample.class);

    public void doSomething() {
        logger.info("开始处理业务逻辑");
        logger.warn("这是一个警告信息");
        logger.error("发生了错误", new RuntimeException("示例异常"));
    }
}
```

## 项目结构

本项目采用单仓库多模块（Monorepo）架构，统一管理所有组件：

```
oss-appender/                     # 主仓库
├── .bmad-core/                   # BMAD项目管理配置
├── docs/                         # 项目文档
│   ├── architecture.md          # 架构文档
│   ├── prd.md                   # 产品需求文档
│   ├── developer-guide.md       # 开发者指南
│   └── git-management.md        # Git管理指南
├── log-java-producer/           # 核心处理引擎
├── log4j-oss-appender/          # Log4j集成模块
├── log4j2-oss-appender/         # Log4j2集成模块
├── logback-oss-appender/        # Logback集成模块
└── pom.xml                      # 父POM文件
```

### 模块组件

各模块功能清晰分工，构成完整的日志上传解决方案：

| 模块名称 | 功能描述 | 依赖关系 |
|---------|---------|----------|
| **log-java-producer** | 核心处理引擎，提供队列管理、异步处理、S3接口抽象 | 基础模块，无依赖 |
| **log4j-oss-appender** | Log4j 1.x框架适配器，实现OSSAppender | 依赖log-java-producer |
| **log4j2-oss-appender** | Log4j2框架适配器，支持插件配置 | 依赖log-java-producer |
| **logback-oss-appender** | Logback框架适配器，支持Spring Boot | 依赖log-java-producer |

### 项目管理

本项目采用统一的Git工作流管理，详细说明请参考：
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程

## 技术栈

- **语言**: Java 8+
- **构建工具**: Maven 3.9.6
- **核心依赖**: LMAX Disruptor 3.4.4
- **云存储**: AWS SDK 2.28.16, Aliyun OSS SDK 3.17.4
- **测试**: JUnit 5, Mockito, AssertJ

## 高级配置

### 性能优化建议

```xml
<!-- 高性能配置示例 (Log4j2) -->
<Configuration>
    <Appenders>
        <OSSAppender name="OSS">
            <endpoint>https://oss-cn-hangzhou.aliyuncs.com</endpoint>
            <accessKey>${env:OSS_ACCESS_KEY}</accessKey>
            <secretKey>${env:OSS_SECRET_KEY}</secretKey>
            <bucketName>my-log-bucket</bucketName>

            <!-- 性能调优参数 -->
            <batchSize>500</batchSize>           <!-- 增大批量大小 -->
            <flushInterval>10000</flushInterval> <!-- 增加刷新间隔 -->
            <bufferSize>16384</bufferSize>       <!-- 增大缓冲区 -->
            <compression>true</compression>       <!-- 启用压缩节省带宽 -->

            <!-- 文件分区策略 -->
            <keyPrefix>logs/app/</keyPrefix>
            <timePattern>yyyy/MM/dd/HH</timePattern>
            <maxFileSize>50MB</maxFileSize>
        </OSSAppender>
    </Appenders>
    <Loggers>
        <!-- 使用异步Logger提升性能 -->
        <AsyncRoot level="INFO">
            <AppenderRef ref="OSS"/>
        </AsyncRoot>
    </Loggers>
</Configuration>
```

### 生产环境最佳实践

#### 1. 安全配置
- ✅ 使用环境变量存储敏感信息
- ✅ 配置最小权限的IAM策略
- ✅ 启用OSS访问日志审计
- ✅ 定期轮换访问密钥

#### 2. 性能优化
- ✅ 根据日志量调整`batchSize`和`flushInterval`
- ✅ 使用异步Logger减少应用延迟
- ✅ 启用压缩节省存储和带宽成本
- ✅ 合理设置文件大小避免小文件问题

#### 3. 监控告警
- ✅ 监控OSS上传成功率
- ✅ 设置存储用量告警
- ✅ 监控应用日志队列深度
- ✅ 配置网络异常重试机制

#### 4. 成本控制
- ✅ 设置日志生命周期策略
- ✅ 配置冷存储转换规则
- ✅ 定期清理过期日志文件
- ✅ 监控存储和流量费用

### 故障排查

#### 常见问题

**问题1: 上传失败**
```bash
# 检查网络连接
curl -I https://oss-cn-hangzhou.aliyuncs.com

# 验证密钥权限
ossutil ls oss://your-bucket-name --config-file ~/.ossutilconfig
```

**问题2: 性能问题**
```xml
<!-- 调整批量参数 -->
<batchSize>1000</batchSize>
<flushInterval>30000</flushInterval>
```

**问题3: 内存占用过高**
```xml
<!-- 减少缓冲区大小 -->
<bufferSize>4096</bufferSize>
<maxFileSize>10MB</maxFileSize>
```

### 集成示例

#### Spring Boot集成

```yaml
# application.yml
logging:
  config: classpath:logback-spring.xml

# 环境变量
OSS_ACCESS_KEY: ${OSS_ACCESS_KEY}
OSS_SECRET_KEY: ${OSS_SECRET_KEY}
OSS_BUCKET_NAME: ${OSS_BUCKET_NAME:app-logs}
OSS_ENDPOINT: ${OSS_ENDPOINT:https://oss-cn-hangzhou.aliyuncs.com}
```

#### Docker部署

```dockerfile
# Dockerfile
FROM openjdk:8-jre-alpine
COPY app.jar /app.jar

# 设置环境变量
ENV OSS_ACCESS_KEY=""
ENV OSS_SECRET_KEY=""
ENV OSS_BUCKET_NAME="app-logs"
ENV OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 文档

- [架构设计文档](docs/architecture.md) - 详细的技术架构说明
- [产品需求文档](docs/prd.md) - 项目需求和Epic定义
- [开发者指南](docs/developer-guide.md) - 开发环境设置和贡献指南
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程

## 开发

### 开发环境设置

```bash
# 1. 克隆仓库
git clone --recursive https://github.com/kzy77/oss-appender.git

# 2. 验证构建
mvn validate

# 3. 运行测试
mvn test

# 4. 代码质量检查
mvn spotbugs:check formatter:validate
```

### 模块开发

```bash
# 构建特定模块
mvn clean install -pl log4j2-oss-appender

# 测试特定模块
mvn test -pl log-java-producer

# 检查模块依赖
mvn dependency:tree -pl logback-oss-appender

# 统一更新版本号
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit
```

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 贡献

欢迎贡献代码！请查看 [开发者指南](docs/developer-guide.md) 了解详细的贡献流程。

## 支持

如果遇到问题或有建议，请：

1. 查看 [文档](docs/)
2. 搜索 [Issues](https://github.com/kzy77/oss-appender/issues)
3. 创建新的 Issue

---

🚀 **OSS Appender - 让日志上传更简单、更高效！**