package com.opcua_arrow.config;

import java.util.Map;

/**
 * Configuration for infrastructure components (queues, buffers, etc.)
 */
public class AppConfig {

    private String sourceName;
    private long updateIntervalSeconds;

    private int initialQueueCapacity;
    private int queueTimeoutMs;

    private int initialBufferBuilderCapacity;
    private boolean bufferCompressionEnabled;

    private int minBufferFlushSize;
    private long minFlushIntervalNanos; // 1 second

    public String getSourceName() {
        return sourceName;
    }

    public AppConfig setSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    public long getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    public AppConfig setUpdateIntervalSeconds(long updateIntervalSeconds) {
        this.updateIntervalSeconds = updateIntervalSeconds;
        return this;
    }

    public int getInitialQueueCapacity() {
        return initialQueueCapacity;
    }

    public AppConfig setInitialQueueCapacity(int initialQueueCapacity) {
        this.initialQueueCapacity = initialQueueCapacity;
        return this;
    }

    public int getQueueTimeoutMs() {
        return queueTimeoutMs;
    }

    public AppConfig setQueueTimeoutMs(int queueTimeoutMs) {
        this.queueTimeoutMs = queueTimeoutMs;
        return this;
    }

    public int getInitialBufferBuilderCapacity() {
        return initialBufferBuilderCapacity;
    }

    public AppConfig setInitialBufferBuilderCapacity(int initialBufferBuilderCapacity) {
        this.initialBufferBuilderCapacity = initialBufferBuilderCapacity;
        return this;
    }

    public boolean isBufferCompressionEnabled() {
        return bufferCompressionEnabled;
    }

    public AppConfig setBufferCompressionEnabled(boolean bufferCompressionEnabled) {
        this.bufferCompressionEnabled = bufferCompressionEnabled;
        return this;
    }

    public int getMinBufferFlushSize() {
        return minBufferFlushSize;
    }

    public AppConfig setMinBufferFlushSize(int minBufferFlushSize) {
        this.minBufferFlushSize = minBufferFlushSize;
        return this;
    }

    public long getMinFlushIntervalNanos() {
        return minFlushIntervalNanos;
    }

    public AppConfig setMinFlushIntervalNanos(long minFlushIntervalNanos) {
        this.minFlushIntervalNanos = minFlushIntervalNanos;
        return this;
    }

    @Override
    public String toString() {
        return "InfraConfig{" +
                "initialQueueCapacity=" + initialQueueCapacity +
                ", queueTimeoutMs=" + queueTimeoutMs +
                ", initialBufferBuilderCapacity=" + initialBufferBuilderCapacity +
                ", bufferCompressionEnabled=" + bufferCompressionEnabled +
                ", minBufferFlushSize=" + minBufferFlushSize +
                ", minFlushIntervalNanos=" + minFlushIntervalNanos +
                '}';
    }

    public static AppConfig fromMap(Map<String, String> configMap) {
        return new AppConfig();
    }
}
