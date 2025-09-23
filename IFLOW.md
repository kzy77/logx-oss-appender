# LogX OSS Appender 项目开发流程文档 (IFLOW.md)

## 项目概述

LogX OSS Appender 是一个高性能日志上传组件套件，支持将日志异步批量上传到阿里云OSS和AWS S3兼容的对象存储服务。项目采用单仓库多模块（Monorepo）架构，包含四个核心模块：

- **log-java-producer** - 核心基础模块，提供日志生产和队列管理
- **log4j-oss-appender** - Log4j 1.x版本的OSS Appender
- **log4j2-oss-appender** - Log4j2版本的OSS Appender
- **logback-oss-appender** - Logback版本的OSS Appender

## 项目架构

### 技术架构

```
logx-oss-appender/                     # 主仓库
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

### 核心组件依赖关系

```
log-java-producer (核心)
    ↓
log4j-oss-appender ← log4j2-oss-appender ← logback-oss-appender
```

### 技术栈

- **语言**: Java 8+
- **构建工具**: Maven 3.9.6
- **核心依赖**: LMAX Disruptor 3.4.4
- **云存储**: AWS SDK 2.28.16, Aliyun OSS SDK 3.17.4
- **测试**: JUnit 5, Mockito, AssertJ

## 开发环境设置

### 系统要求

- **Java**: OpenJDK 8u392 或更高版本
- **Maven**: 3.9.6 或更高版本
- **Git**: 2.0+ (支持submodules)
- **IDE**: IntelliJ IDEA 或 Eclipse（推荐IntelliJ IDEA）

### 环境变量

```bash
export JAVA_HOME=/path/to/java8
export MAVEN_HOME=/path/to/maven
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
```

### 克隆项目

```bash
# 完整克隆（包含所有子模块）
git clone --recursive https://github.com/logx-oss-appender/logx-oss-appender.git
cd logx-oss-appender

# 如果已克隆但缺少子模块
git submodule update --init --recursive
```

## 构建和测试

### 完整构建

```bash
# 清理并构建所有模块
mvn clean install

# 并行构建（更快）
mvn clean install -T 1C

# 跳过测试的快速构建
mvn clean install -DskipTests
```

### 单模块构建

```bash
# 构建特定模块
mvn clean install -pl log4j2-oss-appender

# 构建模块及其依赖
mvn clean install -pl log4j2-oss-appender -am
```

### 测试执行

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl log-java-producer

# 运行集成测试
mvn verify -Pintegration-tests

# 生成测试报告
mvn surefire-report:report
```

### 代码质量检查

```bash
# 代码格式检查
mvn formatter:validate

# 自动格式化代码
mvn formatter:format

# 静态代码分析
mvn spotbugs:check

# 安全扫描
mvn org.owasp:dependency-check-maven:check -Psecurity
```

## Git 工作流程

### 分支管理

- **main**: 主分支，稳定发布版本
- **develop**: 开发分支，集成最新功能
- **feature/***: 功能分支，开发新特性
- **hotfix/***: 热修复分支，紧急修复

### 功能开发流程

```bash
# 创建功能分支
git checkout -b feature/new-feature develop

# 开发完成后提交
git add .
git commit -m "feat: add new feature description"

# 推送到远程
git push origin feature/new-feature

# 创建Pull Request合并到develop
```

### 代码提交规范

#### Commit Message格式
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

#### Type类型
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建工具、依赖更新

#### Scope范围
- `core`: log-java-producer核心模块
- `log4j`: log4j适配器
- `log4j2`: log4j2适配器
- `logback`: logback适配器
- `docs`: 文档
- `build`: 构建相关

#### 示例
```bash
feat(core): 实现DisruptorBatchingQueue高性能队列
fix(log4j2): 修复配置解析异常
docs(readme): 更新配置示例说明
```

## 版本管理

### 版本号策略
采用语义化版本控制（Semantic Versioning）：`MAJOR.MINOR.PATCH`

- **MAJOR**: 不兼容的API变更
- **MINOR**: 向后兼容的功能性新增
- **PATCH**: 向后兼容的问题修正

### 发布流程

#### 1. 准备发布
```bash
# 切换到develop分支
git checkout develop
git pull origin develop

# 创建release分支
git checkout -b release/1.2.0

# 更新版本号
mvn versions:set -DnewVersion=1.2.0
mvn versions:commit

# 提交版本变更
git add .
git commit -m "chore: bump version to 1.2.0"
```

#### 2. 完成发布
```bash
# 合并到main分支
git checkout main
git merge --no-ff release/1.2.0

# 创建标签
git tag -a v1.2.0 -m "Release version 1.2.0"

# 推送到远程
git push origin main
git push origin v1.2.0

# 合并回develop分支
git checkout develop
git merge --no-ff release/1.2.0

# 删除release分支
git branch -d release/1.2.0
git push origin --delete release/1.2.0
```

## 代码规范

### Java编码标准

- **编译目标**: Java 8
- **编码格式**: UTF-8
- **代码风格**: Google Java Style（通过Maven Formatter Plugin强制）
- **命名约定**:
  - 类名: PascalCase (`QueueManager`)
  - 方法名: camelCase (`processLogEvent`)
  - 常量: UPPER_SNAKE_CASE (`DEFAULT_BATCH_SIZE`)
  - 包名: lowercase.dotted (`org.logx.core`)

### 关键规则

- **日志**: 使用SLF4J进行内部日志，禁止在生产代码中使用`System.out`
- **异常处理**: 将云存储异常包装在库特定异常中
- **线程安全**: 所有公共API必须是线程安全的
- **配置**: 对配置对象使用构建器模式
- **依赖**: 将日志框架标记为`provided`作用域

### 测试标准

- **框架**: JUnit 5.10.1
- **模拟**: Mockito 5.8.0
- **断言**: AssertJ 3.24.2
- **测试文件**: `*Test.java`位于`src/test/java`
- **覆盖率**: 核心逻辑最低85%

## 贡献流程

### 1. Fork和克隆

```bash
# Fork主仓库和相关子模块仓库到你的GitHub账户
# 然后克隆你的fork

git clone --recursive https://github.com/你的用户名/logx-oss-appender.git
cd logx-oss-appender

# 添加upstream远程仓库
git remote add upstream https://github.com/logx-oss-appender/logx-oss-appender.git
```

### 2. 创建功能分支

```bash
git checkout -b feature/新功能描述

# 如果修改子模块，也要在子模块中创建分支
cd log-java-producer
git checkout -b feature/新功能描述
cd ..
```

### 3. 开发和测试

```bash
# 进行代码修改...

# 运行测试确保没有破坏现有功能
mvn test

# 运行代码质量检查
mvn formatter:validate spotbugs:check
```

### 4. 提交更改

```bash
# 如果修改了子模块，先提交子模块更改
cd log-java-producer
git add .
git commit -m "feat: 添加新功能描述"
git push origin feature/新功能描述
cd ..

# 提交主仓库更改
git add .
git commit -m "feat: 在主仓库中集成新功能"
git push origin feature/新功能描述
```

### 5. 创建Pull Request

1. 为修改的子模块创建PR（如适用）
2. 为主仓库创建PR
3. 确保PR描述清晰，包含更改摘要和测试信息

### 6. 代码审查

- 响应审查意见
- 进行必要的修改
- 确保CI检查通过

## 故障排除

### 常见问题

#### 1. 子模块更新问题

```bash
# 如果子模块没有正确更新
git submodule deinit -f .
git submodule update --init --recursive
```

#### 2. Maven构建失败

```bash
# 清理所有缓存
mvn clean
rm -rf ~/.m2/repository/org/logx

# 重新构建
mvn clean install
```

#### 3. 代码格式问题

```bash
# 自动修复格式问题
mvn formatter:format

# 检查格式
mvn formatter:validate
```

#### 4. 依赖冲突

```bash
# 查看依赖树
mvn dependency:tree

# 解决冲突后重新构建
mvn clean install -U
```

## BMAD 开发流程

本项目使用BMAD（Brownfield Methodology for Agile Development）开发方法论，包含以下核心组件：

### 核心配置文件
- `.bmad-core/core-config.yaml` - 项目核心配置
- `.bmad-core/agents/dev.md` - 开发者代理配置
- `.bmad-core/tasks/` - 任务模板
- `.bmad-core/checklists/` - 检查清单

### 开发代理使用

开发者可以使用BMAD开发代理来协助开发工作：

1. 激活开发者代理
2. 使用 `*help` 查看可用命令
3. 使用 `*develop-story` 执行用户故事开发
4. 使用 `*run-tests` 执行测试
5. 使用 `*review-qa` 应用QA修复

### 用户故事管理

用户故事文件位于 `docs/stories/` 目录下，按Epic组织：

- `1.x.*.md` - Epic 1: 核心基础设施
- `2.x.*.md` - Epic 2: 高性能异步队列
- `3.x.*.md` - Epic 3: 多框架适配器
- `4.x.*.md` - Epic 4: 生产就绪特性

## 项目文档

项目包含完整的文档体系：

- [架构设计文档](docs/architecture.md) - 详细的技术架构说明
- [产品需求文档](docs/prd.md) - 项目需求和Epic定义
- [开发者指南](docs/developer-guide.md) - 开发环境设置和贡献指南
- [Git管理指南](docs/git-management.md) - 分支策略、版本发布、协作流程
- [编码标准](docs/architecture/coding-standards.md) - 代码规范和最佳实践
- [技术栈](docs/architecture/tech-stack.md) - 技术选型和依赖管理
- [源码树结构](docs/architecture/source-tree.md) - 项目结构和包组织

## 性能目标和基准

### 核心性能指标

OSS Appender 设计了明确的性能目标，确保在生产环境中提供卓越的性能：

- **写入延迟**: < 1ms (99%分位数)
- **吞吐量**: > 10万条日志/秒
- **内存占用**: < 50MB
- **CPU占用**: < 5%

### 性能优化策略

1. **LMAX Disruptor队列**: 使用无锁环形缓冲区实现超低延迟
2. **批处理优化**: 智能批处理机制优化网络传输效率
3. **资源保护**: 固定线程池和低优先级调度确保不影响业务系统
4. **压缩传输**: 支持GZIP压缩减少网络带宽使用

### 已知性能问题

- Epic 2：`AsyncEngineIntegrationTest.shouldMeetLatencyTarget` 在当前容器/CI 环境下可能因性能抖动导致断言失败，不影响 Epic 1 交付与评审。

## 关键开发规则

### 规则1：中文沟通规范
必须使用中文沟通：
- 所有与用户的交流都必须使用中文
- 包括代码说明、进度报告、提交信息、测试输出等
- 只有技术关键词、API名称、配置键名可以使用英文

## 联系方式

如果在开发过程中遇到问题：

1. **查看文档**: [docs/](docs/)
2. **搜索Issues**: [GitHub Issues](https://github.com/logx-oss-appender/logx-oss-appender/issues)
3. **创建新Issue**: 详细描述问题和重现步骤
4. **讨论**: [GitHub Discussions](https://github.com/logx-oss-appender/logx-oss-appender/discussions)

---

*本文档最后更新于 2025-09-23*