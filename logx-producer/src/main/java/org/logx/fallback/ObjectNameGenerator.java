package org.logx.fallback;

import org.logx.util.IPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 对象名生成器
 * <p>
 * 负责生成与OSS保持一致的文件对象名
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class ObjectNameGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(ObjectNameGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
    private static final String UNKNOWN_HOST = "unknown-host";
    private static final String DEFAULT_SUFFIX = ".log.gz";
    private static final String FALLBACK_SUFFIX = "_fallback.log.gz";
    private static final String RETRIED_SUFFIX = "_retried.log.gz";
    
    private final String fileName;
    private final String localIP;
    
    /**
     * 构造对象名生成器
     * 
     * @param fileName 文件名前缀
     * @throws IllegalArgumentException 如果文件名为空
     */
    public ObjectNameGenerator(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        this.fileName = fileName.trim();
        this.localIP = getSafeLocalIP();
    }
    
    /**
     * 生成标准对象名
     * 
     * @return 标准对象名
     */
    public String generateNormalObjectName() {
        try {
            LocalDateTime nowTime = LocalDateTime.now();
            String datePath = nowTime.format(DATE_FORMATTER) + "/" + nowTime.getDayOfMonth();
            String timePath = nowTime.format(TIME_FORMATTER);
            
            return fileName + "_" + datePath + "/" + timePath + "_" + localIP + DEFAULT_SUFFIX;
        } catch (Exception e) {
            logger.warn("Failed to generate normal object name, using fallback naming", e);
            return generateFallbackObjectNameInternal("_normal" + DEFAULT_SUFFIX);
        }
    }
    
    /**
     * 生成兜底文件对象名
     * 
     * @return 兜底文件对象名
     */
    public String generateFallbackObjectName() {
        try {
            LocalDateTime nowTime = LocalDateTime.now();
            String datePath = nowTime.format(DATE_FORMATTER) + "/" + nowTime.getDayOfMonth();
            String timePath = nowTime.format(TIME_FORMATTER);
            
            return fileName + "_" + datePath + "/" + timePath + "_" + localIP + FALLBACK_SUFFIX;
        } catch (Exception e) {
            logger.warn("Failed to generate fallback object name, using fallback naming", e);
            return generateFallbackObjectNameInternal(FALLBACK_SUFFIX);
        }
    }
    
    /**
     * 生成重传对象名（基于原始对象名）
     * 
     * @param originalObjectName 原始对象名
     * @return 重传对象名
     */
    public String generateRetryObjectName(String originalObjectName) {
        if (originalObjectName == null) {
            logger.warn("Original object name is null, generating unique retry name");
            return generateFallbackObjectNameInternal("_retried" + DEFAULT_SUFFIX);
        }
        
        String trimmedName = originalObjectName.trim();
        if (trimmedName.isEmpty()) {
            logger.warn("Original object name is empty, generating unique retry name");
            return generateFallbackObjectNameInternal("_retried" + DEFAULT_SUFFIX);
        }
        
        try {
            if (trimmedName.endsWith(FALLBACK_SUFFIX)) {
                return trimmedName.substring(0, trimmedName.length() - FALLBACK_SUFFIX.length()) + RETRIED_SUFFIX;
            } else if (trimmedName.endsWith(DEFAULT_SUFFIX)) {
                return trimmedName.substring(0, trimmedName.length() - DEFAULT_SUFFIX.length()) + RETRIED_SUFFIX;
            }
            return trimmedName + RETRIED_SUFFIX;
        } catch (Exception e) {
            logger.warn("Failed to generate retry object name, using fallback naming", e);
            return generateFallbackObjectNameInternal("_retried_" + System.currentTimeMillis() + DEFAULT_SUFFIX);
        }
    }
    
    /**
     * 安全获取本地IP地址
     * 
     * @return 本地IP地址或默认值
     */
    private String getSafeLocalIP() {
        try {
            String ip = IPUtil.getLocalIP();
            if (ip == null || ip.trim().isEmpty()) {
                logger.warn("IPUtil returned null or empty IP, using default");
                return UNKNOWN_HOST;
            }
            return ip.trim();
        } catch (Exception e) {
            logger.warn("Failed to get local IP, using default", e);
            return UNKNOWN_HOST + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    /**
     * 生成兜底对象名（内部使用）
     * 
     * @param suffix 后缀
     * @return 兜底对象名
     */
    private String generateFallbackObjectNameInternal(String suffix) {
        LocalDateTime nowTime = LocalDateTime.now();
        String datePath = nowTime.format(DATE_FORMATTER) + "/" + nowTime.getDayOfMonth();
        String timePath = nowTime.format(TIME_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return fileName + "_" + datePath + "/" + timePath + "_" + uniqueId + suffix;
    }
}