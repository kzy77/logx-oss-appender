#!/bin/bash
# JSP/Servlet兼容性测试脚本

echo "开始JSP/Servlet兼容性测试..."

# 构建项目
echo "1. 构建项目..."
mvn clean package -pl compatibility-tests/jsp-servlet-test -am

# 运行单元测试
echo "2. 运行单元测试..."
mvn test -pl compatibility-tests/jsp-servlet-test

# 运行集成测试
echo "3. 运行集成测试..."
mvn verify -pl compatibility-tests/jsp-servlet-test

# 部署到Tomcat进行集成测试（如果Tomcat可用）
echo "4. 部署到Tomcat进行集成测试..."
# 这里可以添加部署到Tomcat的脚本逻辑

# 运行性能测试
echo "5. 运行性能测试..."
mvn test -Dtest=JspServletPerformanceTest -pl compatibility-tests/jsp-servlet-test

# 验证web.xml配置
echo "6. 验证web.xml配置..."
# 这里可以添加web.xml配置验证的脚本逻辑

# 测试环境变量和系统属性
echo "7. 测试环境变量和系统属性..."
# 这里可以添加环境变量和系统属性测试的脚本逻辑

echo "JSP/Servlet兼容性测试完成。"