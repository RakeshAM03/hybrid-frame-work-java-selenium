package config;

import java.util.Properties;

/**
 * Loads framework configuration (baseUrl, browser, timeouts, valid credentials) from
 * config.properties. Any key can be overridden at runtime with -Dkey=value, e.g.
 * mvn test -Dbrowser=firefox -DexplicitWaitSeconds=20
 */
public final class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = PropertiesLoader.load(CONFIG_FILE);

    private ConfigReader() {
    }

    public static String get(String key) {
        return PropertiesLoader.get(PROPERTIES, key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static String getBaseUrl() {
        return get("baseUrl");
    }

    public static String getBrowser() {
        return get("browser");
    }

    public static boolean isHeadless() {
        return getBoolean("headless");
    }

    public static int getExplicitWaitSeconds() {
        return getInt("explicitWaitSeconds");
    }

    public static int getPageLoadTimeoutSeconds() {
        return getInt("pageLoadTimeoutSeconds");
    }

    public static String getValidEmail() {
        return get("valid.email");
    }

    public static String getValidPassword() {
        return get("valid.password");
    }
}
