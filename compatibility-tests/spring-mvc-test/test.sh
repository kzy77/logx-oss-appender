#!/bin/bash
# Spring MVC兼容性测试脚本

echo "开始Spring MVC兼容性测试..."

# 构建项目
echo "1. 构建项目..."
mvn clean package -pl compatibility-tests/spring-mvc-test -am

# 运行单元测试
echo "2. 运行单元测试..."
mvn test -pl compatibility-tests/spring-mvc-test

# 运行集成测试
echo "3. 运行集成测试..."
mvn verify -pl compatibility-tests/spring-mvc-test

# 部署到Tomcat进行集成测试（如果Tomcat可用）
echo "4. 部署到Tomcat进行集成测试..."
# 这里可以添加部署到Tomcat的脚本逻辑

# 运行性能测试
echo "5. 运行性能测试..."
mvn test -Dtest=SpringMVCPerformanceTest -pl compatibility-tests/spring-mvc-test

# 验证XML配置
echo "6. 验证XML配置..."
# 这里可以添加XML配置验证的脚本逻辑

echo "Spring MVC兼容性测试完成。"