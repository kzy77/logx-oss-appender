package org.logx.compatibility.spring.mvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC配置类
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "org.logx.compatibility.spring.mvc")
public class WebConfig implements WebMvcConfigurer {
    
}