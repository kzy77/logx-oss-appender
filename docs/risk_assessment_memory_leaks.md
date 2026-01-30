# 内存泄漏风险评估报告

**生成日期**: 2025-10-06
**项目**: LogX OSS Appender
**评估级别**: ⚠️ 中风险 (存在高优先级修复项)

## 1. 执行摘要

经过对代码库的静态分析和模式匹配，本项目**未发现严重的资源未关闭（Resource Leak）问题**，大部分 I/O 资源均使用了 `try-with-resources` 或显式关闭。

然而，发现了 **3个高风险** 和 **1个中风险** 的内存泄漏隐患，主要集中在配置管理和生命周期回调处理上。建议在下一版本中优先修复。

---

## 2. 详细风险列表

### 🔴 高优先级 (High Priority)

#### 2.1 ConfigManager 无界缓存

- **位置**: `logx-producer/src/main/java/org/logx/config/ConfigManager.java` (Line 38)
- **问题描述**: `configCache` 使用 `ConcurrentHashMap` 但未设置大小限制、过期时间和驱逐策略。
- **风险**: 在动态配置场景下（如键名动态生成），缓存会无限增长导致 `OutOfMemoryError`。
- **修复建议**: 引入 Caffeine 或 Guava Cache，设置 `maximumSize` 和 `expireAfterAccess`。

#### 2.2 ShutdownHookHandler 回调累积泄漏

- **位置**: `logx-producer/src/main/java/org/logx/reliability/ShutdownHookHandler.java` (Line 90)
- **问题描述**: `registerCallback` 方法只添加回调到 `List`，但类中**完全缺失** `unregisterCallback` 或 `removeCallback` 方法。
- **风险**: 每次创建 `AsyncEngine` 实例都会注册一个新的回调。在容器频繁重启或测试场景下，回调列表会持续增长，导致内存泄漏。
- **修复建议**: 添加 `unregisterCallback` 方法，并在组件关闭时调用。

#### 2.3 AsyncEngineImpl 匿名类持有引用

- **位置**: `logx-producer/src/main/java/org/logx/core/AsyncEngineImpl.java` (Line 81)
- **问题描述**: 注册的 `ShutdownCallback` 是匿名内部类，隐式持有 `AsyncEngineImpl` 的 `this` 引用。
- **风险**: 由于回调无法注销（见 2.2），`ShutdownHookHandler` 会一直持有 `AsyncEngineImpl` 实例的引用，导致整个引擎无法被垃圾回收（GC）。
- **修复建议**: 配合 2.2 的修复，在 `stop()` 方法中显式注销回调；或使用静态内部类避免持有外部引用。

### 🟡 中优先级 (Medium Priority)

#### 2.4 RetryManager 匿名策略类

- **位置**: `logx-producer/src/main/java/org/logx/reliability/RetryManager.java` (Lines 329, 352)
- **问题描述**: 工厂方法返回的匿名 `RetryPolicy` 类持有外部 `RetryManager` 实例引用。
- **风险**: 阻碍 `RetryManager` 被 GC 回收。
- **修复建议**: 将匿名类重构为静态内部类 (Static Nested Class)。

---

## 3. 资源管理审查 (Resource Management)

| 资源类型 | 状态 | 说明 |
| :--- | :--- | :--- |
| **InputStream/OutputSteam** | ✅ 安全 | 所有发现的流均使用了 `try-with-resources` |
| **S3Client** | ✅ 安全 | 在 `S3StorageServiceAdapter.close()` 中正确关闭 |
| **线程池 (ExecutorService)** | ✅ 安全 | 所有线程池均有 `shutdown()` 和 `awaitTermination()` 处理 |
| **ByteArrayOutputStream** | ℹ️ 忽略 | 未关闭，但无原生资源占用，不属于泄漏 |

---

## 4. 修复计划建议

1. **P0 (立即修复)**: 
   - 在 `ShutdownHookHandler` 中添加 `unregisterCallback`。
   - 在 `AsyncEngineImpl.close()`/`stop()` 中注销回调。

2. **P1 (下个Sprint)**:
   - 改造 `ConfigManager.configCache` 为有界缓存。

3. **P2 (技术债)**:
   - 重构所有匿名内部类为静态内部类或 Lambda 表达式（如果不捕获 `this`）。

## 5. 修复状态更新 (2025-10-06)

所有已识别的内存泄漏风险均已在 `feature/fix-memory-leaks` 分支修复：

| 风险项 | 修复措施 | 状态 |
| :--- | :--- | :--- |
| **ConfigManager 无界缓存** | 替换 `ConcurrentHashMap` 为 `LinkedHashMap`，设置最大容量 1000 且启用 LRU 淘汰策略。 | ✅ 已修复 |
| **ShutdownHookHandler 回调泄漏** | 新增 `unregisterCallback` 方法，允许注销回调。 | ✅ 已修复 |
| **AsyncEngineImpl 匿名类引用** | 将匿名回调重构为静态内部类 `AsyncEngineShutdownCallback`，并在 `stop()` 时显式注销。 | ✅ 已修复 |
| **RetryManager 匿名策略类** | 将所有匿名 `RetryPolicy` 实现重构为静态内部类 (`CustomRetryPolicy`, `FixedDelayRetryPolicy`)。 | ✅ 已修复 |

### 验证结果
- **编译检查**: `mvn clean compile` 通过。
- **单元测试**: `mvn test -pl logx-producer` 全部通过。

