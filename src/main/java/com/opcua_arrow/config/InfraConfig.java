package com.opcua_arrow.config;

/**
 * Configuration for infrastructure components (queues, buffers, etc.)
 */
public class InfraConfig {

    private int initialQueueCapacity = 1000;
    private int queueTimeoutMs = 5000;

    private int initialBufferBuilderCapacity = 1024;
    private boolean bufferCompressionEnabled = true;

    private int minBufferFlushSize = 100;
    private long minFlushIntervalNanos = 1_000_000_000L; // 1 second
    private int bufferFlushIntervalMs = 1000;

    public int getInitialQueueCapacity() {
        return initialQueueCapacity;
    }

    public InfraConfig setInitialQueueCapacity(int initialQueueCapacity) {
        this.initialQueueCapacity = initialQueueCapacity;
        return this;
    }

    public int getQueueTimeoutMs() {
        return queueTimeoutMs;
    }

    public InfraConfig setQueueTimeoutMs(int queueTimeoutMs) {
        this.queueTimeoutMs = queueTimeoutMs;
        return this;
    }

    public int getInitialBufferBuilderCapacity() {
        return initialBufferBuilderCapacity;
    }

    public InfraConfig setInitialBufferBuilderCapacity(int initialBufferBuilderCapacity) {
        this.initialBufferBuilderCapacity = initialBufferBuilderCapacity;
        return this;
    }

    public boolean isBufferCompressionEnabled() {
        return bufferCompressionEnabled;
    }

    public InfraConfig setBufferCompressionEnabled(boolean bufferCompressionEnabled) {
        this.bufferCompressionEnabled = bufferCompressionEnabled;
        return this;
    }

    public int getMinBufferFlushSize() {
        return minBufferFlushSize;
    }

    public InfraConfig setMinBufferFlushSize(int minBufferFlushSize) {
        this.minBufferFlushSize = minBufferFlushSize;
        return this;
    }

    public long getMinFlushIntervalNanos() {
        return minFlushIntervalNanos;
    }

    public InfraConfig setMinFlushIntervalNanos(long minFlushIntervalNanos) {
        this.minFlushIntervalNanos = minFlushIntervalNanos;
        return this;
    }

    public int getBufferFlushIntervalMs() {
        return bufferFlushIntervalMs;
    }

    public InfraConfig setBufferFlushIntervalMs(int bufferFlushIntervalMs) {
        this.bufferFlushIntervalMs = bufferFlushIntervalMs;
        return this;
    }

    /**
     * Create configuration from environment variables
     */
    public static InfraConfig fromEnvironment() {
        InfraConfig config = new InfraConfig();

        // Queue settings
        String initialQueueCapacity = System.getenv("QUEUE_INITIAL_CAPACITY");
        if (initialQueueCapacity != null) {
            config.setInitialQueueCapacity(Integer.parseInt(initialQueueCapacity));
        }

        String queueTimeoutMs = System.getenv("QUEUE_TIMEOUT_MS");
        if (queueTimeoutMs != null) {
            config.setQueueTimeoutMs(Integer.parseInt(queueTimeoutMs));
        }

        // Buffer settings
        String initialBufferBuilderCapacity = System.getenv("BUFFER_INITIAL_CAPACITY");
        if (initialBufferBuilderCapacity != null) {
            config.setInitialBufferBuilderCapacity(Integer.parseInt(initialBufferBuilderCapacity));
        }

        String bufferCompressionEnabled = System.getenv("BUFFER_COMPRESSION_ENABLED");
        if (bufferCompressionEnabled != null) {
            config.setBufferCompressionEnabled(Boolean.parseBoolean(bufferCompressionEnabled));
        }

        String minBufferFlushSize = System.getenv("BUFFER_MIN_FLUSH_SIZE");
        if (minBufferFlushSize != null) {
            config.setMinBufferFlushSize(Integer.parseInt(minBufferFlushSize));
        }

        String minFlushIntervalNanos = System.getenv("BUFFER_MIN_FLUSH_INTERVAL_NANOS");
        if (minFlushIntervalNanos != null) {
            config.setMinFlushIntervalNanos(Long.parseLong(minFlushIntervalNanos));
        }

        String bufferFlushIntervalMs = System.getenv("BUFFER_FLUSH_INTERVAL_MS");
        if (bufferFlushIntervalMs != null) {
            config.setBufferFlushIntervalMs(Integer.parseInt(bufferFlushIntervalMs));
        }

        return config;
    }

    /**
     * Validate configuration
     */
    public void validate() {
        if (initialQueueCapacity <= 0) {
            throw new IllegalArgumentException("Initial queue capacity must be positive");
        }
        if (queueTimeoutMs <= 0) {
            throw new IllegalArgumentException("Queue timeout must be positive");
        }
        if (initialBufferBuilderCapacity <= 0) {
            throw new IllegalArgumentException("Initial buffer builder capacity must be positive");
        }
        if (minBufferFlushSize <= 0) {
            throw new IllegalArgumentException("Min buffer flush size must be positive");
        }
        if (minFlushIntervalNanos <= 0) {
            throw new IllegalArgumentException("Min flush interval nanos must be positive");
        }
        if (bufferFlushIntervalMs <= 0) {
            throw new IllegalArgumentException("Buffer flush interval must be positive");
        }
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
                ", bufferFlushIntervalMs=" + bufferFlushIntervalMs +
                '}';
    }
}
