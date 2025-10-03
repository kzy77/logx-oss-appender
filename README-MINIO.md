# MinIO集成测试环境使用指南

## 概述

已为LogX OSS Appender配置完整的MinIO集成测试环境，可模拟真实的对象存储服务进行功能测试。

## 文件清单

### 1. Docker配置文件

- **docker-compose.yml** - MinIO服务编排配置
  - MinIO API端口：9000
  - MinIO控制台：9001
  - 自动创建测试bucket：logx-test-bucket
  - 默认凭证：minioadmin/minioadmin

- **start-minio.sh** - 一键启动脚本
  - 自动检查Docker环境
  - 启动MinIO服务
  - 显示访问信息

### 2. 测试配置文件

- **logx-producer/src/test/resources/minio-test.properties**
  - MinIO连接配置
  - 测试环境专用参数（较小的触发阈值）

### 3. 集成测试类

- **logx-s3-adapter/src/test/java/org/logx/integration/MinIOIntegrationTest.java**
  - 基本上传功能测试
  - 消息数量触发测试
  - 时间触发测试
  - 压缩功能测试

### 4. 文档

- **docker/README.md** - Docker环境详细说明

## 快速开始

### 前置条件

✅ Docker已安装
✅ Docker守护进程正在运行
✅ 端口9000和9001可用

### 启动MinIO

```bash
# 方法1：使用一键启动脚本（推荐）
./start-minio.sh

# 方法2：手动启动
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f minio
```

### 访问MinIO控制台

浏览器访问：**http://localhost:9001**

```
用户名：minioadmin
密码：minioadmin
```

### 运行集成测试

```bash
# 运行所有MinIO集成测试
mvn test -Dtest=MinIOIntegrationTest -pl logx-s3-adapter

# 运行单个测试方法
mvn test -Dtest=MinIOIntegrationTest#shouldUploadLogsToMinIO -pl logx-s3-adapter
```

## MinIO配置信息

| 配置项 | 值 |
|-------|-----|
| API端点 | http://localhost:9000 |
| 控制台 | http://localhost:9001 |
| Access Key ID | minioadmin |
| Access Key Secret | minioadmin |
| 测试Bucket | logx-test-bucket |
| 对象前缀 | integration-test/ |

## 测试用例说明

### 1. shouldUploadLogsToMinIO
- **测试目标**：验证基本的日志上传功能
- **步骤**：发送150条测试日志
- **验证点**：
  - 文件成功上传到MinIO
  - 文件内容可正确解压
  - 日志内容格式正确

### 2. shouldTriggerUploadByMessageCount
- **测试目标**：验证消息数量触发机制
- **触发条件**：超过4096条消息
- **验证点**：达到阈值后自动触发上传

### 3. shouldTriggerUploadByTime
- **测试目标**：验证时间触发机制
- **触发条件**：10分钟超时
- **验证点**：超时后自动触发上传

## 环境变量配置

可通过环境变量覆盖默认配置：

```bash
export LOGX_OSS_ENDPOINT="http://localhost:9000"
export LOGX_OSS_ACCESS_KEY_ID="minioadmin"
export LOGX_OSS_ACCESS_KEY_SECRET="minioadmin"
export LOGX_OSS_BUCKET="logx-test-bucket"
export LOGX_OSS_KEY_PREFIX="integration-test/"
export LOGX_OSS_OSS_TYPE="S3"
```

## 停止MinIO服务

```bash
# 停止容器
docker-compose down

# 停止并删除数据卷（完全清理）
docker-compose down -v
```

## 常见问题

### 1. Docker守护进程未运行

**错误信息**：`Cannot connect to the Docker daemon`

**解决方法**：
- **macOS/Windows**：启动Docker Desktop
- **Linux**：`sudo systemctl start docker`

### 2. 端口被占用

**错误信息**：`port is already allocated`

**解决方法**：修改`docker-compose.yml`中的端口映射
```yaml
ports:
  - "19000:9000"  # 修改主机端口
  - "19001:9001"
```

### 3. Bucket未自动创建

**解决方法**：手动创建bucket
```bash
docker exec -it logx-minio mc alias set myminio http://localhost:9000 minioadmin minioadmin
docker exec -it logx-minio mc mb myminio/logx-test-bucket
```

### 4. 权限错误

**错误信息**：`permission denied`

**解决方法**：
```bash
# Linux环境需要root权限启动Docker
sudo docker-compose up -d
```

## 测试环境配置

测试环境使用较小的阈值以便快速触发：

| 配置项 | 测试环境 | 生产环境 |
|-------|---------|---------|
| maxBatchCount | 100条 | 4096条 |
| maxBatchBytes | 1MB | 10MB |
| maxMessageAgeMs | 5秒 | 10分钟 |
| queueCapacity | 1024 | 8192 |

## 性能测试建议

1. **吞吐量测试**：使用大量消息测试系统承载能力
2. **并发测试**：多线程同时写入测试并发性能
3. **故障恢复测试**：停止MinIO服务测试兜底机制
4. **压缩效果测试**：对比压缩前后的文件大小

## 下一步

1. 在宿主机启动Docker守护进程
2. 运行`./start-minio.sh`启动MinIO
3. 执行集成测试验证功能
4. 查看MinIO控制台确认文件上传

## 注意事项

⚠️ **当前环境限制**：
- Docker守护进程需要在宿主机或支持Docker的环境中运行
- 容器化环境中无法直接启动Docker守护进程（需要特权模式）
- 建议在本地开发环境运行集成测试

✅ **已完成配置**：
- MinIO Docker编排配置
- 自动化启动脚本
- 测试配置文件
- 集成测试代码（已移除@Disabled注解）

## 技术支持

- **MinIO文档**：https://min.io/docs/minio/linux/index.html
- **Docker文档**：https://docs.docker.com/
- **项目文档**：docs/architecture/
