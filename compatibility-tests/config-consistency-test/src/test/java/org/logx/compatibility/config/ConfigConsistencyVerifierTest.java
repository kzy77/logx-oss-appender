package org.logx.compatibility.config;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * 配置一致性验证工具测试
 */
public class ConfigConsistencyVerifierTest {

    @Test
    public void testConfigConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建模拟配置
        Map<String, String> logbackConfig = createMockConfig();
        Map<String, String> log4j2Config = createMockConfig();
        Map<String, String> log4j1Config = createMockConfig();
        
        // 验证配置参数一致性
        ConfigConsistencyReport report = verifier.verifyParameterConsistency(
                logbackConfig, log4j2Config, log4j1Config);
        
        // 验证报告一致性
        assertTrue("配置应该是一致的", report.isConsistent());
    }

    @Test
    public void testEnvironmentVariableConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建模拟环境变量
        Map<String, String> envVars = createMockEnvironmentVariables();
        
        // 验证环境变量一致性
        ConfigConsistencyReport report = verifier.verifyEnvironmentVariableConsistency(envVars);
        
        // 验证报告一致性
        assertTrue("环境变量应该是一致的", report.isConsistent());
    }

    @Test
    public void testValidationMechanismConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 验证配置验证机制一致性
        ConfigConsistencyReport report = verifier.verifyValidationMechanismConsistency();
        
        // 验证报告一致性
        assertTrue("配置验证机制应该是一致的", report.isConsistent());
    }

    @Test
    public void testErrorHandlingConsistencyVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 验证错误处理一致性
        ConfigConsistencyReport report = verifier.verifyErrorHandlingConsistency();
        
        // 验证报告一致性
        assertTrue("错误处理应该是一致的", report.isConsistent());
    }

    @Test
    public void testConfigInconsistencyDetection() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建不一致的配置
        Map<String, String> logbackConfig = createMockConfig();
        Map<String, String> log4j2Config = createMockConfig();
        Map<String, String> log4j1Config = createInconsistentMockConfig();
        
        // 验证配置参数一致性
        ConfigConsistencyReport report = verifier.verifyParameterConsistency(
                logbackConfig, log4j2Config, log4j1Config);
        
        // 验证报告不一致性 (Log4j1缺少8个参数)
        assertFalse("配置应该是不一致的", report.isConsistent());
        assertEquals("应该检测到不一致的参数", 8, report.getInconsistentParameters().size());
    }

    @Test
    public void testEnvironmentVariableInconsistencyDetection() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建不一致的环境变量
        Map<String, String> envVars = createInconsistentEnvironmentVariables();
        
        // 验证环境变量一致性
        ConfigConsistencyReport report = verifier.verifyEnvironmentVariableConsistency(envVars);
        
        // 验证报告不一致性 (缺少5个环境变量)
        assertFalse("环境变量应该是不一致的", report.isConsistent());
        assertEquals("应该检测到不一致的环境变量", 5, report.getInconsistentParameters().size());
    }

    @Test
    public void testReportContentVerification() {
        ConfigConsistencyVerifier verifier = new ConfigConsistencyVerifier();
        
        // 创建模拟配置
        Map<String, String> logbackConfig = createMockConfig();
        Map<String, String> log4j2Config = createMockConfig();
        Map<String, String> log4j1Config = createMockConfig();
        
        // 验证配置参数一致性
        ConfigConsistencyReport report = verifier.verifyParameterConsistency(
                logbackConfig, log4j2Config, log4j1Config);
        
        // 验证报告内容 (11个参数 × 3个框架 = 33个一致的参数)
        assertEquals("应该有33个一致的参数", 33, report.getConsistentParameters().size());
        assertEquals("应该没有不一致的参数", 0, report.getInconsistentParameters().size());
    }

    private Map<String, String> createMockConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("logx.oss.bucket", "test-bucket");
        config.put("logx.oss.keyPrefix", "logs/");
        config.put("logx.oss.region", "us-east-1");
        config.put("logx.oss.accessKeyId", "test-access-key");
        config.put("logx.oss.accessKeySecret", "test-secret-key");
        config.put("logx.oss.endpoint", "https://s3.amazonaws.com");
        config.put("logx.oss.pathStyleAccess", "false");
        config.put("logx.oss.enableSsl", "true");
        config.put("logx.oss.maxConnections", "50");
        config.put("logx.oss.connectTimeout", "30000");
        config.put("logx.oss.readTimeout", "60000");
        return config;
    }

    private Map<String, String> createInconsistentMockConfig() {
        Map<String, String> config = new HashMap<>();
        // 缺少一些关键配置参数
        config.put("logx.oss.bucket", "test-bucket");
        config.put("logx.oss.keyPrefix", "logs/");
        // 缺少 logx.oss.region
        config.put("logx.oss.accessKeyId", "test-access-key");
        // 缺少 logx.oss.accessKeySecret
        // 缺少其他参数
        return config;
    }

    private Map<String, String> createMockEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("LOGX_OSS_ENDPOINT", "https://s3.amazonaws.com");
        envVars.put("LOGX_OSS_REGION", "us-east-1");
        envVars.put("LOGX_OSS_ACCESS_KEY_ID", "test-access-key");
        envVars.put("LOGX_OSS_ACCESS_KEY_SECRET", "test-secret-key");
        envVars.put("LOGX_OSS_BUCKET", "test-bucket");
        envVars.put("LOGX_OSS_KEY_PREFIX", "logs/");
        envVars.put("LOGX_OSS_TYPE", "SF_OSS");
        envVars.put("LOGX_OSS_MAX_UPLOAD_SIZE_MB", "20");
        return envVars;
    }

    private Map<String, String> createInconsistentEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("LOGX_OSS_BUCKET", "test-bucket");
        envVars.put("LOGX_OSS_KEY_PREFIX", "logs/");
        // 缺少 LOGX_OSS_REGION
        envVars.put("LOGX_OSS_ACCESS_KEY_ID", "test-access-key");
        // 缺少 LOGX_OSS_ACCESS_KEY_SECRET
        // 缺少其他参数
        return envVars;
    }
}