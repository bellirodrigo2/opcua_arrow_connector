package com.opcua_arrow.writer.arrow;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.types.pojo.Schema;

public class AcumBatchArrowBuilder extends BaseArrowBatchBuilder {

    public AcumBatchArrowBuilder(
            Schema schema,
            int initialCapacity,
            BufferAllocator allocator,
            boolean compress,
            IValueColumn valueColumn) {
        super(schema, initialCapacity, allocator, compress, valueColumn);
    }

    public AcumBatchArrowBuilder(
            int initialCapacity,
            boolean compress,
            IValueColumn valueColumn) {
        super(initialCapacity, compress, valueColumn);
    }

    private int minBatchSize = 1;
    private long minFlushIntervalNanos = 0;
    private long lastFlushTimeNanos = System.nanoTime();

    public void setMinBatchSize(int v) {
        this.minBatchSize = v;
    }

    public void setMinFlushIntervalSeconds(int sec) {
        this.minFlushIntervalNanos = sec * 1_000_000_000L;
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
