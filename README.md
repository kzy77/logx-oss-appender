# OSS Appender

一个高性能日志上传组件套件，支持将日志异步批量上传到阿里云OSS和AWS S3兼容的对象存储服务。

## 项目概述

OSS Appender 为Java应用程序提供了一套完整的日志上传解决方案，包含四个核心模块：

- **log-java-producer** - 核心基础模块，提供日志生产和队列管理
- **log4j-oss-appender** - Log4j 1.x版本的OSS Appender
- **log4j2-oss-appender** - Log4j2版本的OSS Appender
- **logback-oss-appender** - Logback版本的OSS Appender

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
git clone --recursive https://github.com/ossappender/oss-appender.git
cd oss-appender

# 构建所有模块
mvn clean install

# 构建特定模块
mvn clean install -pl log4j2-oss-appender
```

### 基本使用

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
    <appender name="OSS" class="io.github.ossappender.logback.OSSAppender">
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

## 项目结构

本项目采用Git Submodules管理的monorepo架构：

```
oss-appender/                     # 主仓库
├── .bmad-core/                   # BMAD项目管理配置
├── docs/                         # 项目文档
│   ├── architecture.md          # 架构文档
│   ├── prd.md                   # 产品需求文档
│   └── developer-guide.md       # 开发者指南
├── log-java-producer/           # [子模块] 核心处理引擎
├── log4j-oss-appender/          # [子模块] Log4j集成
├── log4j2-oss-appender/         # [子模块] Log4j2集成
├── logback-oss-appender/        # [子模块] Logback集成
└── pom.xml                      # 父POM文件
```

## 技术栈

- **语言**: Java 8+
- **构建工具**: Maven 3.9.6
- **核心依赖**: LMAX Disruptor 3.4.4
- **云存储**: AWS SDK 2.28.16, Aliyun OSS SDK 3.17.4
- **测试**: JUnit 5, Mockito, AssertJ

## 文档

- [架构设计文档](docs/architecture.md) - 详细的技术架构说明
- [产品需求文档](docs/prd.md) - 项目需求和Epic定义
- [开发者指南](docs/developer-guide.md) - 开发环境设置和贡献指南

## 开发

### 开发环境设置

```bash
# 1. 克隆仓库
git clone --recursive https://github.com/ossappender/oss-appender.git

# 2. 验证构建
mvn validate

# 3. 运行测试
mvn test

# 4. 代码质量检查
mvn spotbugs:check formatter:validate
```

### 子模块管理

```bash
# 更新所有子模块
git submodule update --remote

# 更新特定子模块
git submodule update --remote log-java-producer

# 拉取子模块的最新更改
git submodule foreach git pull origin main
```

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 贡献

欢迎贡献代码！请查看 [开发者指南](docs/developer-guide.md) 了解详细的贡献流程。

## 支持

如果遇到问题或有建议，请：

1. 查看 [文档](docs/)
2. 搜索 [Issues](https://github.com/ossappender/oss-appender/issues)
3. 创建新的 Issue

---

🚀 **OSS Appender - 让日志上传更简单、更高效！**