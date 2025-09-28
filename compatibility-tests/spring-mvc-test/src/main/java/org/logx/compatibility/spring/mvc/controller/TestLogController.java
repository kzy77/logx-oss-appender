package org.logx.compatibility.spring.mvc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 测试控制器，用于生成各种级别的日志消息
 */
@Controller
public class TestLogController {

    private static final Logger logger = LoggerFactory.getLogger(TestLogController.class);

    @GetMapping(value = "/test-log", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String testLog() {
        // 生成不同级别的日志消息
        logger.trace("这是一条TRACE级别的日志消息");
        logger.debug("这是一条DEBUG级别的日志消息");
        logger.info("这是一条INFO级别的日志消息");
        logger.warn("这是一条WARN级别的日志消息");
        logger.error("这是一条ERROR级别的日志消息");

        return "日志消息已生成";
    }

    @GetMapping(value = "/test-exception", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String testException() {
        try {
            // 模拟一个异常
            throw new RuntimeException("测试异常");
        } catch (Exception e) {
            logger.error("捕获到异常", e);
        }

        return "异常日志已生成";
    }
}