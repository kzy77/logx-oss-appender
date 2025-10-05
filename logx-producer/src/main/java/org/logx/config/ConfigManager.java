package org.logx.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一配置管理器
 * <p>
 * 支持多种配置源的优先级读取，按以下顺序：
 * <ol>
 * <li>JVM系统属性 (-Dkey=value)</li>
 * <li>环境变量 (KEY=value)</li>
 * <li>配置文件属性 (application.properties)</li>
 * <li>代码默认值</li>
 * </ol>
 * <p>
 * 支持配置键的命名规范转换，自动处理不同格式之间的映射。
 *
 * @author OSS Appender Team
 *
 * @since 1.0.0
 */
public class ConfigManager {

    private static final String DEFAULT_CONFIG_FILE = "application.properties";

    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private final Map<String, String> defaultValues = new HashMap<>();
    private Properties fileProperties;
    // 配置文件路径
    private String configFilePath;

    /**
     * 获取配置文件路径
     *
     * @return 配置文件路径
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * 构造配置管理器
     */
    public ConfigManager() {
        try {
            this.configFilePath = DEFAULT_CONFIG_FILE;
            loadFileProperties();
            initializeDefaults();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ConfigManager", e);
        }
    }

    /**
     * 构造配置管理器，指定配置文件路径
     *
     * @param configFilePath
     *            配置文件路径
     */
    public ConfigManager(String configFilePath) {
        try {
            this.configFilePath = configFilePath;
            loadFileProperties(configFilePath);
            initializeDefaults();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ConfigManager with config file: " + configFilePath, e);
        }
    }

    /**
     * 获取配置值，按优先级顺序查找
     *
     * @param key
     *            配置键
     *
     * @return 配置值，如果不存在返回null
     */
    public String getProperty(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        // 检查缓存
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }

        // 优先级1: JVM系统属性
        String value = System.getProperty(key);
        if (value != null) {
            configCache.put(key, value);
            return value;
        }

        // 优先级2: 环境变量 (支持不同命名格式)
        value = getEnvironmentVariable(key);
        if (value != null) {
            configCache.put(key, value);
            return value;
        }

        // 优先级3: 配置文件属性
        if (fileProperties != null) {
            value = fileProperties.getProperty(key);
            if (value != null) {
                configCache.put(key, value);
                return value;
            }
        }

        // 优先级4: 默认值
        value = defaultValues.get(key);
        if (value != null) {
            configCache.put(key, value);
        }

        return value;
    }

    /**
     * 获取配置值，如果不存在返回默认值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 配置值或默认值
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数配置值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 整数配置值
     *
     * @throws NumberFormatException
     *             如果配置值不是有效整数
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for key '" + key + "': " + value, e);
        }
    }

    /**
     * 获取长整数配置值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 长整数配置值
     *
     * @throws NumberFormatException
     *             如果配置值不是有效长整数
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long value for key '" + key + "': " + value, e);
        }
    }

    /**
     * 获取布尔配置值
     *
     * @param key
     *            配置键
     * @param defaultValue
     *            默认值
     *
     * @return 布尔配置值
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        String trimmedValue = value.trim().toLowerCase(java.util.Locale.ENGLISH);
        return "true".equals(trimmedValue) || "yes".equals(trimmedValue) || "1".equals(trimmedValue);
    }

    /**
     * 设置默认值
     *
     * @param key
     *            配置键
     * @param value
     *            默认值
     */
    public void setDefault(String key, String value) {
        if (key != null && value != null) {
            defaultValues.put(key, value);
        }
    }

    /**
     * 清除配置缓存
     */
    public void clearCache() {
        configCache.clear();
    }

    /**
     * 重新加载配置文件
     */
    public void reload() {
        clearCache();
        loadFileProperties(this.configFilePath);
    }

    /**
     * 重新加载指定配置文件
     *
     * @param configFilePath
     *            配置文件路径
     */
    public void reload(String configFilePath) {
        clearCache();
        loadFileProperties(configFilePath);
    }

    /**
     * 获取所有配置的副本
     *
     * @return 配置映射的副本
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> allProperties = new HashMap<>();

        // 添加默认值
        allProperties.putAll(defaultValues);

        // 添加文件属性
        if (fileProperties != null) {
            for (String key : fileProperties.stringPropertyNames()) {
                allProperties.put(key, fileProperties.getProperty(key));
            }
        }

        // 添加环境变量 (只添加已知的配置键)
        for (String key : allProperties.keySet()) {
            String envValue = getEnvironmentVariable(key);
            if (envValue != null) {
                allProperties.put(key, envValue);
            }
        }

        // 添加所有系统属性
        Properties systemProperties = System.getProperties();
        for (String key : systemProperties.stringPropertyNames()) {
            allProperties.put(key, systemProperties.getProperty(key));
        }

        return allProperties;
    }

    /**
     * 从环境变量获取值，支持不同命名格式转换
     *
     * @param key
     *            配置键
     *
     * @return 环境变量值，如果不存在返回null
     */
    private String getEnvironmentVariable(String key) {
        // 直接查找
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }

        // 转换为大写，用下划线替换点号
        String envKey = key.toUpperCase(java.util.Locale.ENGLISH).replace('.', '_');
        value = System.getenv(envKey);
        if (value != null) {
            return value;
        }

        // 转换为全大写
        value = System.getenv(key.toUpperCase(java.util.Locale.ENGLISH));
        if (value != null) {
            return value;
        }

        return null;
    }

    /**
     * 加载默认配置文件
     */
    private void loadFileProperties() {
        loadFileProperties(DEFAULT_CONFIG_FILE);
    }

    /**
     * 加载指定配置文件
     *
     * @param configFilePath
     *            配置文件路径
     */
    private void loadFileProperties(String configFilePath) {
        fileProperties = new Properties();

        // 尝试从类路径加载
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFilePath)) {
            if (input != null) {
                fileProperties.load(input);
                return;
            }
        } catch (IOException e) {
            // 忽略，继续尝试其他方式
        }

        // 尝试从文件系统加载
        try (FileInputStream input = new FileInputStream(configFilePath)) {
            fileProperties.load(input);
        } catch (IOException e) {
            // 配置文件不存在或无法读取，使用空属性
            fileProperties = new Properties();
        }
    }

    /**
     * 初始化默认配置值
     */
    private void initializeDefaults() {
        // LogX OSS统一配置默认值
        setDefault("logx.oss.region", "ap-guangzhou");
        setDefault("logx.oss.keyPrefix", "logs/");
        // pathStyleAccess不设置全局默认值，由各OSS类型自己决定（MinIO=true, S3=false）
        setDefault("logx.oss.connectTimeout", "10000");
        setDefault("logx.oss.readTimeout", "30000");

        // 批处理配置默认值
        setDefault("logx.oss.maxBatchCount", String.valueOf(CommonConfig.Defaults.MAX_BATCH_COUNT));
        setDefault("logx.oss.maxBatchBytes", String.valueOf(CommonConfig.Defaults.MAX_BATCH_BYTES));
        setDefault("logx.oss.maxMessageAgeMs", String.valueOf(CommonConfig.Defaults.MAX_MESSAGE_AGE_MS));

        // 队列配置默认值
        setDefault("logx.oss.queueCapacity", String.valueOf(CommonConfig.Defaults.QUEUE_CAPACITY));
        setDefault("logx.oss.blockWhenFull", "false");

        // 线程池配置默认值
        setDefault("logx.oss.corePoolSize", "2");
        setDefault("logx.oss.maximumPoolSize", "4");
        setDefault("logx.oss.keepAliveTime", "60000");

        // 重试配置默认值
        setDefault("logx.oss.maxRetries", "3");
        setDefault("logx.oss.baseBackoffMs", "1000");
        setDefault("logx.oss.maxBackoffMs", "30000");
    }
}
