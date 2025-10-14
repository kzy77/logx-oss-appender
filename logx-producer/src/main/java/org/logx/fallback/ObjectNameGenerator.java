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
 * 负责生成OSS对象名和本地兜底文件名
 * <p>
 * <b>统一时间格式规则</b>：
 * <ul>
 * <li>年月日：yyyy/MM/dd</li>
 * <li>时间戳：HHmmssSSS（时分秒毫秒，紧凑格式无分隔符）</li>
 * </ul>
 * <p>
 * <b>文件格式（OSS上传、本地兜底、重试上传统一）</b>：
 * <pre>
 * yyyy/MM/dd/HHmmssSSS-applog-IP-uniqueId.log.gz
 * 示例：2025/10/14/143250200-applog-100.119.145.245-abc12345.log.gz
 * </pre>
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public class ObjectNameGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ObjectNameGenerator.class);

    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");

    private static final String APP_IDENTIFIER = "applog";
    private static final String UNKNOWN_HOST = "unknown-host";
    private static final String FILE_SUFFIX = ".log.gz";

    private final String fileName;
    private final String localIP;

    /**
     * 构造对象名生成器
     *
     * @param fileName 文件名前缀（保留参数用于兼容性，实际使用固定标识applog）
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
     * 生成对象名
     * <p>
     * 格式：yyyy/MM/dd/HHmmssSSS-applog-IP-uniqueId.log.gz
     *
     * @return 对象名
     */
    public String generateObjectName() {
        try {
            LocalDateTime nowTime = LocalDateTime.now();
            String year = nowTime.format(YEAR_FORMATTER);
            String month = nowTime.format(MONTH_FORMATTER);
            String day = nowTime.format(DAY_FORMATTER);
            String time = nowTime.format(TIME_FORMATTER);
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);

            return year + "/" + month + "/" + day + "/" + time + "-" + APP_IDENTIFIER + "-" + localIP + "-" + uniqueId + FILE_SUFFIX;
        } catch (Exception e) {
            logger.warn("Failed to generate object name, using fallback naming", e);

            LocalDateTime nowTime = LocalDateTime.now();
            String year = nowTime.format(YEAR_FORMATTER);
            String month = nowTime.format(MONTH_FORMATTER);
            String day = nowTime.format(DAY_FORMATTER);
            String time = nowTime.format(TIME_FORMATTER);
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);

            return year + "/" + month + "/" + day + "/" + time + "-" + APP_IDENTIFIER + "-" + uniqueId + FILE_SUFFIX;
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
}
