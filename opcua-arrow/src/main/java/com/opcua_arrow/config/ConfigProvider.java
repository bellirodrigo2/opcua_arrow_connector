package com.opcua_arrow.config;

import java.util.Map;

/**
 * Central configuration provider for the application
 */
public class ConfigProvider {

    private final PostgreSQLConfig postgreSQLConfig;
    private final RetryPolicyConfig retryPolicyConfig;
    private final AppConfig appConfig;
    private final OPCUAClientConfig opcuaClientConfig;
    private final MetricsConfig metricsConfig;

    public ConfigProvider(PostgreSQLConfig postgreSQLConfig, OPCUAClientConfig opcuaClientConfig,
            RetryPolicyConfig retryPolicyConfig,
            AppConfig appConfig, MetricsConfig metricsConfig) {
        this.postgreSQLConfig = postgreSQLConfig;
        this.retryPolicyConfig = retryPolicyConfig;
        this.appConfig = appConfig;
        this.opcuaClientConfig = opcuaClientConfig;
        this.metricsConfig = metricsConfig;
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

    public MetricsConfig getMetricsConfig() {
        return metricsConfig;
    }

    public static ConfigProvider fromMaps(Map<String, String> configMap) {

        var pgConfig = PostgreSQLConfig.fromMap(configMap);
        var opcuaConfig = OPCUAClientConfig.fromMap(configMap);
        var retryConfig = RetryPolicyConfig.fromMap(configMap);
        var appConfig = AppConfig.fromMap(configMap);
        var metricsConfig = MetricsConfig.fromMap(configMap);
        return new ConfigProvider(pgConfig, opcuaConfig, retryConfig, appConfig, metricsConfig);
    }
}
