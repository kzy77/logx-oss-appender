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
支持通过系统属性覆盖配置（logx.oss前缀）：
- `logx.oss.endpoint` - 存储端点
- `logx.oss.region` - 存储区域
- `logx.oss.accessKeyId` - 访问密钥ID
- `logx.oss.accessKeySecret` - 秘密访问密钥
- `logx.oss.bucket` - 存储桶名称
- `logx.oss.keyPrefix` - 对象key前缀
- `logx.oss.pathStyleAccess` - 路径风格访问
- `logx.oss.enableSsl` - SSL启用

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