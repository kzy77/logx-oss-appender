package org.logx.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

/**
 * OSS相关配置的Lookup插件 支持以下查找键： - oss:endpoint - 获取推荐的OSS endpoint - oss:region - 获取推荐的region - oss:keyPrefix - 获取默认的key前缀
 * 使用示例： ${oss:endpoint} - 解析为推荐的OSS endpoint ${oss:keyPrefix} - 解析为默认的key前缀
 */
@Plugin(name = "oss", category = StrLookup.CATEGORY)
public class OSSLookupPlugin implements StrLookup {

    @Override
    public String lookup(String key) {
        if (key == null) {
            return null;
        }

        switch (key.toLowerCase()) {
            case "endpoint":
                // 返回推荐的OSS endpoint（可以从环境变量或系统属性获取）
                return System.getProperty("oss.default.endpoint", System.getenv("OSS_DEFAULT_ENDPOINT"));

            case "region":
                // 返回推荐的region
                return System.getProperty("oss.default.region", System.getenv("OSS_DEFAULT_REGION"));

            case "keyprefix":
            case "key-prefix":
                // 返回默认的key前缀
                String appName = System.getProperty("app.name", "application");
                String env = System.getProperty("app.env", "dev");
                return String.format("logs/%s/%s/", appName, env);

            case "bucket":
                // 返回默认的bucket名称
                return System.getProperty("oss.default.bucket", System.getenv("OSS_DEFAULT_BUCKET"));

            case "access-key-id":
            case "accesskeyid":
                // 返回Access Key ID（从环境变量获取，更安全）
                return System.getenv("OSS_ACCESS_KEY_ID");

            case "access-key-secret":
            case "accesskeysecret":
                // 返回Access Key Secret（从环境变量获取，更安全）
                return System.getenv("OSS_ACCESS_KEY_SECRET");

            case "max-queue-size":
            case "maxqueuesize":
                // 根据系统内存推荐队列大小
                return getRecommendedQueueSize();

            case "flush-interval":
            case "flushinterval":
                // 根据环境推荐刷新间隔
                String env2 = System.getProperty("app.env", "dev");
                return "prod".equals(env2) ? "5000" : "2000";

            default:
                return null;
        }
    }

    @Override
    public String lookup(LogEvent event, String key) {
        // 对于事件相关的查找，可以添加更多上下文信息
        String baseResult = lookup(key);

        if (baseResult != null && "keyprefix".equals(key.toLowerCase()) && event != null) {
            // 可以根据日志事件的logger名称调整key前缀
            String loggerName = event.getLoggerName();
            if (loggerName != null && loggerName.contains(".")) {
                String[] parts = loggerName.split("\\.");
                String packageName = parts[0];
                return baseResult + packageName + "/";
            }
        }

        return baseResult;
    }

    /**
     * 根据系统内存推荐队列大小
     */
    private String getRecommendedQueueSize() {
        long maxMemory = Runtime.getRuntime().maxMemory();

        if (maxMemory > 4L * 1024 * 1024 * 1024) { // > 4GB
            return "131072"; // 128K
        } else if (maxMemory > 2L * 1024 * 1024 * 1024) { // > 2GB
            return "65536"; // 64K
        } else if (maxMemory > 1L * 1024 * 1024 * 1024) { // > 1GB
            return "32768"; // 32K
        } else {
            return "16384"; // 16K
        }
    }
}
