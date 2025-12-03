package com.opcua_arrow.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central configuration provider for the application
 */
public class ConfigProvider {

    private static final Logger logger = LoggerFactory.getLogger(ConfigProvider.class);

    private final PostgreSQLConfig postgreSQLConfig;
    private final RetryPolicyConfig retryPolicyConfig;
    private final AppConfig appConfig;
    private final OPCUAClientConfig opcuaClientConfig;

    public ConfigProvider() {
        this.appConfig = loadAppConfig();
        this.postgreSQLConfig = loadPostgreSQLConfig();
        this.retryPolicyConfig = loadRetryPolicyConfig();
        this.opcuaClientConfig = loadOPCUAClientConfig();

        // Validate all configurations
        validateConfigurations();

        logger.info("Configuration loaded successfully");
        logger.debug("PostgreSQL Config: {}", postgreSQLConfig);
        logger.debug("Retry Policy Config: {}", retryPolicyConfig);
        logger.debug("Infra Config: {}", appConfig);
        logger.debug("OPCUA Client Config: {}", opcuaClientConfig);
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

    private AppConfig loadAppConfig() {
        try {
            AppConfig config = AppConfig.fromEnvironment();
            config.validate();
            return config;
        } catch (Exception e) {
            logger.error("Failed to load Infra configuration", e);
            throw new RuntimeException("Infra configuration error", e);
        }
    }

    private OPCUAClientConfig loadOPCUAClientConfig() {
        try {
            String serverUrl = System.getenv("OPCUA_SERVER_URL");
            if (serverUrl == null) {
                throw new IllegalArgumentException("OPCUA_SERVER_URL environment variable is required");
            }
            String username = System.getenv("OPCUA_USERNAME");
            String password = System.getenv("OPCUA_PASSWORD");
            Long requestTimeoutMs = System.getenv().containsKey("OPCUA_REQUEST_TIMEOUT_MS")
                    ? Long.parseLong(System.getenv("OPCUA_REQUEST_TIMEOUT_MS"))
                    : null;
            Long sessionTimeoutMs = System.getenv().containsKey("OPCUA_SESSION_TIMEOUT_MS")
                    ? Long.parseLong(System.getenv("OPCUA_SESSION_TIMEOUT_MS"))
                    : null;
            Long keepAliveIntervalMs = System.getenv().containsKey("OPCUA_KEEP_ALIVE_INTERVAL_MS")
                    ? Long.parseLong(System.getenv("OPCUA_KEEP_ALIVE_INTERVAL_MS"))
                    : null;

            OPCUAClientConfig config = new OPCUAClientConfig()
                    .setServerUrl(serverUrl);

            if (username != null) {
                config.setUsername(username);
            }
            if (password != null) {
                config.setPassword(password);
            }
            if (requestTimeoutMs != null) {
                config.setRequestTimeout(Duration.ofMillis(requestTimeoutMs));
            }
            if (sessionTimeoutMs != null) {
                config.setSessionTimeout(Duration.ofMillis(sessionTimeoutMs));
            }
            if (keepAliveIntervalMs != null) {
                config.setKeepAliveInterval(Duration.ofMillis(keepAliveIntervalMs));
            }

            return config;
        } catch (Exception e) {
            logger.error("Failed to load OPC UA Client configuration", e);
            throw new RuntimeException("OPC UA Client configuration error", e);
        }
    }

    private void validateConfigurations() {
        try {
            postgreSQLConfig.validate();
            retryPolicyConfig.validate();
            appConfig.validate();
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

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public OPCUAClientConfig getOPCUAClientConfig() {
        return opcuaClientConfig;
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
        pgConfig.setMaxPoolSize(5);
        pgConfig.setMinPoolSize(1);

        // Create test retry config
        RetryPolicyConfig retryConfig = new RetryPolicyConfig();

        // Create test infra config
        AppConfig appConfig = new AppConfig();

        return new ConfigProvider(pgConfig, retryConfig, appConfig);
    }

    /**
     * Constructor for testing purposes
     */
    private ConfigProvider(PostgreSQLConfig postgreSQLConfig, RetryPolicyConfig retryPolicyConfig,
            AppConfig appConfig) {
        this.postgreSQLConfig = postgreSQLConfig;
        this.retryPolicyConfig = retryPolicyConfig;
        this.appConfig = appConfig;
        this.opcuaClientConfig = new OPCUAClientConfig().setServerUrl("opc.tcp://localhost:4840");
        validateConfigurations();
        logger.info("Test configuration loaded");
    }
}
