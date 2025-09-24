#!/bin/bash
# Spring Boot兼容性测试脚本

echo "开始Spring Boot兼容性测试..."

# 构建项目
echo "1. 构建项目..."
mvn clean install -pl compatibility-tests/spring-boot-test -am

# 运行单元测试
echo "2. 运行单元测试..."
mvn test -pl compatibility-tests/spring-boot-test

# 运行集成测试
echo "3. 运行集成测试..."
mvn verify -pl compatibility-tests/spring-boot-test

# 启动应用（后台运行）
echo "4. 启动应用..."
mvn spring-boot:run -pl compatibility-tests/spring-boot-test &
APP_PID=$!

# 等待应用启动
sleep 10

# 测试日志端点
echo "5. 测试日志端点..."
curl -s http://localhost:8080/test-log
echo ""

# 测试异常端点
echo "6. 测试异常端点..."
curl -s http://localhost:8080/test-exception
echo ""

# 停止应用
echo "7. 停止应用..."
kill $APP_PID

# 运行性能测试
echo "8. 运行性能测试..."
mvn test -Dtest=SpringBootPerformanceBenchmarkTest -pl compatibility-tests/spring-boot-test

echo "Spring Boot兼容性测试完成。"