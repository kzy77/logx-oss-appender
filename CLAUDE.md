# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Critical Development Rules

**RULE 1**: 提交代码时必须主仓库和子模块一起提交
- 修改子模块后，必须同时提交子模块变更AND主仓库的子模块引用更新
- 永远不要只提交子模块而不更新主仓库引用
- 工作流程：子模块commit & push → 主仓库add submodule → 主仓库commit & push

**RULE 2**: 必须使用中文沟通
- 所有与用户的交流都必须使用中文
- 包括代码说明、进度报告、提交信息、测试输出等
- 只有技术关键词、API名称、配置键名可以使用英文

**RULE 3**: 代码不能尾行注释，注释应该在代码上一行
- 禁止在代码行尾添加注释
- 所有注释必须写在被注释代码的上一行
- 示例：
  ```java
  // 正确：注释在上一行
  String message = "Hello World";

  String message = "Hello World"; // 错误：尾行注释
  ```

**RULE 4**: if语句一定要有大括号
- 所有if语句必须使用大括号，即使只有一行代码
- 包括if、else if、else、for、while、do-while等所有控制结构
- 示例：
  ```java
  // 正确：使用大括号
  if (condition) {
      doSomething();
  }

  // 错误：缺少大括号
  if (condition) doSomething();
  ```

## Project Overview

OSS Appender is a high-performance Java logging component suite that provides asynchronous batch log uploading to cloud object storage services (Aliyun OSS, AWS S3, MinIO). The project uses a multi-module Git Submodules architecture with a core abstraction layer and framework-specific adapters.

## Core Architecture

The project follows a **layered abstraction architecture**:

- **Core Layer**: `log-java-producer` - High-performance async processing engine with LMAX Disruptor, resource protection, and S3-compatible storage abstraction
- **Adapter Layer**: Three framework adapters (`log4j-oss-appender`, `log4j2-oss-appender`, `logback-oss-appender`) providing consistent integration
- **Storage Layer**: S3-compatible object storage services (OSS, S3, MinIO)

Key architectural principles:
- **Simplicity**: Highly abstracted core with consistent adapter implementations
- **High Performance**: Minimal latency, low resource usage, high throughput
- **Switchability**: Runtime storage backend switching with zero data loss
- **Resource Protection**: Fixed thread pools, low priority scheduling, CPU yielding

## Common Development Commands

### Building and Testing

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl log-java-producer

# Run tests for specific module
mvn test -pl log-java-producer

# Run specific test class
mvn test -Dtest=AsyncEngineIntegrationTest

# Run single test method
mvn test -Dtest=AsyncEngineIntegrationTest#shouldAchieveThroughputTarget
```

### Code Quality and Analysis

```bash
# Code formatting validation
mvn formatter:validate

# Apply code formatting
mvn formatter:format

# Static analysis with SpotBugs
mvn spotbugs:check

# Security vulnerability check
mvn dependency-check:check -Psecurity

# Generate Javadoc
mvn javadoc:javadoc
```

### Development Workflow

```bash
# Clone with submodules
git clone --recursive https://github.com/kzy77/oss-appender.git

# Update submodules
git submodule update --init --recursive

# Navigate to core module for development
cd log-java-producer

# Work on framework adapters
cd log4j2-oss-appender
```

## Epic-Based Development Structure

The project follows an Epic-based development methodology defined in `docs/prd.md`:

- **Epic 1**: Core infrastructure & S3 abstraction interface ✅ **COMPLETED**
- **Epic 2**: High-performance async queue engine ✅ **COMPLETED**
- **Epic 3**: Multi-framework adapter implementation (🚧 **NEXT**)
- **Epic 4**: Production readiness & operational support

### Epic 2 Completed Components (Core Engine)

The high-performance async processing engine is fully implemented:

- **DisruptorBatchingQueue**: LMAX Disruptor-based queue with YieldingWaitStrategy
- **ResourceProtectedThreadPool**: Fixed-size thread pool with low priority scheduling
- **BatchProcessor**: Intelligent batching with GZIP compression and NDJSON serialization
- **DataLossMonitor**: Real-time data loss monitoring with sliding window algorithm
- **RetryManager**: Smart retry mechanism with exponential backoff
- **ShutdownHookHandler**: Graceful JVM shutdown with 30-second timeout protection
- **ErrorHandler**: Hierarchical error handling (WARN/ERROR/FATAL) with recovery strategies
- **MetricsCollector**: Multi-dimensional performance metrics (counters, gauges, timers)
- **HealthCheckService**: Async health checking with component aggregation

Performance achievements:
- Throughput: 24,777+ messages/second
- Latency: 2.21ms average
- Memory: 6MB usage
- Compression: 94.4% savings
- Reliability: 100% success rate, 0% data loss

## Key Technical Details

### Core Module Structure (`log-java-producer`)

```
org.logx/
├── core/           # DisruptorBatchingQueue, ResourceProtectedThreadPool
├── batch/          # BatchProcessor, compression, serialization
├── reliability/    # DataLossMonitor, RetryManager, ShutdownHookHandler
├── monitoring/     # ErrorHandler, MetricsCollector, HealthCheckService
├── s3/             # S3StorageAdapter, configuration abstractions
├── config/         # AppenderConfig, validation framework
└── factory/        # Component factories and builders
```

### Performance-Critical Configurations

- **LMAX Disruptor**: Uses YieldingWaitStrategy for optimal latency/CPU balance
- **Thread Pool**: Fixed-size (core:4, max:8) with `Thread.MIN_PRIORITY`
- **Batch Processing**: Default 100 messages/batch, 5-second flush interval
- **Compression**: GZIP enabled by default, achieves 90%+ compression ratios
- **Queue Capacity**: 4K ring buffer size for optimal memory usage

### Testing Strategy

- **Unit Tests**: JUnit 5 with 90%+ coverage requirement
- **Integration Tests**: `AsyncEngineIntegrationTest` validates complete Epic 2 system
- **Performance Tests**: Throughput, latency, resource usage, fault recovery
- **Framework Tests**: Individual adapter compatibility validation

Use AssertJ for assertions and Mockito for mocking. All Epic 2 tests pass with strict performance requirements.

### Git Submodules Workflow

This project uses Git Submodules for component isolation:

```bash
# After changes in submodule
cd log-java-producer
git add . && git commit -m "feat: implement feature"
git push

# IMPORTANT: Always update parent repository after submodule changes
cd ..
git add log-java-producer
git commit -m "feat: update log-java-producer submodule"
git push
```

**CRITICAL RULE**: Always commit both the submodule AND the parent repository together. Never commit only the submodule without updating the parent repository reference.

### Configuration Standards

All framework adapters follow consistent configuration keys:
- `endpoint` - Object storage service endpoint
- `accessKey` - Access key ID
- `secretKey` - Secret access key
- `bucketName` - Storage bucket name
- `batchSize` - Batch processing size (default: 100)
- `flushInterval` - Forced flush interval in ms (default: 5000)

## Communication Requirements

**MANDATORY**: All communication must be in Chinese (中文). This includes:
- All responses and explanations to users
- Commit messages and documentation
- Story point descriptions and Epic planning
- Test output and logging messages
- Progress reports and summaries
- Code comments and variable names where appropriate

English is only acceptable for:
- Technical keywords and API names
- Configuration keys and property names
- Code identifiers following Java conventions

### Next Development Focus (Epic 3)

The immediate next phase is implementing the framework adapters:
- Log4j 1.x adapter with XML/properties configuration support
- Log4j2 adapter with plugin architecture integration
- Logback adapter with Spring Boot compatibility
- Unified configuration validation and error handling across all adapters

Each adapter should be lightweight, delegating all heavy lifting to the Epic 2 core engine.