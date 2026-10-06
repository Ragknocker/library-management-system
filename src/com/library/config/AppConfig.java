package com.library.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages runtime and persistent application configurations.
 */
public class AppConfig {
    private static final String CONFIG_FILE = "resources/db.properties";
    private static final Properties props = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Warning: Could not read " + CONFIG_FILE + ", using defaults. " + e.getMessage());
            }
        }
        // Set safe defaults if missing
        props.putIfAbsent("db.host", "localhost");
        props.putIfAbsent("db.port", "3306");
        props.putIfAbsent("db.name", "library_db");
        props.putIfAbsent("db.user", "root");
        props.putIfAbsent("db.password", "root");
        props.putIfAbsent("db.url", "jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        props.putIfAbsent("policy.fine.per.day", "1.00");
        props.putIfAbsent("policy.student.max.books", "4");
        props.putIfAbsent("policy.student.loan.days", "14");
        props.putIfAbsent("policy.faculty.max.books", "10");
        props.putIfAbsent("policy.faculty.loan.days", "30");
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void set(String key, String value) {
        props.setProperty(key, value);
    }

    public static void saveConfig() throws IOException {
        File file = new File(CONFIG_FILE);
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Library Management System Runtime Properties");
        }
    }
}
