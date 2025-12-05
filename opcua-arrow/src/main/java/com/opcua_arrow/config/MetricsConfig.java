package com.opcua_arrow.config;

import java.util.Map;

public class MetricsConfig {

    private String otlpUrl;
    private String serviceName;
    private String prometheusUrl;

    public String getOtlpUrl() {
        return otlpUrl;
    }

    public void setOtlpUrl(String otlpUrl) {
        this.otlpUrl = otlpUrl;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setPrometheusUrl(String prometheusUrl) {
        this.prometheusUrl = prometheusUrl;
    }

    public String getPrometheusUrl() {
        return prometheusUrl;
    }

    public static MetricsConfig fromMap(Map<String, String> configMap) {
        return new MetricsConfig();
    }

}
