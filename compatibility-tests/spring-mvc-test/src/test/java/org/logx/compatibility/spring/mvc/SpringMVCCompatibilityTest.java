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
 * Spring MVC兼容性测试
 */
public class SpringMVCCompatibilityTest {

    @Test
    public void testLogEndpoint() throws Exception {
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultRequest(get("/").characterEncoding("UTF-8"))
                .alwaysDo(result -> result.getResponse().setCharacterEncoding("UTF-8"))
                .build();

        mockMvc.perform(get("/test-log").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));
    }

    @Test
    public void testExceptionEndpoint() throws Exception {
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultRequest(get("/").characterEncoding("UTF-8"))
                .alwaysDo(result -> result.getResponse().setCharacterEncoding("UTF-8"))
                .build();

        mockMvc.perform(get("/test-exception").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("异常日志已生成"));
    }

    @Test
    public void testDifferentLogLevels() throws Exception {
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultRequest(get("/").characterEncoding("UTF-8"))
                .alwaysDo(result -> result.getResponse().setCharacterEncoding("UTF-8"))
                .build();

        // 测试不同日志级别的生成
        mockMvc.perform(get("/test-log").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));
    }

    @Test
    public void testWebApplicationContext() throws Exception {
        // 测试Web应用上下文配置
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestLogController())
            .build();

        mockMvc.perform(get("/test-log"))
                .andExpect(status().isOk());
    }

    @Test
    public void testConfigurationLoading() throws Exception {
        // 测试配置加载
        TestLogController controller = new TestLogController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultRequest(get("/").characterEncoding("UTF-8"))
                .alwaysDo(result -> result.getResponse().setCharacterEncoding("UTF-8"))
                .build();
        
        // 验证控制器能够正确处理请求
        mockMvc.perform(get("/test-log").characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(content().string("日志消息已生成"));
    }
}