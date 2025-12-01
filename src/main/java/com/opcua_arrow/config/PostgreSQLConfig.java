package com.opcua_arrow.config;

import java.time.Duration;

/**
 * Configuration for PostgreSQL database connection and settings
 */
public class PostgreSQLConfig {
    private String jdbcUrl = "jdbc:postgresql://localhost:5432/opcua_arrow";
    private String username = "postgres";
    private String password = "password";
    private String driverClassName = "org.postgresql.Driver";

    // Connection pool settings
    private int maxPoolSize = 10;
    private int minPoolSize = 2;
    private Duration connectionTimeout = Duration.ofSeconds(30);
    private Duration idleTimeout = Duration.ofMinutes(10);
    private Duration maxLifetime = Duration.ofMinutes(30);

    // Query settings
    private Duration queryTimeout = Duration.ofSeconds(30);
    private boolean autoCommit = true;
    private String transactionIsolation = "READ_COMMITTED";

    // Getters and Setters

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public PostgreSQLConfig setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public PostgreSQLConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public PostgreSQLConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public PostgreSQLConfig setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public PostgreSQLConfig setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public PostgreSQLConfig setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
        return this;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public PostgreSQLConfig setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public PostgreSQLConfig setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    public Duration getMaxLifetime() {
        return maxLifetime;
    }

    public PostgreSQLConfig setMaxLifetime(Duration maxLifetime) {
        this.maxLifetime = maxLifetime;
        return this;
    }

    public Duration getQueryTimeout() {
        return queryTimeout;
    }

    public PostgreSQLConfig setQueryTimeout(Duration queryTimeout) {
        this.queryTimeout = queryTimeout;
        return this;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public PostgreSQLConfig setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        return this;
    }

    public String getTransactionIsolation() {
        return transactionIsolation;
    }

    public PostgreSQLConfig setTransactionIsolation(String transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
        return this;
    }

    /**
     * Create configuration from environment variables
     */
    public static PostgreSQLConfig fromEnvironment() {
        PostgreSQLConfig config = new PostgreSQLConfig();

        // Database connection
        String jdbcUrl = System.getenv("DB_JDBC_URL");
        if (jdbcUrl != null)
            config.setJdbcUrl(jdbcUrl);

        String username = System.getenv("DB_USERNAME");
        if (username != null)
            config.setUsername(username);

        String password = System.getenv("DB_PASSWORD");
        if (password != null)
            config.setPassword(password);

        // Connection pool
        String maxPoolSize = System.getenv("DB_MAX_POOL_SIZE");
        if (maxPoolSize != null)
            config.setMaxPoolSize(Integer.parseInt(maxPoolSize));

        String minPoolSize = System.getenv("DB_MIN_POOL_SIZE");
        if (minPoolSize != null)
            config.setMinPoolSize(Integer.parseInt(minPoolSize));

        String connectionTimeout = System.getenv("DB_CONNECTION_TIMEOUT_SECONDS");
        if (connectionTimeout != null) {
            config.setConnectionTimeout(Duration.ofSeconds(Long.parseLong(connectionTimeout)));
        }

        // Query settings
        String queryTimeout = System.getenv("DB_QUERY_TIMEOUT_SECONDS");
        if (queryTimeout != null) {
            config.setQueryTimeout(Duration.ofSeconds(Long.parseLong(queryTimeout)));
        }

        String autoCommit = System.getenv("DB_AUTO_COMMIT");
        if (autoCommit != null)
            config.setAutoCommit(Boolean.parseBoolean(autoCommit));

        String transactionIsolation = System.getenv("DB_TRANSACTION_ISOLATION");
        if (transactionIsolation != null)
            config.setTransactionIsolation(transactionIsolation);

        return config;
    }

    /**
     * Validate configuration
     */
    public void validate() {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL is required");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password is required (can be empty)");
        }
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("Max pool size must be positive");
        }
        if (minPoolSize < 0 || minPoolSize > maxPoolSize) {
            throw new IllegalArgumentException("Min pool size must be between 0 and max pool size");
        }
    }

    @Override
    public String toString() {
        return "PostgreSQLConfig{" +
                "jdbcUrl='" + jdbcUrl + '\'' +
                ", username='" + username + '\'' +
                ", password='***'" +
                ", maxPoolSize=" + maxPoolSize +
                ", minPoolSize=" + minPoolSize +
                ", connectionTimeout=" + connectionTimeout +
                ", queryTimeout=" + queryTimeout +
                '}';
    }
}
