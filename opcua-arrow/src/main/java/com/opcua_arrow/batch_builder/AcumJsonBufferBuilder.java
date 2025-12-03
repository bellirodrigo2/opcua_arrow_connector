package com.opcua_arrow.batch_builder;

public class AcumJsonBufferBuilder extends JsonBufferBuilder {

    private int minBatchSize;
    private long minFlushIntervalNanos;
    private long lastFlushTimeNanos = System.nanoTime();

    public AcumJsonBufferBuilder(String sourceName, int minBatchSize, long minFlushIntervalNanos) {
        super(sourceName);
        this.minBatchSize = minBatchSize;
        this.minFlushIntervalNanos = minFlushIntervalNanos;
    }

    @Override
    public byte[] flush() {
        long now = System.nanoTime();

        boolean shouldFlush = size() >= minBatchSize || (now - lastFlushTimeNanos) >= minFlushIntervalNanos;

        if (!shouldFlush)
            return null;

        // chama flush REAL da classe base
        byte[] out = super.flush();

        lastFlushTimeNanos = now;
        return out;
    }
}
