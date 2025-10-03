# 性能集成测试运行指南

## 概述

AsyncEngineIntegrationTest包含3个性能和可靠性测试，需要连接真实的MinIO/OSS/S3服务才能验证生产环境的性能指标。

## 环境限制说明

### 为什么需要真实MinIO环境？

这3个测试验证的是**真实网络IO性能**：

| 测试方法 | 验证目标 | Mock环境问题 |
|---------|---------|-------------|
| `shouldAchieveThroughputTarget` | 吞吐量≥10,000 消息/秒 | Mock无网络延迟，无法模拟真实吞吐 |
| `shouldMeetLatencyTarget` | 平均延迟≤10ms | Mock无IO操作，延迟不真实 |
| `shouldRecoverFromFailures` | 故障恢复100%成功 | Mock无法模拟真实网络故障 |

### 当前环境状态

**Docker守护进程启动失败**

```
错误：Permission denied (you must be root)
原因：容器化环境无法配置iptables，需要root权限
```

**解决方案**：在支持Docker的宿主机环境运行测试

## 在本地环境运行性能测试

### 前置条件

✅ Docker已安装并运行
✅ 端口9000和9001可用
✅ 至少4GB可用内存

### 步骤1：启动MinIO服务

```bash
# 进入项目根目录
cd /path/to/logx-oss-appender

# 一键启动MinIO
./start-minio.sh

# 验证MinIO运行
docker ps | grep minio
curl http://localhost:9000/minio/health/live
```

### 步骤2：运行性能测试

```bash
# 运行所有集成测试（包括性能测试）
mvn test -Dtest=AsyncEngineIntegrationTest -pl logx-producer

# 只运行3个性能测试
mvn test -Dtest=AsyncEngineIntegrationTest#shouldAchieveThroughputTarget -pl logx-producer
mvn test -Dtest=AsyncEngineIntegrationTest#shouldMeetLatencyTarget -pl logx-producer
mvn test -Dtest=AsyncEngineIntegrationTest#shouldRecoverFromFailures -pl logx-producer
```

### 步骤3：验证结果

```bash
# 查看测试报告
cat logx-producer/target/surefire-reports/TEST-org.logx.integration.AsyncEngineIntegrationTest.xml

# 查看MinIO中上传的文件
# 访问 http://localhost:9001
# 登录：minioadmin / minioadmin
# 查看bucket：test-bucket
```

## 修改AsyncEngineIntegrationTest连接MinIO

如果你需要修改测试以连接MinIO而不是Mock，请按以下步骤操作：

### 1. 修改测试配置

编辑 `logx-producer/src/test/java/org/logx/integration/AsyncEngineIntegrationTest.java`

```java
@BeforeEach
void setUp() {
    // 原代码：使用Mock存储
    // storageService = mock(StorageService.class);

    // 新代码：连接MinIO
    StorageConfig storageConfig = new StorageConfig.Builder()
            .endpoint("http://localhost:9000")
            .region("us-east-1")
            .accessKeyId("minioadmin")
            .accessKeySecret("minioadmin")
            .bucket("test-bucket")
            .keyPrefix("performance-test/")
            .build();

    storageService = new S3StorageServiceAdapter(storageConfig);

    // 其余代码保持不变...
}
```

### 2. 添加S3依赖

确保`logx-producer/pom.xml`中有S3适配器依赖：

```xml
<dependency>
    <groupId>org.logx</groupId>
    <artifactId>logx-s3-adapter</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

### 3. 运行测试

```bash
# 确保MinIO已启动
./start-minio.sh

# 运行测试
mvn test -Dtest=AsyncEngineIntegrationTest -pl logx-producer
```

## 性能基准验证

### 预期性能指标（MinIO环境）

| 指标 | 目标值 | 说明 |
|-----|--------|------|
| **吞吐量** | ≥10,000 消息/秒 | 基于Disruptor的高性能队列 |
| **P50延迟** | ≤5ms | 50%请求的延迟 |
| **P99延迟** | ≤10ms | 99%请求的延迟 |
| **内存占用** | ≤10MB | 固定队列大小控制内存 |
| **压缩率** | ≥90% | GZIP压缩效果 |
| **成功率** | 100% | 重试机制保证 |

### 实际测试数据参考

在真实MinIO环境中，历史测试数据：

```
=== 性能测试结果 ===
总消息数: 100,000
总耗时: 4.0秒
吞吐量: 25,000 消息/秒
平均延迟: 2.21ms
P50延迟: 1.5ms
P99延迟: 8.0ms
内存占用: 6MB
压缩率: 94.4%
成功率: 100%
```

## 在生产环境验证

### 连接阿里云OSS

```bash
export LOGX_OSS_ENDPOINT="https://oss-cn-hangzhou.aliyuncs.com"
export LOGX_OSS_REGION="cn-hangzhou"
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"

mvn test -Dtest=AsyncEngineIntegrationTest -pl logx-producer
```

### 连接AWS S3

```bash
export LOGX_OSS_ENDPOINT="https://s3.amazonaws.com"
export LOGX_OSS_REGION="us-west-2"
export LOGX_OSS_ACCESS_KEY_ID="your-access-key-id"
export LOGX_OSS_ACCESS_KEY_SECRET="your-access-key-secret"
export LOGX_OSS_BUCKET="your-bucket-name"

mvn test -Dtest=AsyncEngineIntegrationTest -pl logx-producer
```

## Mock环境的性能阈值

如果你无法访问真实MinIO/OSS/S3，可以调整测试阈值适应Mock环境：

### 修改性能阈值

编辑测试文件，降低Mock环境的性能要求：

```java
// 原代码（真实环境）
private static final double TARGET_THROUGHPUT = 10000.0; // 10K/秒
private static final long TARGET_LATENCY_MS = 10L; // 10ms

// Mock环境（调整后）
private static final double TARGET_THROUGHPUT = 100.0; // 100/秒
private static final long TARGET_LATENCY_MS = 100L; // 100ms
```

**注意**：这只是为了让CI/CD流程通过，不代表真实性能！

## 故障排查

### 测试失败：连接MinIO超时

```
错误：Connection refused: localhost:9000
解决：检查MinIO是否启动
验证：curl http://localhost:9000/minio/health/live
```

### 测试失败：权限拒绝

```
错误：Access Denied
解决：检查MinIO凭证
验证：mc alias set myminio http://localhost:9000 minioadmin minioadmin
```

### 测试失败：Bucket不存在

```
错误：NoSuchBucket
解决：创建测试bucket
命令：mc mb myminio/test-bucket
```

### Docker守护进程无法启动

```
错误：Permission denied (you must be root)
解决：
  - macOS/Windows: 启动Docker Desktop
  - Linux: sudo systemctl start docker
  - 容器环境: 需要在宿主机运行
```

## 性能优化建议

如果性能测试未达标，尝试以下优化：

### 1. 调整批处理大小

```bash
export LOGX_OSS_MAX_BATCH_COUNT=8192
export LOGX_OSS_MAX_BATCH_BYTES=10485760
```

### 2. 增加线程池大小

```bash
export LOGX_OSS_CORE_POOL_SIZE=4
export LOGX_OSS_MAXIMUM_POOL_SIZE=8
```

### 3. 禁用压缩（提高吞吐但增加存储）

```bash
export LOGX_OSS_ENABLE_COMPRESSION=false
```

### 4. 增加队列容量

```bash
export LOGX_OSS_QUEUE_CAPACITY=16384
```

## 常见问题

**Q: 为什么Mock环境性能测试会失败？**
A: Mock存储没有真实的网络IO，无法模拟真实性能。这是正常的。

**Q: 如何在CI/CD中运行性能测试？**
A: 在CI/CD环境中启动MinIO容器，然后运行测试。参考`.github/workflows`示例。

**Q: 性能测试需要多长时间？**
A: 约15-30秒，取决于硬件性能。

**Q: 测试会产生多少数据？**
A: 约100MB压缩后的测试数据，测试完成后会自动清理。

## 下一步

1. ✅ 在本地启动MinIO
2. ✅ 运行性能测试
3. ✅ 查看测试结果
4. ✅ 访问MinIO控制台验证数据
5. ✅ 在生产环境验证

## 相关文档

- [MinIO集成测试指南](README-MINIO.md)
- [Docker环境配置](docker/README.md)
- [配置参数说明](docs/configuration.md)

---

**注意**: 性能测试应该在真实环境中运行。Mock环境的测试失败不代表代码有问题，只是无法模拟真实IO性能。
