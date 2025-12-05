package com.opcua_arrow.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for OPC-UA client.
 */
public class OPCUAClientConfig {
    private String serverUrl;
    private String username;
    private String password;
    private Duration requestTimeout;
    private Duration sessionTimeout;
    private Duration keepAliveInterval;
    private Map<String, Object> additionalProperties = new HashMap<>();

    public String getServerUrl() {
        return serverUrl;
    }

    public OPCUAClientConfig setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public OPCUAClientConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public OPCUAClientConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public OPCUAClientConfig setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public OPCUAClientConfig setSessionTimeout(Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    public Duration getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public OPCUAClientConfig setKeepAliveInterval(Duration keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
        return this;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public OPCUAClientConfig setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

    public OPCUAClientConfig addProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
        return this;
    }

    public static OPCUAClientConfig fromMap(Map<String, String> configMap) {
        return new OPCUAClientConfig();
    }
}
