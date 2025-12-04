package com.opcua_arrow.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

public class OTelConfig {

        public static Tracer init(String url, String serviceName) {
                if (url == null || url.isEmpty()) {
                        throw new IllegalArgumentException("OTel endpoint URL must be provided");
                }
                if (serviceName == null || serviceName.isEmpty()) {
                        throw new IllegalArgumentException("Service name must be provided");
                }

                OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                                .setEndpoint(url)
                                .build();

                SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                                .build();

                OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                                .setTracerProvider(tracerProvider)
                                .build();

                GlobalOpenTelemetry.set(openTelemetry);

                return openTelemetry.getTracer(serviceName);
        }
}
