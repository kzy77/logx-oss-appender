# 兼容性测试环境设置

## 概述

兼容性测试已全面升级为真实环境测试，不再使用Mock模拟。所有测试都基于真实的MinIO环境进行，确保测试结果的准确性和可靠性。

## MinIO环境设置

⚠️ **重要提示**: 请参考 [`minio/README-MINIO.md`](minio/README-MINIO.md) 获取完整的MinIO环境设置指南。

### 快速启动（推荐方式）

```bash
# 进入MinIO环境目录
cd compatibility-tests/minio

# 使用启动脚本（推荐）
./start-minio-local.sh
```

### 手动安装MinIO（Linux/macOS）

```bash
# 下载MinIO服务端和客户端
wget https://dl.min.io/server/minio/release/linux-amd64/minio
wget https://dl.min.io/client/mc/release/linux-amd64/mc

# 添加执行权限
chmod +x minio mc

# 启动MinIO服务
MINIO_ROOT_USER=minioadmin \
MINIO_ROOT_PASSWORD=minioadmin \
./minio server ~/minio-data --console-address ":9001"

# 创建测试桶
./mc alias set local http://localhost:9000 minioadmin minioadmin
./mc mb local/logx-test-bucket
```

### 验证MinIO服务

1. **WebUI访问**: http://localhost:9001
   - 用户名: `minioadmin`
   - 密码: `minioadmin`

2. **API访问**: http://localhost:9000

3. **测试桶**: `logx-test-bucket`

## 标准配置参数

按照 `minio/README-MINIO.md` 规范，使用以下标准配置：

```bash
export LOGX_OSS_ENDPOINT="http://localhost:9000"
export LOGX_OSS_ACCESS_KEY_ID="minioadmin"
export LOGX_OSS_ACCESS_KEY_SECRET="minioadmin"
export LOGX_OSS_BUCKET="logx-test-bucket"
export LOGX_OSS_KEY_PREFIX="integration-test/"
export LOGX_OSS_OSS_TYPE="S3"
export LOGX_OSS_REGION="ap-guangzhou"
```

## 测试模块说明

### 1. spring-mvc-test
- **目的**: 测试Spring MVC框架集成
- **真实业务日志功能**:
  - 多级别业务日志生成（TRACE/DEBUG/INFO/WARN/ERROR）
  - 电商业务场景模拟（商品查询、订单处理、库存管理）
  - 性能监控日志（内存使用、CPU使用率、响应时间）
  - 异常处理日志（支付失败、连接超时等）
  - 高容量日志生成测试（50+条业务日志）
- **测试端点**:
  - `/test-log` - 业务日志生成（支持level、count参数）
  - `/test-exception` - 异常日志生成（支持stacktrace参数）
  - `/test-high-volume` - 高容量日志测试

### 2. jsp-servlet-test
- **目的**: 测试JSP/Servlet环境集成
- **真实业务日志功能**:
  - Servlet环境下的业务日志生成
  - 多业务分类（订单业务、安全审计、数据查询）
  - HTTP请求信息记录（URI、参数、客户端信息）
  - 业务异常处理（支付失败、系统异常）
  - 并发Servlet访问测试
- **业务分类**:
  - `order` - 订单业务日志
  - `security` - 安全审计日志
  - `general` - 通用业务日志

### 3. spring-boot-test
- **目的**: 测试Spring Boot集成
- **真实业务日志功能**:
  - **电商业务日志**: 订单创建、库存更新、物流信息、支付处理
  - **金融业务日志**: 交易记录、风控检查、合规审计、账户监控
  - **系统监控日志**: JVM监控、数据库监控、接口性能监控
  - **高并发测试**: 多线程异步日志生成，模拟真实高并发场景
  - **混合业务场景**: 同时模拟多种业务类型的日志生成
  - **性能压力测试**: 大量日志生成，测试系统承载能力

### 4. config-consistency-test
- **目的**: 测试配置一致性
- **真实功能**:
  - 真实MinIO配置验证
  - 环境变量读取测试
  - 配置参数一致性检查

### 5. multi-framework-test
- **目的**: 测试多框架兼容性
- **真实功能**:
  - Log4j/Log4j2/Logback兼容性
  - 真实日志框架集成
  - 跨框架配置一致性

## 运行测试

### 前置条件
1. 确保MinIO服务已启动并运行正常
2. 创建必要的测试桶 (`logx-test-bucket`)
3. 设置正确的环境变量

### 执行测试
```bash
# 运行所有兼容性测试
mvn clean install

# 运行特定模块测试
cd spring-mvc-test
mvn test

# 运行特定测试类
mvn test -Dtest=SpringMVCCompatibilityTest
```

## 测试验证

### 查看上传的日志文件
1. 访问MinIO WebUI: http://localhost:9001
2. 登录后查看 `logx-test-bucket` 桶
3. 验证日志文件是否成功上传

### 监控测试日志
测试过程中会输出详细的日志信息，包括：
- OSS连接状态
- 日志上传进度
- 错误信息（如有）

## 故障排除

### 常见问题

1. **MinIO连接失败**
   - 检查MinIO服务是否正常运行
   - 验证端口9000是否可访问
   - 确认环境变量设置正确

2. **桶不存在错误**
   - 确保已创建 `logx-test-bucket`
   - 检查桶名称拼写是否正确

3. **认证失败**
   - 验证访问密钥是否正确
   - 确认MinIO用户权限设置

### 调试命令

```bash
# 检查MinIO服务状态
curl http://localhost:9000/minio/health/live

# 列出所有桶
mc ls local

# 查看特定桶内容
mc ls local/logx-test-bucket
```

## 测试数据清理

测试完成后，可以清理测试数据：

```bash
# 删除测试桶中的所有对象
mc rm --recursive local/logx-test-bucket

# 重新创建空桶
mc mb local/logx-test-bucket
```

## 注意事项

1. **真实环境**: 所有测试都在真实MinIO环境中运行，产生的日志文件会实际存储
2. **异步处理**: 日志上传是异步的，测试中包含适当的等待时间
3. **并发安全**: 测试验证了并发场景下的系统稳定性
4. **配置统一**: 所有测试模块使用统一的MinIO配置规范

## 性能基准

根据架构文档调整后的性能指标：

### 核心性能要求
| 指标 | 目标值 | 说明 |
|------|--------|------|
| 吞吐量 | 100,000条日志/秒 | 高并发处理能力 |
| 日志无丢失 | 零丢失率 | 在高吞吐量负载下确保数据完整性 |
| 队列内存占用 | < 512MB | 内存高效使用，避免OOM |

### 测试验证标准
- **吞吐量测试**: 生成10万条日志，验证实际处理速度
- **无丢失率测试**: 高负载下验证日志完整性，需对比MinIO上传文件数量
- **内存控制测试**: 多场景下监控队列内存峰值使用情况
- **高并发测试**: 20线程并发处理，每线程5000条日志

### 压测修正机制
如果标准指标在特定环境下无法达到，系统会自动基于实际测试结果提供修正建议：
- 吞吐量修正目标: 基于实际测试结果的80%作为保守目标
- 内存修正目标: 基于实际峰值的120%作为安全边际

## 详细文档

如需了解更多详细信息，请参考：
- [MinIO环境设置指南](minio/README-MINIO.md) - 完整的MinIO安装和配置指南
- [Docker部署方式](minio/docker/README.md) - 使用Docker部署MinIO（可选）
- 各测试模块的README.md文件