package ua.kiev.splash.mathhelper.db;

import ua.kiev.splash.mathhelper.exceptions.UnexpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final String DB_PROPERTIES_FILE = "db.properties";
    private static final String DB_PROPERTY_URL = "db.url";
    private static final String DB_PROPERTY_USERNAME = "db.username";
    private static final String DB_PROPERTY_PASSWORD = "db.password";

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(DB_PROPERTIES_FILE)) { // DatabaseConfig.class.getResourceAsStream()
            if (input == null) {
                throw new UnexpectedException("Unable to find database configuration file " + DB_PROPERTIES_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new UnexpectedException("Error occurred while loading database configuration", e);
        }
    }

    public static String getDbUrl() {
        return properties.getProperty(DB_PROPERTY_URL);
    }

    public static String getDbUsername() {
        return properties.getProperty(DB_PROPERTY_USERNAME);
    }

    public static String getDbPassword() {
        return properties.getProperty(DB_PROPERTY_PASSWORD);
    }
}
