# Repository Guidelines

## 项目结构与模块组织
- 根目录：Maven 多模块仓库。
- 核心：`log-java-producer/`（引擎、S3 抽象、健康检查工具）。
- 适配器：`log4j-oss-appender/`、`log4j2-oss-appender/`、`logback-oss-appender/`。
- 文档：`docs/`（架构、PRD、QA、stories），QA Gate 位于 `docs/qa/gates/`。
- 测试：各模块的 `src/test/java/`。
- 代理与配置：`.bmad-core/core-config.yaml`（如 `devStoryLocation: docs/stories`）。

## 构建、测试与开发命令
- 构建全部模块：`mvn -q -DskipTests package`
- 构建单模块（含依赖）：`mvn -q -pl log-java-producer -am package`
- 运行全部测试：`mvn -q -DskipTests=false test`
- 仅运行模块测试：`mvn -q -pl log-java-producer test`
- 指定测试类：`mvn -q -pl log-java-producer -Dtest=org.logx.health.HealthCheckIntegrationTest test`
- 初始化子模块：`git submodule update --init --recursive`

## 代码风格与命名
- Java 8+，UTF-8，LF 换行，4 空格缩进，单行≤120 字符。
- 包名：`org.logx.*`，类名 UpperCamelCase，常量 UPPER_SNAKE_CASE。
- 统一遵循 `docs/architecture/coding-standards.md`。

## 测试规范
- 框架：JUnit 5、AssertJ；单元覆盖率目标＞90%（酌情）。
- 命名：文件名以 `*Test` 结尾，路径在 `src/test/java`。
- 健康检查：覆盖 `HealthChecker`、`LogUploadTester` 与集成路径。
- 示例：`mvn -q -pl log-java-producer -Dtest=org.logx.test.LogUploadTesterTest test`。

## 提交与 PR 规范
- 提交信息使用祈使句+范围，如：`log-java-producer: add HealthChecker metrics`，或 `Story 1.5: validate health checks`。
- 说明动机与变更点，避免夹带无关修改，保持最小 diff。
- PR：清晰描述、关联问题/故事、附测试命令与结果；如存在与范围无关的既有失败，请在说明中注明。

## 安全与配置提示（可选）
- 禁止提交凭据；S3/OSS 端点通过环境变量/本地配置提供。
- 保持适配器配置键一致（如 bucket、keyPrefix、region）。
- 运行代理流程前检查 `.bmad-core/core-config.yaml` 配置路径。
