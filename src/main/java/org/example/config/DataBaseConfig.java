package org.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DataBaseConfig {

    private static final HikariDataSource dataSource;

    static {
        HikariDataSource ds;
        try {
            ds = initializeDataSource();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize HikariCP pool: " + e.getMessage());
        }
        dataSource = ds;
    }

    private static HikariDataSource initializeDataSource() {
        try (InputStream input = DataBaseConfig.class.getClassLoader().getResourceAsStream("database.yml")) {
            Properties properties = new Properties();
            if (input == null) {
                throw new RuntimeException("Unable to find database.yml file");
            }
            properties.load(input);

            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(properties.getProperty("jdbcUrl"));
            config.setUsername(properties.getProperty("username"));
            config.setPassword(properties.getProperty("password"));
            config.setDriverClassName(properties.getProperty("driver"));

            config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("maxPoolSize", "10")));
            config.setMinimumIdle(Integer.parseInt(properties.getProperty("minIdle", "2")));
            config.setConnectionTimeout(Long.parseLong(properties.getProperty("connectionTimeout", "30000")));
            config.setIdleTimeout(Long.parseLong(properties.getProperty("idleTimeout", "600000")));
            config.setMaxLifetime(Long.parseLong(properties.getProperty("maxLifetime", "1800000")));

            return new HikariDataSource(config);
        } catch (IOException e) {
            throw new RuntimeException("Error loading database properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
