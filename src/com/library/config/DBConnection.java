package com.library.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Thread-Safe Singleton JDBC Connection Factory.
 * Supports both enterprise MySQL 8.0 Server and Zero-Config Embedded SQLite.
 */
public class DBConnection {
    private static volatile DBConnection instance;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}
    }

    private DBConnection() {}

    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        String dbType = AppConfig.get("db.type", "mysql").toLowerCase();
        if ("sqlite".equals(dbType)) {
            return DriverManager.getConnection("jdbc:sqlite:library.db");
        }

        String url = AppConfig.get("db.url");
        String user = AppConfig.get("db.user");
        String password = AppConfig.get("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean testConnection() {
        try (Connection conn = getInstance().getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void initializeDatabaseIfMissing() throws SQLException, IOException {
        String dbType = AppConfig.get("db.type", "mysql").toLowerCase();

        if ("sqlite".equals(dbType)) {
            File schemaFile = new File("resources/schema_sqlite.sql");
            try (Connection conn = getInstance().getConnection()) {
                if (schemaFile.exists()) {
                    executeSqlScript(conn, schemaFile);
                }
            }
            return;
        }

        // MySQL Path
        String host = AppConfig.get("db.host", "localhost");
        String port = AppConfig.get("db.port", "3306");
        String user = AppConfig.get("db.user", "root");
        String pass = AppConfig.get("db.password", "root");
        String serverUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        // Step 1: Ensure database exists
        try (Connection serverConn = DriverManager.getConnection(serverUrl, user, pass);
             Statement stmt = serverConn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
        }

        // Step 2: Run schema script on library_db
        try (Connection dbConn = getInstance().getConnection()) {
            File schemaFile = new File("resources/schema.sql");
            if (schemaFile.exists()) {
                executeSqlScript(dbConn, schemaFile);
            }
        }
    }

    public static void executeSqlScript(Connection conn, File file) throws IOException, SQLException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             Statement stmt = conn.createStatement()) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("--") || line.startsWith("//") || line.startsWith("/*") || line.isEmpty()) {
                    continue;
                }
                sql.append(line).append(" ");
                if (line.endsWith(";")) {
                    String query = sql.toString().trim();
                    if (query.endsWith(";")) {
                        query = query.substring(0, query.length() - 1);
                    }
                    if (!query.isEmpty() && !query.toUpperCase().startsWith("USE ")) {
                        stmt.execute(query);
                    }
                    sql.setLength(0);
                }
            }
        }
    }
}
