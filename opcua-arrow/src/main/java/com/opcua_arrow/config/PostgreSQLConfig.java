package com.opcua_arrow.config;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for PostgreSQL database connection and settings
 */
public class PostgreSQLConfig {
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName;

    // Connection pool settings
    private int maxPoolSize;
    private int minPoolSize;
    private Duration connectionTimeout;
    private Duration idleTimeout;
    private Duration maxLifetime;
    // Query settings
    private Duration queryTimeout;
    private boolean autoCommit;
    private String transactionIsolation;

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

    public static PostgreSQLConfig fromMap(Map<String, String> configMap) {
        return new PostgreSQLConfig();
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
