package org.logx.config;

/**
 * Appender配置解析工具类
 * 提供统一的配置解析方法，应用完整的配置优先级链：
 * JVM系统属性 > 环境变量 > 配置文件 > XML字段值 > 默认值
 *
 * @author OSS Appender Team
 * @since 1.0.0
 */
public final class AppenderConfigResolver {

    private static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    private AppenderConfigResolver() {
    }

    /**
     * 解析字符串配置，应用完整的优先级链
     *
     * 优先级顺序：JVM系统属性 > 环境变量 > 配置文件 > XML明确配置的值 > ConfigManager默认值
     *
     * 支持所有配置源中的${ENV:-default}占位符语法，确保占位符在任何层级都能被正确解析
     *
     * @param configKey 配置键（logx.oss.xxx格式）
     * @param xmlValue XML配置中设置的字段值（可能包含${ENV:-default}占位符）
     * @return 最终配置值
     */
    public static String resolveStringConfig(String configKey, String xmlValue) {
        String value = CONFIG_MANAGER.getPropertyWithoutDefaults(configKey);
        if (value != null && !value.trim().isEmpty()) {
            String resolvedValue = CONFIG_MANAGER.resolvePlaceholders(value);
            if (resolvedValue != null && !resolvedValue.trim().isEmpty()) {
                return resolvedValue;
            }
        }

        if (xmlValue != null && !xmlValue.trim().isEmpty()) {
            String resolvedXmlValue = CONFIG_MANAGER.resolvePlaceholders(xmlValue);
            if (resolvedXmlValue != null && !resolvedXmlValue.trim().isEmpty()) {
                return resolvedXmlValue;
            }
        }

        return CONFIG_MANAGER.getProperty(configKey);
    }

    /**
     * 解析整数配置，应用完整的优先级链
     *
     * @param configKey 配置键
     * @param xmlValue XML配置值
     * @return 最终配置值
     */
    public static int resolveIntConfig(String configKey, int xmlValue) {
        String value = CONFIG_MANAGER.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return xmlValue;
            }
        }
        return xmlValue;
    }

    /**
     * 解析长整数配置，应用完整的优先级链
     *
     * @param configKey 配置键
     * @param xmlValue XML配置值
     * @return 最终配置值
     */
    public static long resolveLongConfig(String configKey, long xmlValue) {
        String value = CONFIG_MANAGER.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return xmlValue;
            }
        }
        return xmlValue;
    }

    /**
     * 解析布尔配置，应用完整的优先级链
     *
     * @param configKey 配置键
     * @param xmlValue XML配置值
     * @return 最终配置值
     */
    public static boolean resolveBooleanConfig(String configKey, boolean xmlValue) {
        String value = CONFIG_MANAGER.getProperty(configKey);
        if (value != null && !value.trim().isEmpty()) {
            String trimmedValue = value.trim().toLowerCase(java.util.Locale.ENGLISH);
            return "true".equals(trimmedValue) || "yes".equals(trimmedValue) || "1".equals(trimmedValue);
        }
        return xmlValue;
    }
}
