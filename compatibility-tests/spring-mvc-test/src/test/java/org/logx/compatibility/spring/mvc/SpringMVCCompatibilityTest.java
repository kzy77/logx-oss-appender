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
 * Spring MVC兼容性测试 - 使用真实MinIO环境
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
public class SpringMVCCompatibilityTest {

    private static final Logger logger = LoggerFactory.getLogger(SpringMVCCompatibilityTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testBusinessLogGenerationWithRealOSS() throws Exception {
        // 测试真实的业务日志生成和OSS上传功能
        String result = mockMvc.perform(get("/test-log")
                .param("level", "all")
                .param("count", "20")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andReturn().getResponse().getContentAsString();

        logger.info("业务日志生成结果: {}", result);

        // 等待异步上传完成
        Thread.sleep(3000);
    }

    @Test
    public void testExceptionLogWithStackTraceRealOSS() throws Exception {
        // 测试真实的异常日志记录和OSS上传（包含堆栈跟踪）
        String result = mockMvc.perform(get("/test-exception")
                .param("stacktrace", "true")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andReturn().getResponse().getContentAsString();

        logger.info("异常日志生成结果: {}", result);

        // 等待异步上传完成
        Thread.sleep(2000);
    }

    @Test
    public void testDifferentLogLevelsWithRealOSS() throws Exception {
        // 测试不同日志级别的真实记录
        logger.info("测试INFO级别日志...");
        String infoResult = mockMvc.perform(get("/test-log")
                .param("level", "info")
                .param("count", "15")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andReturn().getResponse().getContentAsString();
        logger.info("INFO级别结果: {}", infoResult);

        logger.info("测试DEBUG级别日志...");
        String debugResult = mockMvc.perform(get("/test-log")
                .param("level", "debug")
                .param("count", "10")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        logger.info("DEBUG级别结果: {}", debugResult);

        logger.info("测试ERROR级别日志...");
        String errorResult = mockMvc.perform(get("/test-log")
                .param("level", "error")
                .param("count", "8")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        logger.info("ERROR级别结果: {}", errorResult);

        // 等待所有异步上传完成
        Thread.sleep(5000);
    }

    @Test
    public void testHighVolumeBusinessLogsWithRealOSS() throws Exception {
        // 测试高容量业务日志生成
        logger.info("开始高容量业务日志测试...");
        String result = mockMvc.perform(get("/test-high-volume")
                .param("volume", "50")
                .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andReturn().getResponse().getContentAsString();

        logger.info("高容量日志生成结果: {}", result);

        // 等待大量日志异步上传完成
        Thread.sleep(8000);
    }
}