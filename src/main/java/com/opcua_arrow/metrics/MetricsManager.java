package com.opcua_arrow.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsManager {
    private static MetricsManager instance;
    private final PrometheusMeterRegistry registry;
    JvmGcMetrics gc;

    private MetricsManager(int port) {
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // Registrar todas as métricas de JVM
        registerJvmMetrics();

        // Registrar métricas customizadas
        registerCustomMetrics();

    }

    private void registerJvmMetrics() {
        // ===== MEMÓRIA =====
        // Heap, Non-Heap, Eden, Survivor, Old Gen, Metaspace, etc.
        new JvmMemoryMetrics().bindTo(registry);

        // ===== GARBAGE COLLECTION =====
        // Contadores de GC, tempo gasto, etc.
        gc = new JvmGcMetrics();
        gc.bindTo(registry);

        // ===== THREADS =====
        // Threads ativos, daemon, pico, deadlocks
        new JvmThreadMetrics().bindTo(registry);

        // ===== CLASS LOADER =====
        // Classes carregadas/descarregadas
        new ClassLoaderMetrics().bindTo(registry);

        // ===== CPU =====
        // Uso de CPU do processo
        new ProcessorMetrics().bindTo(registry);

        // ===== FILE DESCRIPTORS =====
        // Arquivos abertos (importante para conexões)
        new FileDescriptorMetrics().bindTo(registry);

        // ===== UPTIME =====
        new UptimeMetrics().bindTo(registry);

        // ===== INFO DA JVM =====
        new JvmInfoMetrics().bindTo(registry);
    }

    private void registerCustomMetrics() {
        // Métricas customizadas adicionais do seu connector
        Tags commonTags = Tags.of(
                "application", "kafka-connector",
                "environment", System.getProperty("environment", "production"));

        registry.config().commonTags(commonTags);
    }

    public static synchronized MetricsManager getInstance(int port) {
        if (instance == null) {
            instance = new MetricsManager(port);
        }
        return instance;
    }

    public MeterRegistry getRegistry() {
        return registry;
    }

    public void shutdown() {
        gc.close();
    }
}
