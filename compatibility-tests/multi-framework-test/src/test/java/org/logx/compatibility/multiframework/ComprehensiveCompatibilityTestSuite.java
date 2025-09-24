package org.logx.compatibility.multiframework;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * 综合兼容性测试套件
 * 包含所有多框架共存相关的测试
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    MultiFrameworkCoexistenceTestTest.class,
    ConfigurationIsolationTest.class,
    LogOutputConsistencyTest.class,
    ResourceCompetitionTest.class,
    MultiFrameworkPerformanceTest.class
})
public class ComprehensiveCompatibilityTestSuite {
    // 测试套件入口点
}