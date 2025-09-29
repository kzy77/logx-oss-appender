package org.logx.compatibility.spring.boot;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot性能基准测试
 */
@SpringBootTest
public class SpringBootPerformanceBenchmarkTest {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootPerformanceBenchmarkTest.class);
    
    @Test
    public void testLoggingPerformance() {
        // 执行简单的性能测试
        long startTime = System.currentTimeMillis();
        int logCount = 1000;
        
        for (int i = 0; i < logCount; i++) {
            logger.info("性能测试日志消息 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证日志记录在合理时间内完成
        assertTrue(duration < 5000, "日志记录应在5秒内完成");
        
        logger.info("记录{}条日志耗时: {}毫秒", logCount, duration);
    }
    
    @Test
    public void testDifferentLogLevelPerformance() {
        // 测试不同日志级别的性能
        long startTime = System.currentTimeMillis();
        
        // INFO级别日志
        for (int i = 0; i < 100; i++) {
            logger.info("INFO级别日志 - {}", i);
        }
        
        // WARN级别日志
        for (int i = 0; i < 100; i++) {
            logger.warn("WARN级别日志 - {}", i);
        }
        
        // ERROR级别日志
        for (int i = 0; i < 100; i++) {
            logger.error("ERROR级别日志 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 3000, "不同级别日志记录应在3秒内完成");
        
        logger.info("不同级别日志记录耗时: {}毫秒", duration);
    }
}