package com.timemanagement.db;

import com.timemanagement.exception.DatabaseOperationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Enterprise Connection Pool Manager utilizing HikariCP.
 * Replaces the legacy shared Singleton connection with an asynchronous,
 * thread-safe connection pool preventing race conditions across threads.
 */
public class DBConnection {

    private static DBConnection instance;
    private final HikariDataSource dataSource;
    private final Properties properties = new Properties();

    private DBConnection() throws DatabaseOperationException {
        this(null);
    }

    private DBConnection(String customUrl) throws DatabaseOperationException {
        loadProperties();
        this.dataSource = initDataSource(customUrl);
    }

    /**
     * Loads database credentials from environment variables, system properties,
     * or db.properties resource file.
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
            // Fall back to defaults
        }

        // Apply fallback defaults if not provided
        if (!properties.containsKey("db.driver")) {
            properties.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        }
        if (!properties.containsKey("db.url")) {
            properties.setProperty("db.url", "jdbc:mysql://localhost:3306/employee_time_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true");
        }
        if (!properties.containsKey("db.user")) {
            properties.setProperty("db.user", "root");
        }
        if (!properties.containsKey("db.password")) {
            properties.setProperty("db.password", "");
        }
    }

    /**
     * Configures and initializes the HikariCP DataSource.
     */
    private HikariDataSource initDataSource(String customUrl) throws DatabaseOperationException {
        try {
            HikariConfig config = new HikariConfig();
            config.setPoolName("EmployeeTimePool");

            // Priority: customUrl > ENV var > System Property > db.properties
            String envUrl = System.getenv("DB_URL");
            String sysUrl = System.getProperty("db.url");
            String finalUrl = customUrl != null ? customUrl : (envUrl != null ? envUrl : (sysUrl != null ? sysUrl : properties.getProperty("db.url")));

            String envUser = System.getenv("DB_USER");
            String sysUser = System.getProperty("db.user");
            String finalUser = envUser != null ? envUser : (sysUser != null ? sysUser : properties.getProperty("db.user"));

            String envPass = System.getenv("DB_PASSWORD");
            String sysPass = System.getProperty("db.password");
            String finalPass = envPass != null ? envPass : (sysPass != null ? sysPass : properties.getProperty("db.password"));

            config.setDriverClassName(properties.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            config.setJdbcUrl(finalUrl);
            config.setUsername(finalUser);
            config.setPassword(finalPass);

            // Connection Pool Sizing & Lifecycle Tuning
            config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("db.pool.maxSize", "10")));
            config.setMinimumIdle(Integer.parseInt(properties.getProperty("db.pool.minIdle", "2")));
            config.setConnectionTimeout(Long.parseLong(properties.getProperty("db.pool.timeout", "30000")));
            config.setIdleTimeout(600000);   // 10 minutes
            config.setMaxLifetime(1800000);  // 30 minutes
            config.setAutoCommit(true);

            // MySQL performance optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            return new HikariDataSource(config);
        } catch (Exception ex) {
            throw new DatabaseOperationException("Failed to initialize HikariCP connection pool: " + ex.getMessage(), ex);
        }
    }

    /**
     * Obtains the thread-safe singleton instance of DBConnection.
     */
    public static synchronized DBConnection getInstance() throws DatabaseOperationException {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Reconfigures the singleton to connect to a specific database (e.g. employee_time_test_db).
     * Used for isolated test suites.
     */
    public static synchronized void reinitializeForDatabase(String jdbcUrl) throws DatabaseOperationException {
        if (instance != null) {
            instance.closePool();
        }
        instance = new DBConnection(jdbcUrl);
    }

    /**
     * Borrows an active JDBC Connection from the HikariCP pool.
     * Callers MUST close the returned Connection using try-with-resources.
     */
    public Connection getConnection() throws DatabaseOperationException {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Failed to lease database connection from pool: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gracefully shuts down the HikariCP connection pool.
     */
    public synchronized void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Legacy backward-compatibility method.
     */
    public synchronized void closeConnection() {
        // In HikariCP, connection lifecycle is handled per leased connection via try-with-resources
    }
}
