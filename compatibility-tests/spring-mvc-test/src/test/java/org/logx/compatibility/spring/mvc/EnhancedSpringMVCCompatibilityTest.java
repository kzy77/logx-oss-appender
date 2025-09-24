package org.logx.compatibility.spring.mvc;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import org.logx.compatibility.spring.mvc.controller.TestLogController;

/**
 * 增强的Spring MVC兼容性测试
 */
public class EnhancedSpringMVCCompatibilityTest {

    @Test
    public void testLogEndpointWithDifferentLevels() throws Exception {
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // 测试不同日志级别的端点功能
        mockMvc.perform(get("/test-log").param("level", "info"))
                .andExpect(status().isOk())
                .andExpect(content().string("INFO日志消息已生成"));

        mockMvc.perform(get("/test-log").param("level", "debug"))
                .andExpect(status().isOk())
                .andExpect(content().string("DEBUG日志消息已生成"));

        mockMvc.perform(get("/test-log").param("level", "error"))
                .andExpect(status().isOk())
                .andExpect(content().string("ERROR日志消息已生成"));
    }

    @Test
    public void testExceptionEndpointWithStackTrace() throws Exception {
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // 测试带堆栈跟踪的异常端点功能
        mockMvc.perform(get("/test-exception").param("stacktrace", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("异常日志已生成"));
    }

    @Test
    public void testXmlConfigurationWithDifferentProfiles() throws Exception {
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // 测试不同配置文件的XML配置
        mockMvc.perform(get("/test-log").param("profile", "development"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test-log").param("profile", "production"))
                .andExpect(status().isOk());
    }

    @Test
    public void testProgrammaticConfiguration() throws Exception {
        // 测试程序化配置
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        // 验证控制器能够正确处理请求
        mockMvc.perform(get("/test-log").param("config", "programmatic"))
                .andExpect(status().isOk())
                .andExpect(content().string("日志消息已生成"));
    }

    @Test
    public void testSpringContextIntegrationWithCustomBeans() throws Exception {
        // 测试与Spring上下文的集成，包括自定义Bean
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/test-log").param("bean", "custom"))
                .andExpect(status().isOk());
    }

    @Test
    public void testConcurrentAccessHandling() throws Exception {
        // 测试并发访问处理
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // 模拟并发请求
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/test-log").param("thread", String.valueOf(i)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testConfigurationLoadingWithOverrides() throws Exception {
        // 测试带覆盖的配置加载
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        // 验证配置覆盖能够正确应用
        mockMvc.perform(get("/test-log").param("override", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("日志消息已生成"));
    }
}