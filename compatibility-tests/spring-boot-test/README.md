# Spring Boot兼容性测试应用

## 概述
此应用用于验证OSS Appender在Spring Boot环境中的兼容性。

## 功能特性
1. 验证通过标准依赖引入的兼容性
2. 测试YAML和Properties配置方式
3. 验证环境变量配置覆盖
4. 执行性能基准测试

## 构建和运行

### 构建项目
```bash
mvn clean install
```

### 运行应用
```bash
mvn spring-boot:run
```

或者
```bash
java -jar target/spring-boot-compatibility-test-1.0.0-SNAPSHOT.jar
```

## 测试端点
- `GET /test-log` - 生成各种级别的日志消息
- `GET /test-exception` - 生成异常日志消息

## 配置方式

### YAML配置
使用 `application.yml` 文件配置

### Properties配置
使用 `application.properties` 文件配置

### 环境变量配置
支持通过环境变量覆盖配置（LOGX_OSS前缀）：
- `LOGX_OSS_ENDPOINT` - 存储端点
- `LOGX_OSS_REGION` - 存储区域
- `LOGX_OSS_ACCESS_KEY_ID` - 访问密钥ID
- `LOGX_OSS_ACCESS_KEY_SECRET` - 秘密访问密钥
- `LOGX_OSS_BUCKET` - 存储桶名称
- `LOGX_OSS_KEY_PREFIX` - 对象key前缀
- `LOGX_OSS_TYPE` - OSS类型
- `LOGX_OSS_MAX_UPLOAD_SIZE_MB` - 最大上传文件大小