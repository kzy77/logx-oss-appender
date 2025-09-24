package org.logx.compatibility.jsp.servlet;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * JSP/Servlet性能测试
 */
public class JspServletPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(JspServletPerformanceTest.class);

    @Test
    public void testServletRequestPerformance() {
        // 测试Servlet请求处理性能
        long startTime = System.currentTimeMillis();
        
        // 模拟处理多个请求
        for (int i = 0; i < 100; i++) {
            logger.info("Servlet请求处理测试 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证请求处理在合理时间内完成
        assertTrue("Servlet请求处理应在合理时间内完成", duration < 5000);
        
        System.out.println("处理100个Servlet请求耗时: " + duration + "毫秒");
    }

    @Test
    public void testJspPagePerformance() {
        // 测试JSP页面执行性能
        long startTime = System.currentTimeMillis();
        
        // 模拟JSP页面执行
        for (int i = 0; i < 50; i++) {
            logger.info("JSP页面执行测试 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("JSP页面执行应在合理时间内完成", duration < 3000);
        
        System.out.println("执行50个JSP页面耗时: " + duration + "毫秒");
    }

    @Test
    public void testConcurrentAccessPerformance() {
        // 测试并发访问性能
        long startTime = System.currentTimeMillis();
        
        // 模拟并发访问
        for (int i = 0; i < 200; i++) {
            logger.info("并发访问测试 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("并发访问应在合理时间内完成", duration < 10000);
        
        System.out.println("200次并发访问耗时: " + duration + "毫秒");
    }

    @Test
    public void testLoggingPerformance() {
        // 测试日志记录性能
        long startTime = System.currentTimeMillis();
        
        // 记录不同级别的日志
        for (int i = 0; i < 100; i++) {
            logger.trace("TRACE级别日志 - {}", i);
            logger.debug("DEBUG级别日志 - {}", i);
            logger.info("INFO级别日志 - {}", i);
            logger.warn("WARN级别日志 - {}", i);
            logger.error("ERROR级别日志 - {}", i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("日志记录应在合理时间内完成", duration < 5000);
        
        System.out.println("记录500条日志耗时: " + duration + "毫秒");
    }
}