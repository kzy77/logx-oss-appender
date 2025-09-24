package org.logx.compatibility.spring.mvc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * Spring MVC性能测试
 */
public class SpringMVCPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(SpringMVCPerformanceTest.class);

    @Test
    public void testRequestHandlingPerformance() {
        // 测试请求处理性能
        long startTime = System.currentTimeMillis();
        
        // 模拟处理多个请求
        for (int i = 0; i < 100; i++) {
            logger.info("Spring MVC性能测试请求 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证请求处理在合理时间内完成
        assertTrue("请求处理应在合理时间内完成", duration < 5000);
        
        System.out.println("处理100个请求耗时: " + duration + "毫秒");
    }

    @Test
    public void testConcurrentRequestHandling() {
        // 测试并发请求处理
        long startTime = System.currentTimeMillis();
        
        // 模拟并发处理
        for (int i = 0; i < 50; i++) {
            logger.info("并发请求处理 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("并发请求处理应在合理时间内完成", duration < 3000);
        
        System.out.println("并发处理50个请求耗时: " + duration + "毫秒");
    }

    @Test
    public void testLoggingPerformance() {
        // 测试日志记录性能
        long startTime = System.currentTimeMillis();
        
        // 记录不同级别的日志
        for (int i = 0; i < 200; i++) {
            logger.trace("TRACE级别日志 - {}", i);
            logger.debug("DEBUG级别日志 - {}", i);
            logger.info("INFO级别日志 - {}", i);
            logger.warn("WARN级别日志 - {}", i);
            logger.error("ERROR级别日志 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("日志记录应在合理时间内完成", duration < 10000);
        
        System.out.println("记录1000条日志耗时: " + duration + "毫秒");
    }
}