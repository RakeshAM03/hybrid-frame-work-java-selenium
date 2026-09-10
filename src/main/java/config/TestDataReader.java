package config;

import java.util.Properties;

/**
 * Loads all login test data (negative-case inputs and expected error/validation messages)
 * from testdata.properties, so no test data or expected strings are hardcoded in test/page classes.
 */
public final class TestDataReader {

    private static final String TEST_DATA_FILE = "testdata.properties";
    private static final Properties PROPERTIES = PropertiesLoader.load(TEST_DATA_FILE);

    private TestDataReader() {
    }

    public static String get(String key) {
        return PropertiesLoader.get(PROPERTIES, key);
    }
}
