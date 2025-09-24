# 性能一致性验证工具

## 概述
此工具用于验证OSS Appender在不同框架中的性能表现一致性。

## 功能特性
1. 执行各框架的性能基准测试
2. 验证延迟指标的一致性
3. 测试吞吐量的一致性
4. 验证资源占用的一致性
5. 生成性能对比报告

## 构建和运行

### 构建项目
```bash
mvn clean package
```

### 运行性能测试
```bash
java -jar target/performance-consistency-benchmark.jar
```

或者运行特定的基准测试：
```bash
java -jar target/performance-consistency-benchmark.jar .*Logback.*
java -jar target/performance-consistency-benchmark.jar .*Log4j2.*
java -jar target/performance-consistency-benchmark.jar .*Log4j1.*
```

## 测试内容

### 吞吐量测试
测量每秒可以处理的日志消息数量：
- INFO级别日志
- DEBUG级别日志
- ERROR级别日志

### 延迟测试
测量单条日志消息的处理延迟：
- 平均延迟
- 99%分位数延迟
- 最大延迟

### 资源占用测试
测量不同负载下的资源占用情况：
- 内存使用量
- CPU使用率
- 线程数

## 性能指标

### 目标性能
- 写入延迟: < 1ms (99%分位数)
- 吞吐量: > 10万条日志/秒
- 内存占用: < 50MB
- CPU占用: < 5%

### 一致性要求
各框架之间的性能差异应控制在10%以内。