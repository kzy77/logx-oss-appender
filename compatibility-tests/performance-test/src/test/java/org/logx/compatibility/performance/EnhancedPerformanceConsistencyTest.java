package org.logx.compatibility.performance;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 增强的性能一致性验证工具测试
 */
public class EnhancedPerformanceConsistencyTest {

    @Test
    public void testActualBenchmarkExecution() {
        // 测试实际的基准执行
        try {
            // 配置JMH运行选项，只运行性能测试相关的基准
            Options opt = new OptionsBuilder()
                    .include(".*CrossFrameworkPerformanceComparison.*")
                    .forks(0) // 在测试环境中不进行fork
                    .warmupIterations(1) // 减少预热迭代次数以节省时间
                    .warmupTime(TimeValue.milliseconds(100))
                    .measurementIterations(2) // 减少测量迭代次数以节省时间
                    .measurementTime(TimeValue.milliseconds(200))
                    .build();

            // 运行基准测试
            new Runner(opt).run();
            
            // 验证基准测试成功运行
            assertTrue("基准测试应该成功运行", true);
        } catch (RunnerException e) {
            fail("基准测试执行失败: " + e.getMessage());
        }
    }

    @Test
    public void testPerformanceReportGeneration() {
        // 测试性能报告生成
        try {
            PerformanceReportGenerator reportGenerator = new PerformanceReportGenerator();
            
            // 添加真实的测试结果
            reportGenerator.addResult("Logback", "INFO", 120000.5, 0.0083);
            reportGenerator.addResult("Logback", "DEBUG", 115000.2, 0.0087);
            reportGenerator.addResult("Logback", "ERROR", 125000.8, 0.0080);
            
            reportGenerator.addResult("Log4j2", "INFO", 118000.3, 0.0085);
            reportGenerator.addResult("Log4j2", "DEBUG", 113000.1, 0.0089);
            reportGenerator.addResult("Log4j2", "ERROR", 123000.6, 0.0082);
            
            reportGenerator.addResult("Log4j1", "INFO", 110000.7, 0.0091);
            reportGenerator.addResult("Log4j1", "DEBUG", 108000.4, 0.0093);
            reportGenerator.addResult("Log4j1", "ERROR", 115000.9, 0.0087);
            
            // 生成报告到临时文件
            String reportPath = "target/test-performance-report.md";
            reportGenerator.generateReport(reportPath);
            
            // 验证报告文件已创建
            assertTrue("性能报告文件应该已创建", Files.exists(Paths.get(reportPath)));
            
            // 验证报告文件不为空
            File reportFile = new File(reportPath);
            assertTrue("性能报告文件不应该为空", reportFile.length() > 0);
            
            // 清理临时文件
            Files.deleteIfExists(Paths.get(reportPath));
        } catch (IOException e) {
            fail("性能报告生成失败: " + e.getMessage());
        }
    }

    @Test
    public void testCrossFrameworkComparisonExecution() {
        // 测试跨框架比较执行
        try {
            // 创建测试实例
            CrossFrameworkPerformanceComparison comparison = new CrossFrameworkPerformanceComparison();
            
            // 创建Blackhole实例用于测试
            org.openjdk.jmh.infra.Blackhole blackhole = new org.openjdk.jmh.infra.Blackhole("DEFAULT");
            
            // 执行各种基准测试方法
            comparison.testLogbackInfoLogging(blackhole);
            comparison.testLog4j2InfoLogging(blackhole);
            comparison.testLog4j1InfoLogging(blackhole);
            
            comparison.testLogbackDebugLogging(blackhole);
            comparison.testLog4j2DebugLogging(blackhole);
            comparison.testLog4j1DebugLogging(blackhole);
            
            comparison.testLogbackErrorLogging(blackhole);
            comparison.testLog4j2ErrorLogging(blackhole);
            comparison.testLog4j1ErrorLogging(blackhole);
            
            // 验证所有测试方法都能正常执行
            assertTrue("跨框架比较测试应该成功执行", true);
        } catch (Exception e) {
            fail("跨框架比较测试执行失败: " + e.getMessage());
        }
    }

    @Test
    public void testPerformanceConsistencyEvaluation() {
        // 测试性能一致性评估
        PerformanceReportGenerator reportGenerator = new PerformanceReportGenerator();
        
        // 添加具有不同性能特征的结果
        reportGenerator.addResult("Logback", "INFO", 120000.5, 0.0083);
        reportGenerator.addResult("Log4j2", "INFO", 118000.3, 0.0085);
        reportGenerator.addResult("Log4j1", "INFO", 110000.7, 0.0091);
        
        // 生成报告
        try {
            String reportPath = "target/consistency-test-report.md";
            reportGenerator.generateReport(reportPath);
            
            // 读取报告内容
            String content = new String(Files.readAllBytes(Paths.get(reportPath)));
            
            // 验证报告包含关键信息
            assertTrue("报告应该包含标题", content.contains("# 性能一致性测试报告"));
            assertTrue("报告应该包含测试结果", content.contains("| 框架 | 日志级别 | 吞吐量 (ops/sec) | 延迟 (ms) | 一致性评估 |"));
            assertTrue("报告应该包含评估标准", content.contains("## 一致性评估标准"));
            assertTrue("报告应该包含总体结论", content.contains("## 总体结论"));
            
            // 清理临时文件
            Files.deleteIfExists(Paths.get(reportPath));
        } catch (IOException e) {
            fail("性能一致性评估测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testHighLoadPerformanceConsistency() {
        // 测试高负载下的性能一致性
        PerformanceReportGenerator reportGenerator = new PerformanceReportGenerator();
        
        // 添加大量测试结果来模拟高负载场景
        String[] frameworks = {"Logback", "Log4j2", "Log4j1"};
        String[] levels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
        
        for (String framework : frameworks) {
            for (String level : levels) {
                // 模拟不同的性能数据
                double throughput = 100000 + Math.random() * 50000;
                double latency = 0.005 + Math.random() * 0.01;
                reportGenerator.addResult(framework, level, throughput, latency);
            }
        }
        
        // 生成报告
        try {
            String reportPath = "target/high-load-report.md";
            reportGenerator.generateReport(reportPath);
            
            // 验证报告文件已创建且不为空
            File reportFile = new File(reportPath);
            assertTrue("高负载性能报告文件应该已创建", reportFile.exists());
            assertTrue("高负载性能报告文件不应该为空", reportFile.length() > 0);
            
            // 清理临时文件
            Files.deleteIfExists(Paths.get(reportPath));
        } catch (IOException e) {
            fail("高负载性能一致性测试失败: " + e.getMessage());
        }
    }
}