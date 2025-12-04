package com.opcua_arrow.metrics;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.PushGateway;

public class PushGatewayService {

    private final PushGateway pushGateway;
    private final PrometheusMeterRegistry registry;

    public PushGatewayService(String pushGatewayUrl, PrometheusMeterRegistry registry) {
        this.pushGateway = new PushGateway(pushGatewayUrl);
        this.registry = registry;
    }

    public void push(String jobName) {
        try {
            pushGateway.pushAdd(registry.getPrometheusRegistry(), jobName);
        } catch (Exception e) {
            System.err.println("Failed to push metrics: " + e.getMessage());
        }
    }
}
