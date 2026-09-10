package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Shared loader used by {@link ConfigReader} and {@link TestDataReader}.
 * Every property lookup can be overridden with a JVM -D system property of the same key.
 */
final class PropertiesLoader {

    private PropertiesLoader() {
    }

    static Properties load(String fileName) {
        Properties properties = new Properties();
        try (InputStream inputStream = PropertiesLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Unable to find '" + fileName + "' on the classpath");
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load '" + fileName + "'", e);
        }
        return properties;
    }

    static String get(Properties properties, String key) {
        String systemOverride = System.getProperty(key);
        if (systemOverride != null && !systemOverride.isEmpty()) {
            return systemOverride;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing property key: '" + key + "'");
        }
        return value;
    }
}
