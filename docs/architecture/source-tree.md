# 源码树结构 - OSS Appender

## 概述

本文档详细描述OSS Appender项目的源码组织结构，遵循**分层抽象**架构和**统一包命名**原则。项目采用Maven多模块架构，核心抽象层与框架适配层清晰分离。

## 项目根目录结构

```
oss-appender/
├── docs/                           # 项目文档
│   ├── architecture/              # 架构文档
│   ├── stories/                   # 用户故事
│   ├── prd.md                     # 产品需求文档
│   └── architecture.md            # 架构总览
├── log-java-producer/             # 核心抽象层 (高性能组件)
├── log4j-oss-appender/           # Log4j 1.x 适配器
├── log4j2-oss-appender/          # Log4j2 适配器
├── logback-oss-appender/         # Logback 适配器
├── .bmad-core/                   # 开发工具链配置
├── pom.xml                       # 父项目Maven配置
└── README.md                     # 项目说明
```

## 核心组件: log-java-producer

### 目录结构
```
log-java-producer/
├── src/main/java/org/logx/
│   ├── core/                     # 核心引擎
│   │   ├── DisruptorBatchingQueue.java    # 高性能队列管理
│   │   ├── BinaryUploader.java           # 二进制上传器
│   │   └── UploadHooks.java              # 上传钩子接口
│   └── s3/                       # S3存储抽象
│       ├── S3CompatibleConfig.java        # S3配置管理
│       └── S3CompatibleUploader.java      # S3兼容上传器
├── src/test/java/                # 单元测试
└── pom.xml                       # 模块配置
```

### 核心类职责

#### 队列引擎层 (`core/`)
```java
// 高性能异步队列 (基于LMAX Disruptor)
DisruptorBatchingQueue.java
├── 功能: 无锁环形缓冲区管理
├── 特性: 纳秒级延迟、百万级TPS
└── 资源保护: 固定容量、背压控制

// 二进制数据上传器
BinaryUploader.java
├── 功能: 批量数据上传抽象接口
├── 实现: 异步上传、重试机制
└── 优化: 内存池化、零拷贝

// 上传生命周期钩子
UploadHooks.java
├── 功能: 上传前后的回调接口
├── 用途: 监控、日志、清理
└── 扩展: 支持自定义业务逻辑
```

#### S3存储抽象层 (`s3/`)
```java
// S3兼容配置管理
S3CompatibleConfig.java
├── 统一配置接口: bucket, region, credentials
├── 多云支持: AWS S3, 阿里云OSS, 腾讯云COS
└── 运行时切换: 热配置更新

// S3兼容上传实现
S3CompatibleUploader.java
├── 统一S3 API调用
├── 多云SDK适配 (AWS, Aliyun)
└── 失败重试与降级策略
```

## 框架适配器模块

### Logback适配器: logback-oss-appender

```
```
logback-oss-appender/               # Logback集成模块
├── pom.xml                         # 模块POM文件
├── README.md                       # 使用说明
├── src/
│   ├── main/
│   │   ├── java/org/logx/logback/
│   │   │   ├── LogbackOSSAppender.java       # 异步Appender实现
│   │   │   └── config/
│   │   │       └── LogbackConfiguration.java # 配置管理
│   │   └── resources/
│   │       ├── logback-oss-example.xml       # 配置示例
│   │       └── examples/
│   │           └── logback-spring.xml        # Spring Boot集成示例
│   └── test/
│       └── java/org/logx/logback/
│           ├── LogbackOSSAppenderTest.java   # Appender功能测试
│           └── springboot/
│               └── LogbackOssAutoConfigurationTest.java # Spring Boot自动配置测试
└── target/                         # 构建输出目录
```
```

### Log4j2适配器: log4j2-oss-appender

```
log4j2-oss-appender/
├── src/main/java/org/logx/
│   ├── log4j2/                  # Log4j2集成层
│   │   └── Log4j2OSSAppender.java       # Log4j2 Appender插件
│   └── adapter/                 # 适配器实现
│       └── Log4j2Bridge.java            # Log4j2桥接器
├── src/main/resources/META-INF/org/apache/logging/log4j/core/config/plugins/
│   └── Log4j2Plugins.dat       # Log4j2插件注册
└── pom.xml
```

### Log4j 1.x适配器: log4j-oss-appender

```
log4j-oss-appender/
├── src/main/java/org/logx/
│   ├── log4j/                   # Log4j 1.x集成层
│   │   └── OSSAppender.java             # Log4j 1.x Appender实现
│   └── adapter/                 # 适配器实现
│       └── Log4j1xBridge.java           # Log4j 1.x桥接器
└── pom.xml
```

## 包命名规范

### 统一包前缀策略
```java
// 核心抽象包
org.logx.core.*                   // 高性能引擎
org.logx.s3.*                     // S3存储抽象

// 框架适配包
org.logx.logback.*                // Logback专用包
org.logx.log4j.*                  // Log4j 1.x专用包
org.logx.log4j2.*                 // Log4j2专用包
org.logx.adapter.*                // 适配器实现包
```

### 配置Key统一
```java
// 所有适配器使用相同配置前缀
public static final String CONFIG_PREFIX = "oss.appender";

// 统一配置项命名
s3.bucket          // S3存储桶
s3.keyPrefix       // 对象key前缀
s3.region          // 存储区域
batch.size         // 批处理大小
batch.flushInterval // 刷新间隔
queue.capacity     // 队列容量
thread.poolSize    // 线程池大小
```

## 文档组织结构

### docs/architecture/ (架构文档)
```
docs/architecture/
├── coding-standards.md           # 编码规范标准
├── tech-stack.md                # 技术栈选型说明
└── source-tree.md               # 源码树结构 (本文档)
```

### docs/stories/ (用户故事)
按Epic组织，支持敏捷开发追踪:
```
docs/stories/
├── 1.x.*.md                     # Epic 1: 核心基础设施
├── 2.x.*.md                     # Epic 2: 高性能异步队列
├── 3.x.*.md                     # Epic 3: 多框架适配器
└── 4.x.*.md                     # Epic 4: 生产就绪特性
```

## 测试目录结构

### 单元测试组织
```java
// 每个模块的测试结构
src/test/java/
├── <对应包结构>/               # 与main包结构对应
│   ├── *Test.java              # 单元测试类
│   ├── *IntegrationTest.java   # 集成测试类
│   └── testutils/              # 测试工具类
└── resources/
    ├── logback-test.xml        # 测试环境日志配置
    └── test-*.properties       # 测试配置文件
```

## 构建产物结构

### Maven输出目录
```
target/
├── classes/                    # 编译后的类文件
├── test-classes/              # 测试类文件
├── oss-appender-*.jar         # 构建的JAR包
├── oss-appender-*-sources.jar # 源码包
└── oss-appender-*-javadoc.jar # 文档包
```

## 依赖关系图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ logback-oss-    │    │ log4j2-oss-     │    │ log4j-oss-      │
│ appender        │    │ appender        │    │ appender        │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                     ┌───────────▼───────────┐
                     │                       │
                     │  log-java-producer    │
                     │  (核心抽象层)          │
                     │                       │
                     └───────────────────────┘
```

## 部署包结构

### 发布包组织
```
oss-appender-release/
├── lib/
│   ├── log-java-producer-*.jar        # 核心组件
│   ├── logback-oss-appender-*.jar     # Logback适配器
│   ├── log4j2-oss-appender-*.jar      # Log4j2适配器
│   └── log4j-oss-appender-*.jar       # Log4j适配器
├── docs/                              # 完整文档
├── examples/                          # 配置示例
└── LICENSE
```

这个源码树结构确保了OSS Appender的**简洁性、高性能和可切换性**目标，通过清晰的分层和统一的包命名实现了架构的一致性和可维护性。