#!/bin/bash
# 多框架共存兼容性测试脚本

echo "开始多框架共存兼容性测试..."

# 构建项目
echo "1. 构建项目..."
mvn clean compile -pl compatibility-tests/multi-framework-test -am

# 运行单元测试
echo "2. 运行单元测试..."
mvn test -pl compatibility-tests/multi-framework-test

# 运行集成测试
echo "3. 运行集成测试..."
mvn verify -pl compatibility-tests/multi-framework-test

# 运行配置隔离测试
echo "4. 运行配置隔离测试..."
mvn test -Dtest=ConfigurationIsolationTest -pl compatibility-tests/multi-framework-test

# 运行资源竞争测试
echo "5. 运行资源竞争测试..."
mvn test -Dtest=ResourceCompetitionTest -pl compatibility-tests/multi-framework-test

# 运行日志输出一致性测试
echo "6. 运行日志输出一致性测试..."
mvn test -Dtest=LogOutputConsistencyTest -pl compatibility-tests/multi-framework-test

# 运行性能测试
echo "7. 运行性能测试..."
mvn test -Dtest=MultiFrameworkPerformanceTest -pl compatibility-tests/multi-framework-test

# 验证多框架共存
echo "8. 验证多框架共存..."
java -cp target/classes:target/dependency/* org.logx.compatibility.multiframework.MultiFrameworkCoexistenceTest

echo "多框架共存兼容性测试完成。"