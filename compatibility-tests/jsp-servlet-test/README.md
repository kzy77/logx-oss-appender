# JSP/Servlet兼容性测试应用

## 概述
此应用用于验证OSS Appender在传统JSP/Servlet环境中的兼容性。

## 功能特性
1. 验证web.xml配置方式的兼容性
2. 测试系统属性和环境变量配置
3. 验证与传统Web容器的兼容性
4. 执行性能基准测试

## 构建和部署

### 构建项目
```bash
mvn clean package
```

### 部署应用
将生成的WAR文件部署到支持Servlet 4.0的Web容器中，如Tomcat 9+。

## 测试端点
- `GET /test-log` - 生成各种级别的日志消息（Servlet）
- `GET /test-exception` - 生成异常日志消息（Servlet）
- `GET /test-log.jsp` - 生成各种级别的日志消息（JSP）

## 配置方式

### web.xml配置
使用 `web.xml` 文件配置Logback

### 系统属性配置
支持通过系统属性覆盖配置：
- `oss.appender.s3.bucket` - S3存储桶
- `oss.appender.s3.keyPrefix` - 对象key前缀
- `oss.appender.s3.region` - 存储区域
- `oss.appender.batch.size` - 批处理大小
- `oss.appender.batch.flushInterval` - 刷新间隔
- `oss.appender.queue.capacity` - 队列容量

### 环境变量配置
支持通过环境变量覆盖配置：
- `OSS_APPENDER_S3_BUCKET` - S3存储桶
- `OSS_APPENDER_S3_KEY_PREFIX` - 对象key前缀
- `OSS_APPENDER_S3_REGION` - 存储区域
- `OSS_APPENDER_BATCH_SIZE` - 批处理大小
- `OSS_APPENDER_BATCH_FLUSH_INTERVAL` - 刷新间隔
- `OSS_APPENDER_QUEUE_CAPACITY` - 队列容量