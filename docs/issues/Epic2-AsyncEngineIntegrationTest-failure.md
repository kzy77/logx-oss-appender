# Issue: Epic 2 集成测试失败 —— AsyncEngineIntegrationTest.shouldMeetLatencyTarget

- 模块：`logx-producer`
- 失败用例：`org.logx.integration.AsyncEngineIntegrationTest.shouldMeetLatencyTarget`
- 失败摘要：断言失败 — 期望处理数量大于 4000，实际为 3269
- 失败信息：
  ```
  Expecting actual:
    3269L
  to be greater than:
    4000L
  ```
- 影响范围：性能阈值校验（Epic 2 高性能异步引擎），不影响 Epic 1 交付。

## 复现步骤
1. 初始化/更新子模块（如需）：`git submodule update --init --recursive`
2. 仅运行该测试：
   - `mvn -q -pl logx-producer -Dtest=org.logx.integration.AsyncEngineIntegrationTest#shouldMeetLatencyTarget test`
   - 或运行整个类：`mvn -q -pl logx-producer -Dtest=org.logx.integration.AsyncEngineIntegrationTest test`
3. 查看报告：`logx-producer/target/surefire-reports/org.logx.integration.AsyncEngineIntegrationTest.txt`

## 初步分析
- 该用例验证性能/吞吐或延迟目标，容易受运行环境（CPU/容器/共享资源）影响。
- 当前阈值可能偏严，或需要预热/更稳定的基准环境。
- 建议将性能验证与功能 CI 分离，避免因环境波动导致 CI 红灯。

## 建议修复方向
1. 引入预热（warm-up）与固定负载，降低抖动；或提升/参数化阈值（读取配置）。
2. 为性能类用例添加标签（如 `@Tag("perf")`），默认排除于常规 CI，保留在基准/夜跑任务。
3. 将该用例迁移到基准测试框架（如 JMH）或独立性能套件。
4. 增加稳定性：固定线程池/等待策略、禁用不必要日志、控制 GC 干扰。

## 验收标准
- 在相同 CI/容器环境下，连续 3 次运行均通过。
- 文档化阈值与环境要求（README 或 `docs/architecture.md` 补充性能基线说明）。

## 状态
已计划修复

## 附件与参考
- Surefire 报告路径：`logx-producer/target/surefire-reports/`
- 相关日志（片段）：见上述失败摘要与 surefire 报告文件。