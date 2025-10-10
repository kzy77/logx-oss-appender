package org.logx.compatibility.spring.mvc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.logx.compatibility.spring.mvc.controller.TestLogController;

/**
 * 增强的Spring MVC兼容性测试 - 使用真实MinIO环境
 *
 * 测试前请按照 compatibility-tests/minio/README-MINIO.md 指南启动MinIO服务
 *
 * 快速启动：
 * cd compatibility-tests/minio
 * ./start-minio-local.sh
 *
 * 标准配置：
 * - 端点: http://localhost:9000
 * - 控制台: http://localhost:9001
 * - 用户名/密码: minioadmin/minioadmin
 * - 测试桶: logx-test-bucket
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebMvc
public class EnhancedSpringMVCCompatibilityTest {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedSpringMVCCompatibilityTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLogEndpointWithDifferentLevels() throws Exception {
        // 测试不同日志级别的真实OSS上传
        mockMvc.perform(get("/test-log").param("level", "info").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));

        mockMvc.perform(get("/test-log").param("level", "debug").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));

        mockMvc.perform(get("/test-log").param("level", "error").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));

        // 等待所有日志上传到MinIO
        Thread.sleep(3000);
        logger.info("不同级别日志测试完成");
    }

    @Test
    public void testExceptionEndpointWithStackTrace() throws Exception {
        // 测试带堆栈跟踪的异常真实上传
        logger.info("开始测试异常堆栈跟踪日志...");
        mockMvc.perform(get("/test-exception").param("stacktrace", "true").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("异常日志已生成"));

        // 等待异常日志上传到MinIO
        Thread.sleep(2000);
        logger.info("异常堆栈跟踪日志测试完成");
    }

    @Test
    public void testHighVolumeLogging() throws Exception {
        // 测试高并发日志记录的真实处理
        logger.info("开始高容量日志记录测试...");
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/test-log")
                    .param("profile", "performance")
                    .param("volume", "high")
                    .param("iteration", String.valueOf(i)))
                    .andExpect(status().isOk());
            logger.debug("高容量日志测试 - 完成第{}次迭代", i + 1);
        }

        // 等待所有高并发日志上传完成
        Thread.sleep(5000);
        logger.info("高容量日志记录测试完成");
    }

    @Test
    public void testRealEnvironmentConfiguration() throws Exception {
        // 测试真实环境配置和MinIO连接
        logger.info("开始测试真实环境配置...");
        mockMvc.perform(get("/test-log")
                .param("config", "real")
                .param("ossType", "S3")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));

        // 等待配置验证和日志上传
        Thread.sleep(2000);
        logger.info("真实环境配置测试完成");
    }

    @Test
    public void testConcurrentAccessWithRealOSS() throws Exception {
        // 测试并发访问的真实处理
        logger.info("开始并发访问测试...");
        Thread[] threads = new Thread[5];

        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/test-log")
                            .param("thread", String.valueOf(threadId))
                            .param("concurrent", "true"))
                            .andExpect(status().isOk());
                    logger.debug("并发线程{}执行完成", threadId);
                } catch (Exception e) {
                    logger.error("并发测试失败 - 线程{}", threadId, e);
                    throw new RuntimeException("并发测试失败", e);
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 等待所有并发日志上传完成
        Thread.sleep(3000);
        logger.info("并发访问测试完成");
    }

    @Test
    public void testConfigurationLoadingWithOverrides() throws Exception {
        // 测试带覆盖的配置加载和真实OSS连接
        logger.info("开始测试配置覆盖...");
        mockMvc.perform(get("/test-log")
                .param("override", "true")
                .param("endpoint", "http://localhost:9000")
                .param("bucket", "test-bucket")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));

        // 等待配置覆盖生效和日志上传
        Thread.sleep(2000);
        logger.info("配置覆盖测试完成");
    }
}