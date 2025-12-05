package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.ICallBack;
import com.opcua_arrow.config.MetricsConfig;
import com.opcua_arrow.metrics.CallbackMetrics;
import com.opcua_arrow.metrics.OTelConfig;
import com.opcua_arrow.metrics.PushGatewayService;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.api.trace.Tracer;

public class MetricsModule extends AbstractModule {

    @Provides
    @Singleton
    public Tracer provideTracer(MetricsConfig metricsConfig) {
        return OTelConfig.init(metricsConfig.getOtlpUrl(), metricsConfig.getServiceName());
    }

    @Provides
    @Singleton
    public PrometheusMeterRegistry provideRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Provides
    @Singleton
    public PushGatewayService providePushGatewayService(MetricsConfig metricsConfig,
            PrometheusMeterRegistry registry) {
        return new PushGatewayService(metricsConfig.getPrometheusUrl(), registry);
    }

    @Provides
    @Singleton
    public CallbackMetrics provideCallbackMetrics(
            PrometheusMeterRegistry registry,
            Tracer tracer,
            PushGatewayService push) {
        return new CallbackMetrics(registry, tracer, push);
    }

    @Override
    protected void configure() {

        bind(ICallBack.class).to(CallbackMetrics.class).asEagerSingleton();
    }
}
