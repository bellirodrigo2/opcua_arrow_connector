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
    private final InfraConfig infraConfig;

    public ConfigProvider() {
        this.postgreSQLConfig = loadPostgreSQLConfig();
        this.retryPolicyConfig = loadRetryPolicyConfig();
        this.infraConfig = loadInfraConfig();

        // Validate all configurations
        validateConfigurations();

        logger.info("Configuration loaded successfully");
        logger.debug("PostgreSQL Config: {}", postgreSQLConfig);
        logger.debug("Retry Policy Config: {}", retryPolicyConfig);
        logger.debug("Infra Config: {}", infraConfig);
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

    private InfraConfig loadInfraConfig() {
        try {
            InfraConfig config = InfraConfig.fromEnvironment();
            config.validate();
            return config;
        } catch (Exception e) {
            logger.error("Failed to load Infra configuration", e);
            throw new RuntimeException("Infra configuration error", e);
        }
    }
    
    private void validateConfigurations() {
        try {
            postgreSQLConfig.validate();
            retryPolicyConfig.validate();
            infraConfig.validate();
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

    public InfraConfig getInfraConfig() {
        return infraConfig;
    }
    
    /**
     * Create a configuration for testing with default values
     */
    public static ConfigProvider forTesting() {
        // Create test PostgreSQL config
        PostgreSQLConfig pgConfig = new PostgreSQLConfig();
        pgConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/opcua_arrow_test");
        pgConfig.setUsername("test_user");
        pgConfig.setPassword("test_password");
        pgConfig.setSourceName("test_source");
        pgConfig.setMaxPoolSize(5);
        pgConfig.setMinPoolSize(1);

        // Create test retry config
        RetryPolicyConfig retryConfig = new RetryPolicyConfig();

        // Create test infra config
        InfraConfig infraConfig = new InfraConfig();

        return new ConfigProvider(pgConfig, retryConfig, infraConfig);
    }

    /**
     * Constructor for testing purposes
     */
    private ConfigProvider(PostgreSQLConfig postgreSQLConfig, RetryPolicyConfig retryPolicyConfig, InfraConfig infraConfig) {
        this.postgreSQLConfig = postgreSQLConfig;
        this.retryPolicyConfig = retryPolicyConfig;
        this.infraConfig = infraConfig;
        validateConfigurations();
        logger.info("Test configuration loaded");
    }
}