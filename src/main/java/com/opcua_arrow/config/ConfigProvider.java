package com.opcua_arrow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central configuration provider for the application
 */
public class ConfigProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigProvider.class);
    
    private final PostgreSQLConfig postgreSQLConfig;
    private final RetryPolicyConfig retryPolicyConfig;
    
    public ConfigProvider() {
        this.postgreSQLConfig = loadPostgreSQLConfig();
        this.retryPolicyConfig = loadRetryPolicyConfig();
        
        // Validate all configurations
        validateConfigurations();
        
        logger.info("Configuration loaded successfully");
        logger.debug("PostgreSQL Config: {}", postgreSQLConfig);
        logger.debug("Retry Policy Config: {}", retryPolicyConfig);
    }
    
    private PostgreSQLConfig loadPostgreSQLConfig() {
        try {
            PostgreSQLConfig config = PostgreSQLConfig.fromEnvironment();
            config.validate();
            return config;
        } catch (Exception e) {
            logger.error("Failed to load PostgreSQL configuration", e);
            throw new RuntimeException("PostgreSQL configuration error", e);
        }
    }
    
    private RetryPolicyConfig loadRetryPolicyConfig() {
        try {
            RetryPolicyConfig config = RetryPolicyConfig.fromEnvironment();
            config.validate();
            return config;
        } catch (Exception e) {
            logger.error("Failed to load Retry Policy configuration", e);
            throw new RuntimeException("Retry Policy configuration error", e);
        }
    }
    
    private void validateConfigurations() {
        try {
            postgreSQLConfig.validate();
            retryPolicyConfig.validate();
        } catch (Exception e) {
            logger.error("Configuration validation failed", e);
            throw new RuntimeException("Invalid configuration", e);
        }
    }
    
    public PostgreSQLConfig getPostgreSQLConfig() {
        return postgreSQLConfig;
    }
    
    public RetryPolicyConfig getRetryPolicyConfig() {
        return retryPolicyConfig;
    }
    
    /**
     * Create a configuration for testing with default values
     */
    public static ConfigProvider forTesting() {
        // Set test environment variables
        System.setProperty("DB_JDBC_URL", "jdbc:postgresql://localhost:5432/opcua_arrow_test");
        System.setProperty("DB_USERNAME", "test_user");
        System.setProperty("DB_PASSWORD", "test_password");
        System.setProperty("SOURCE_NAME", "test_source");
        
        return new ConfigProvider();
    }
}