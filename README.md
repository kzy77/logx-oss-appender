# LogX OSS Appender

一个高性能日志上传组件套件，支持将日志异步批量上传到阿里云OSS和AWS S3兼容的对象存储服务。

## 项目概述

LogX OSS Appender 为Java应用程序提供了一套完整的日志上传解决方案，包含十二个核心模块：

- **[logx-producer](logx-producer)** - 核心基础模块，提供日志生产和队列管理
- **[logx-s3-adapter](logx-s3-adapter)** - S3兼容存储适配器，支持AWS S3、阿里云OSS、腾讯云COS、MinIO等
- **[logx-sf-oss-adapter](logx-sf-oss-adapter)** - SF OSS存储适配器，专门支持SF OSS存储服务
- **[log4j-oss-appender](log4j-oss-appender)** - Log4j 1.x版本的OSS Appender
- **[log4j2-oss-appender](log4j2-oss-appender)** - Log4j2版本的OSS Appender
- **[logback-oss-appender](logback-oss-appender)** - Logback版本的OSS Appender
- **[sf-log4j-oss-appender](sf-log4j-oss-appender)** - SF OSS存储服务的Log4j 1.x All-in-One包
- **[sf-log4j2-oss-appender](sf-log4j2-oss-appender)** - SF OSS存储服务的Log4j2 All-in-One包
- **[sf-logback-oss-appender](sf-logback-oss-appender)** - SF OSS存储服务的Logback All-in-One包
- **[s3-log4j-oss-appender](s3-log4j-oss-appender)** - S3兼容存储服务的Log4j 1.x All-in-One包
- **[s3-log4j2-oss-appender](s3-log4j2-oss-appender)** - S3兼容存储服务的Log4j2 All-in-One包
- **[s3-logback-oss-appender](s3-logback-oss-appender)** - S3兼容存储服务的Logback All-in-One包

所有模块都遵循统一的包命名规范和配置Key标准，确保系统的一致性和可维护性。

## Maven项目结构优化

为了更好地管理项目的版本和依赖关系，我们对Maven项目结构进行了优化：

- **统一父POM版本为1.0.0-SNAPSHOT**
- **所有子模块继承父POM版本，移除硬编码版本号**
- **在父POM中统一管理所有内部模块和第三方依赖版本**
- **优化依赖管理，使用dependencyManagement统一版本控制**
- **添加日志框架版本属性定义**

### 版本管理策略

本项目采用统一的版本管理策略，所有子模块都继承自父POM的版本号，确保版本一致性：

1. **父POM版本**：所有子模块的版本号统一由父POM管理，当前版本为`1.0.0-SNAPSHOT`
2. **子模块版本继承**：子模块无需声明版本号，自动继承父POM的版本
3. **依赖版本统一管理**：所有第三方依赖和内部模块依赖的版本在父POM的`dependencyManagement`中统一定义
4. **版本属性定义**：关键依赖的版本通过属性定义，便于统一维护和升级

### 依赖管理优化

我们对项目的依赖管理进行了优化，主要体现在以下几个方面：

1. **内部模块依赖**：所有内部模块（如`logx-producer`、`log4j2-oss-appender`等）的版本在父POM中统一管理，子模块直接引用无需指定版本号
2. **第三方依赖版本控制**：通过`dependencyManagement`统一管理第三方依赖版本，确保所有模块使用一致的依赖版本
3. **日志框架版本属性**：为常用的日志框架（Log4j、Log4j2、Logback）定义了版本属性，便于维护和升级

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

### SDK使用快速开始

以下是以Logback和SF-OSS的All-in-One包为主要示例的快速开始指南：

#### 主要示例：Logback + SF-OSS快速开始

1. **添加依赖**
```xml
<!-- 使用All-in-One包（推荐） -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-logback-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **最简配置（以SF OSS为例）**
```xml
<configuration>
  <appender name="SF_OSS" class="org.logx.logback.LogbackOSSAppender">
    <endpoint>${LOGX_OSS_ENDPOINT:-https://sf-oss-cn-north-1.sf-oss.com}</endpoint>
    <accessKeyId>${LOGX_OSS_ACCESS_KEY_ID}</accessKeyId>
    <accessKeySecret>${LOGX_OSS_ACCESS_KEY_SECRET}</accessKeySecret>
    <bucket>${LOGX_OSS_BUCKET}</bucket>
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
      <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="INFO"><appender-ref ref="SF_OSS"/></root>
</configuration>
```

3. **环境变量配置**
```bash
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"
```

#### 其他框架示例

##### Log4j 1.x + SF-OSS快速开始

1. **添加依赖**
```xml
<!-- 使用All-in-One包（推荐） -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-log4j-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **最简配置（以SF OSS为例）**
```xml
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">
  <appender name="oss" class="org.logx.log4j.Log4jOSSAppender">
    <param name="endpoint" value="${LOGX_OSS_ENDPOINT:-https://sf-oss-cn-north-1.sf-oss.com}"/>
    <param name="accessKeyId" value="${sys:LOGX_OSS_ACCESS_KEY_ID}"/>
    <param name="accessKeySecret" value="${sys:LOGX_OSS_ACCESS_KEY_SECRET}"/>
    <param name="bucket" value="${sys:LOGX_OSS_BUCKET}"/>
    <layout class="org.apache.log4j.PatternLayout">
      <param name="ConversionPattern" value="%d{ISO8601} %-5p %c{1.} - %m%ex{full}"/>
    </layout>
  </appender>
  <root>
    <priority value="info"/>
    <appender-ref ref="oss"/>
  </root>
</log4j:configuration>
```

3. **环境变量配置**
```bash
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"
```

##### Log4j2 + SF-OSS快速开始

1. **添加依赖**
```xml
<!-- 使用All-in-One包（推荐） -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-log4j2-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **最简配置（以SF OSS为例）**
```xml
<Configuration>
  <Appenders>
    <OSS name="oss" endpoint="https://sf-oss-cn-north-1.sf-oss.com"
                 accessKeyId="${sys:LOGX_OSS_ACCESS_KEY_ID}" accessKeySecret="${sys:LOGX_OSS_ACCESS_KEY_SECRET}"
                 bucket="${sys:LOGX_OSS_BUCKET}">
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

3. **环境变量配置**
```bash
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"
```

### 构建项目

```bash
# 克隆项目（包含所有子模块）
git clone --recursive https://github.com/logx-oss-appender/logx-oss-appender.git
cd logx-oss-appender

# 构建所有模块
mvn clean install

# 构建特定模块
mvn clean install -pl log4j2-oss-appender
```

### 安装依赖

根据你使用的日志框架和存储服务选择对应的依赖：

#### Maven 依赖

只需引入一个All-in-One包即可，自动包含日志框架适配器和对应的存储适配器：

```xml
<!-- SF OSS存储服务 -->
<!-- SF Log4j 1.x -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-log4j-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- SF Log4j2 -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-log4j2-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- SF Logback -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>sf-logback-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- S3兼容存储服务（阿里云OSS、AWS S3等） -->
<!-- S3 Log4j 1.x -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>s3-log4j-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- S3 Log4j2 -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>s3-log4j2-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- S3 Logback -->
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>s3-logback-oss-appender</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Gradle 依赖

##### 方式一：使用All-in-One包（推荐）

```groovy
// SF OSS存储服务
// SF Log4j 1.x
implementation 'org.logx:sf-log4j-oss-appender:1.0.0-SNAPSHOT'

// SF Log4j2
implementation 'org.logx:sf-log4j2-oss-appender:1.0.0-SNAPSHOT'

// SF Logback
implementation 'org.logx:sf-logback-oss-appender:1.0.0-SNAPSHOT'

// S3兼容存储服务
// S3 Log4j 1.x
implementation 'org.logx:s3-log4j-oss-appender:1.0.0-SNAPSHOT'

// S3 Log4j2
implementation 'org.logx:s3-log4j2-oss-appender:1.0.0-SNAPSHOT'

// S3 Logback
implementation 'org.logx:s3-logback-oss-appender:1.0.0-SNAPSHOT'
```

#### 非Maven/Gradle项目依赖引入

对于不使用Maven或Gradle的项目，可以通过以下方式引入依赖：

##### 1. 直接使用JAR包

通过CI/CD构建后上传到Maven仓库，可以从Maven仓库下载所需的JAR包：

1. **All-in-One包**（推荐）：每个包都包含了日志框架适配器和对应的存储适配器
   - `sf-log4j-oss-appender-{version}.jar` - SF OSS + Log4j 1.x
   - `sf-log4j2-oss-appender-{version}.jar` - SF OSS + Log4j2
   - `sf-logback-oss-appender-{version}.jar` - SF OSS + Logback
   - `s3-log4j-oss-appender-{version}.jar` - S3兼容存储 + Log4j 1.x
   - `s3-log4j2-oss-appender-{version}.jar` - S3兼容存储 + Log4j2
   - `s3-logback-oss-appender-{version}.jar` - S3兼容存储 + Logback

2. **分别引入组件**：
   - 日志框架适配器（选择其一）：
     - `log4j-oss-appender-{version}.jar` - Log4j 1.x适配器
     - `log4j2-oss-appender-{version}.jar` - Log4j2适配器
     - `logback-oss-appender-{version}.jar` - Logback适配器
   - 核心组件：
     - `logx-producer-{version}.jar` - 核心日志处理引擎
   - 存储适配器（选择其一）：
     - `logx-s3-adapter-{version}.jar` - S3兼容存储适配器
     - `logx-sf-oss-adapter-{version}.jar` - SF OSS存储适配器

##### 2. 环境要求

- Java 8或更高版本
- 对应的日志框架版本：
  - Log4j 1.2.17或更高版本
  - Log4j2 2.22.1或更高版本
  - Logback 1.2.13或更高版本

### 基本使用

在完成快速开始的配置后，你可以按照以下方式使用LogX OSS Appender：

#### Log4j 1.x 示例

```xml
<!-- log4j.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration>
    <appender name="OSS" class="org.logx.log4j.Log4jOSSAppender">
        <param name="endpoint" value="${LOGX_OSS_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}"/>
        <param name="accessKeyId" value="${sys:LOGX_OSS_ACCESS_KEY_ID}"/>
        <param name="accessKeySecret" value="${sys:LOGX_OSS_ACCESS_KEY_SECRET}"/>
        <param name="bucket" value="${sys:LOGX_OSS_BUCKET}"/>
        <param name="region" value="${LOGX_OSS_REGION:-cn-hangzhou}"/>
        <param name="keyPrefix" value="${LOGX_OSS_KEY_PREFIX:-logs/app/}"/>
        <param name="maxBatchCount" value="${LOGX_OSS_MAX_BATCH_COUNT:-4096}"/>
        <param name="maxMessageAgeMs" value="${LOGX_OSS_MAX_MESSAGE_AGE_MS:-600000}"/>
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
log4j.appender.OSS=org.logx.log4j.Log4jOSSAppender
log4j.appender.OSS.endpoint=${LOGX_OSS_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}
log4j.appender.OSS.accessKeyId=${LOGX_OSS_ACCESS_KEY_ID}
log4j.appender.OSS.accessKeySecret=${LOGX_OSS_ACCESS_KEY_SECRET}
log4j.appender.OSS.bucket=${LOGX_OSS_BUCKET}
log4j.appender.OSS.region=${LOGX_OSS_REGION:-cn-hangzhou}
log4j.appender.OSS.keyPrefix=${LOGX_OSS_KEY_PREFIX:-logs/app/}
log4j.appender.OSS.maxBatchCount=${LOGX_OSS_MAX_BATCH_COUNT:-4096}
log4j.appender.OSS.maxMessageAgeMs=${LOGX_OSS_MAX_MESSAGE_AGE_MS:-600000}
log4j.appender.OSS.layout=org.apache.log4j.PatternLayout
log4j.appender.OSS.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c{1} - %m%n
```

#### Log4j2 示例

```xml
<!-- log4j2.xml -->
<Configuration>
    <Appenders>
        <OSS name="OSS">
            <endpoint>${sys:LOGX_OSS_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}</endpoint>
            <accessKeyId>${sys:LOGX_OSS_ACCESS_KEY_ID}</accessKeyId>
            <accessKeySecret>${sys:LOGX_OSS_ACCESS_KEY_SECRET}</accessKeySecret>
            <bucket>${sys:LOGX_OSS_BUCKET}</bucket>
            <region>${sys:LOGX_OSS_REGION:-cn-hangzhou}</region>
            <keyPrefix>${sys:LOGX_OSS_KEY_PREFIX:-logs/app/}</keyPrefix>
            <maxBatchCount>${sys:LOGX_OSS_MAX_BATCH_COUNT:-4096}</maxBatchCount>
            <maxMessageAgeMs>${sys:LOGX_OSS_MAX_MESSAGE_AGE_MS:-600000}</maxMessageAgeMs>
        </OSS>
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
    <appender name="OSS" class="="org.logx.logback.LogbackOSSAppender">
        <endpoint>${LOGX_OSS_ENDPOINT:-https://oss-cn-hangzhou.aliyuncs.com}</endpoint>
        <accessKeyId>${LOGX_OSS_ACCESS_KEY_ID}</accessKeyId>
        <accessKeySecret>${LOGX_OSS_ACCESS_KEY_SECRET}</accessKeySecret>
        <bucket>${LOGX_OSS_BUCKET}</bucket>
        <region>${LOGX_OSS_REGION:-cn-hangzhou}</region>
        <keyPrefix>${LOGX_OSS_KEY_PREFIX:-logs/app/}</keyPrefix>
        <maxBatchCount>${LOGX_OSS_MAX_BATCH_COUNT:-4096}</maxBatchCount>
        <maxMessageAgeMs>${LOGX_OSS_MAX_MESSAGE_AGE_MS:-600000}</maxMessageAgeMs>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="OSS"/>
    </root>
</configuration>

### 配置参数说明

所有Appender都支持以下配置参数：

#### 必需参数

| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| **endpoint** | String | 对象存储服务的访问端点 | `https://oss-cn-hangzhou.aliyuncs.com` |
| **accessKeyId** | String | 访问密钥ID | `${LOGX_OSS_ACCESS_KEY_ID}` |
| **accessKeySecret** | String | 访问密钥Secret | `${LOGX_OSS_ACCESS_KEY_SECRET}` |
| **bucket** | String | 存储桶名称 | `my-log-bucket` |

#### 可选参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **region** | String | ap-guangzhou | 存储区域 |
| **keyPrefix** | String | logs/ | 对象存储中的文件路径前缀 |
| **ossType** | String | SF_OSS | 存储后端类型，支持SF_OSS、S3等 |
| **maxQueueSize** | Integer | 65536 | 内存队列大小（必须是2的幂） |
| **maxBatchCount** | Integer | 4096 | 单批最大条数 |
| **maxBatchBytes** | Integer | 4194304 (4MB) | 单批最大字节 |
| **maxMessageAgeMs** | Long | 600000 | 最早消息年龄阈值（毫秒），10分钟 |
| **dropWhenQueueFull** | Boolean | false | 队列满时是否丢弃日志 |
| **multiProducer** | Boolean | false | 是否支持多生产者 |
| **maxRetries** | Integer | 5 | 最大重试次数 |
| **baseBackoffMs** | Long | 200 | 基础退避时间(毫秒) |
| **maxBackoffMs** | Long | 10000 | 最大退避时间(毫秒) |
| **maxUploadSizeMb** | Integer | 100 | 单个上传文件最大大小（MB），超过此大小的文件将自动分片处理 |

#### 批处理优化参数

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| **batchSize** | Integer | 100 | 批处理大小（条数），支持动态调整范围10-10000 |
| **batchSizeBytes** | Integer | 4194304 (4MB) | 批处理大小（字节） |
| **enableCompression** | Boolean | true | 是否启用数据压缩 |
| **compressionThreshold** | Integer | 1024 (1KB) | 启用压缩的数据大小阈值 |
| **enableSharding** | Boolean | true | 是否启用数据分片处理 |
| **shardingThreshold** | Integer | 104857600 (100MB) | 数据分片阈值 |
| **shardSize** | Integer | 10485760 (10MB) | 分片大小 |

#### 配置优先级

系统支持多种配置源，按以下优先级顺序读取配置：
1. JVM系统属性 (-Dlogx.oss.region=ap-guangzhou)
2. 环境变量 (LOGX_OSS_REGION=ap-guangzhou)
3. 配置文件属性 (application.properties中的logx.oss.region=ap-guangzhou)
4. 代码默认值

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
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"
export LOGX_OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
export LOGX_OSS_REGION="cn-hangzhou"
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

本项目采用单仓库多模块（Monorepo）架构，统一管理所有组件，遵循分层抽象架构和统一包命名原则：

```
logx-oss-appender/                     # 主仓库
├── .bmad-core/                   # BMAD项目管理配置
├── docs/                         # 项目文档
│   ├── architecture.md          # 架构文档
│   ├── prd.md                   # 产品需求文档
│   ├── developer-guide.md       # 开发者指南
│   └── git-management.md        # Git管理指南
├── logx-producer/              # 核心处理引擎
├── logx-s3-adapter/             # S3兼容存储适配器
├── logx-sf-oss-adapter/         # SF OSS存储适配器
├── log4j-oss-appender/          # Log4j集成模块
├── log4j2-oss-appender/         # Log4j2集成模块
├── logback-oss-appender/        # Logback集成模块
└── pom.xml                      # 父POM文件
```

### 模块组件

各模块功能清晰分工，构成完整的日志上传解决方案，遵循正确的依赖结构：

```
logx-producer (核心)
    ↓
log4j-oss-appender
log4j2-oss-appender
logback-oss-appender
```

三个适配器都直接依赖于核心模块，彼此之间没有依赖关系。

| 模块名称 | 功能描述 | 依赖关系 |
|---------|---------|----------|
| **logx-producer** | 核心处理引擎，提供队列管理、异步处理、存储接口抽象，包含AsyncEngine异步引擎和EnhancedDisruptorBatchingQueue一体化批处理队列 | 基础模块，无依赖 |
| **logx-s3-adapter** | S3兼容存储适配器，支持AWS S3、阿里云OSS、腾讯云COS、MinIO等 | 依赖logx-producer |
| **logx-sf-oss-adapter** | SF OSS存储适配器，专门支持SF OSS存储服务 | 依赖logx-producer |
| **log4j-oss-appender** | Log4j 1.x框架适配器，实现OSSAppender | 依赖logx-producer |
| **log4j2-oss-appender** | Log4j2框架适配器，支持插件配置 | 依赖logx-producer |
| **logback-oss-appender** | Logback框架适配器，支持Spring Boot | 依赖logx-producer |

### 项目管理

本项目采用统一的Git工作流管理，详细说明请参考：
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程

## 技术栈

- **语言**: Java 8+
- **构建工具**: Maven 3.9.6
- **核心依赖**: LMAX Disruptor 3.4.4
- **云存储**: AWS SDK 2.28.16
- **测试**: JUnit 5, Mockito, AssertJ

详细技术栈信息请参考 [技术栈文档](docs/architecture/tech-stack.md)。

## 高级配置

### 性能优化建议

```xml
<!-- 高性能配置示例 (Log4j2) -->
<Configuration>
    <Appenders>
        <OSS name="OSS">
            <endpoint>https://oss-cn-hangzhou.aliyuncs.com</endpoint>
            <accessKeyId>${env:OSS_ACCESS_KEY_ID}</accessKeyId>
            <accessKeySecret>${env:OSS_ACCESS_KEY_SECRET}</accessKeySecret>
            <bucket>my-log-bucket</bucket>
            <region>cn-hangzhou</region>

            <!-- 性能调优参数 -->
            <maxBatchCount>5000</maxBatchCount>     <!-- 增大批量大小 -->
            <maxMessageAgeMs>10000</maxMessageAgeMs> <!-- 降低消息年龄阈值，更快触发批处理 -->
            <maxQueueSize>131072</maxQueueSize>      <!-- 增大队列大小（必须是2的幂） -->
            <keyPrefix>logs/app/</keyPrefix>

            <!-- 重试策略 -->
            <maxRetries>3</maxRetries>
            <baseBackoffMs>500</baseBackoffMs>
            <maxBackoffMs>5000</maxBackoffMs>
        </OSS>
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
OSS_ACCESS_KEY_ID: ${LOGX_OSS_ACCESS_KEY_ID}
OSS_ACCESS_KEY_SECRET: ${LOGX_OSS_ACCESS_KEY_SECRET}
OSS_BUCKET: ${LOGX_OSS_BUCKET:app-logs}
LOGX_OSS_ENDPOINT: ${LOGX_OSS_ENDPOINT:https://oss-cn-hangzhou.aliyuncs.com}
LOGX_OSS_REGION: ${LOGX_OSS_REGION:cn-hangzhou}
```

#### Docker部署

```dockerfile
# Dockerfile
FROM openjdk:8-jre-alpine
COPY app.jar /app.jar

# 设置环境变量
ENV LOGX_OSS_ACCESS_KEY_ID=""
ENV LOGX_OSS_ACCESS_KEY_SECRET=""
ENV LOGX_OSS_BUCKET="app-logs"
ENV LOGX_OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
ENV LOGX_OSS_REGION="cn-hangzhou"

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 项目路线图与决策

### 当前版本 (v1.0.0 MVP)

当前MVP版本专注于核心功能，确保日志上传的**高性能**和**高可靠性**：

**✅ 已实现功能**：
- 高性能异步队列（LMAX Disruptor，24,777+ 消息/秒）
- 智能批处理优化（三触发条件：消息数、字节数、消息年龄）
- GZIP压缩（90%+压缩率）
- 数据分片处理（自动分片大文件）
- 失败重试机制（指数退避，最多5次）
- 兜底文件机制（网络异常时本地缓存）
- 优雅关闭保护（30秒超时保护）
- 多框架支持（Log4j、Log4j2、Logback）
- 多云支持（AWS S3、阿里云OSS、MinIO等）

**❌ 明确不在当前版本范围的功能**：

根据项目决策记录，以下功能不在MVP版本实现：

1. **监控和告警接口** ([ADR-001](docs/DECISIONS.md#adr-001-mvp版本不实现监控和告警接口))
   - 原因：核心可靠性机制已满足需求，监控需求差异大
   - 替代方案：通过日志集成到现有监控系统（Prometheus、ELK等）
   - 未来计划：v2.0可能添加Metrics API、Callback API、JMX支持

2. **动态自适应批处理算法** ([ADR-002](docs/DECISIONS.md#adr-002-mvp版本不实现动态自适应批处理算法))
   - 原因：固定配置参数已满足核心需求，自适应算法复杂且难以通用
   - 替代方案：提供三个灵活的配置参数（maxBatchCount、maxBatchBytes、maxMessageAgeMs）
   - 未来计划：v2.0可能添加预设配置模式（低延迟模式、高吞吐模式）

### 未来版本规划

**v1.1.0** (性能优化版本)
- 优化内存占用
- 增强兜底文件管理
- 添加更多性能指标

**v2.0.0** (企业增强版本)
- 监控和告警接口
- 预设配置模式
- 更丰富的扩展点

详细的架构决策和理由请参考 **[项目决策记录](docs/DECISIONS.md)**。

### 默认配置说明

项目使用以下重要默认值（符合PRD要求）：

| 配置项 | 默认值 | 说明 | 决策记录 |
|--------|--------|------|----------|
| region | ap-guangzhou | 默认存储区域 | [ADR-003](docs/DECISIONS.md#adr-003-默认region值使用ap-guangzhou) |
| maxBatchCount | 4096 | 批处理大小 | 性能测试验证 |
| maxBatchBytes | 10MB | 批处理字节数 | 性能测试验证 |
| maxMessageAgeMs | 600000 (10分钟) | 消息年龄阈值 | 平衡延迟和吞吐 |

## 文档

- [架构设计文档](docs/architecture.md) - 详细的技术架构说明
- [产品需求文档](docs/prd.md) - 项目需求和Epic定义
- [项目决策记录](docs/DECISIONS.md) - 架构和功能决策说明（新增）
- [开发者指南](docs/developer-guide.md) - 开发环境设置和贡献指南
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程

## 开发

### 开发环境设置

```bash
# 1. 克隆仓库
git clone --recursive https://github.com/logx-oss-appender/logx-oss-appender.git

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
mvn test -pl logx-producer

# 检查模块依赖
mvn dependency:tree -pl logback-oss-appender

# 统一更新版本号
mvn versions:set -DnewVersion=1.0.0-SNAPSHOT
mvn versions:commit
```

详细开发指南请参考 [开发者指南](docs/developer-guide.md) 和 [编码标准](docs/architecture/coding-standards.md)。

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 贡献

欢迎贡献代码！请查看 [开发者指南](docs/developer-guide.md) 了解详细的贡献流程。

## 已知问题

- Epic 2：`AsyncEngineIntegrationTest.shouldMeetLatencyTarget` 在当前容器/CI 环境下可能因性能抖动导致断言失败，不影响 Epic 1 交付与评审。复现步骤、初步分析与修复建议见：`docs/issues/Epic2-AsyncEngineIntegrationTest-failure.md`。

## 支持

如果遇到问题或有建议，请：

1. 查看 [文档](docs/)
2. 搜索 [Issues](https://github.com/logx-oss-appender/logx-oss-appender/issues)
3. 创建新的 Issue

---

🚀 **LogX OSS Appender - 让日志上传更简单、更高效！**
