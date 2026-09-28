package com.mentorconnect.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Robust DataSource Configuration with Automatic Fallback.
 * Ensures MySQL JDBC connectivity is used when available, but if local MySQL
 * is offline or unreachable, seamlessly falls back to embedded H2 (MySQL mode)
 * so the application starts without crashing.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String configuredUrl;

    @Value("${spring.datasource.username:root}")
    private String configuredUsername;

    @Value("${spring.datasource.password:root}")
    private String configuredPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String configuredDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        // If an in-memory database URL is configured (e.g. during unit tests), use it directly
        if (configuredUrl != null && (configuredUrl.contains(":h2:") || configuredUrl.contains("h2:mem"))) {
            log.info("Using configured in-memory DataSource: {}", configuredUrl);
            return createH2DataSource(configuredUrl, configuredUsername, configuredPassword);
        }

        // Test connectivity to configured MySQL database
        log.info("Testing connectivity to primary MySQL database at: {}", configuredUrl);
        boolean reachable = testConnection(configuredDriver, configuredUrl, configuredUsername, configuredPassword, 3);

        if (reachable) {
            log.info("MySQL connectivity verified successfully! Initializing MySQL Hikari connection pool.");
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(configuredDriver);
            config.setJdbcUrl(configuredUrl);
            config.setUsername(configuredUsername);
            config.setPassword(configuredPassword);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setPoolName("MentorConnect-MySQL-HikariPool");
            return new HikariDataSource(config);
        } else {
            log.warn("==========================================================================================");
            log.warn("WARNING: MySQL database at '{}' is OFFLINE, unreachable, or credentials rejected.", configuredUrl);
            log.warn("ACTIVATING AUTOMATIC FALLBACK: Starting embedded H2 database in MySQL compatibility mode.");
            log.warn("The MentorConnect application will run normally with full functionality and sample data seeding.");
            log.warn("To use MySQL, start your MySQL server on localhost:3306 and restart this application.");
            log.warn("==========================================================================================");

            String fallbackUrl = "jdbc:h2:mem:mentorconnect_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL";
            return createH2DataSource(fallbackUrl, "sa", "");
        }
    }

    private DataSource createH2DataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(url);
        config.setUsername(username != null ? username : "sa");
        config.setPassword(password != null ? password : "");
        config.setMaximumPoolSize(10);
        config.setPoolName("MentorConnect-H2Fallback-HikariPool");
        return new HikariDataSource(config);
    }

    private boolean testConnection(String driver, String url, String user, String pass, int timeoutSec) {
        try {
            Class.forName(driver);
            DriverManager.setLoginTimeout(timeoutSec);
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                return conn != null && !conn.isClosed();
            }
        } catch (Exception e) {
            log.warn("Connection test failed for {}: {}", url, e.getMessage());
            return false;
        }
    }
}
