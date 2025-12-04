package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.opcua_arrow.metrics.CallbackMetrics;
import com.opcua_arrow.metrics.OTelConfig;
import com.opcua_arrow.metrics.PushGatewayService;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.api.trace.Tracer;

public class MetricsModule extends AbstractModule {

    @Override
    protected void configure() {

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        bind(PrometheusMeterRegistry.class).toInstance(registry);

        Tracer tracer = OTelConfig.init("http://localhost:4317", "opcua-arrow-connector");
        bind(Tracer.class).toInstance(tracer);

        PushGatewayService push = new PushGatewayService("http://localhost:9091", registry);
        bind(PushGatewayService.class).toInstance(push);

        bind(CallbackMetrics.class)
                .toInstance(new CallbackMetrics(registry, tracer, push));
    }
}
