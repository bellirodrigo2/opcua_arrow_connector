package com.opcua_arrow.batch_builder.v2;

import com.opcua_arrow.batch_builder.arrow.IValueColumn;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.types.pojo.Schema;

public class AcumBatchArrowBuilder extends BaseArrowBufferBuilder {

    private int minBatchSize;
    private long minFlushIntervalNanos;
    private long lastFlushTimeNanos = System.nanoTime();

    public AcumBatchArrowBuilder(
            Schema schema,
            int initialCapacity,
            BufferAllocator allocator,
            boolean compress,
            IValueColumn valueColumn,
            int minBatchSize,
            long minFlushIntervalNanos) {
        super(schema, initialCapacity, allocator, compress, valueColumn);
        this.minBatchSize = minBatchSize;
        this.minFlushIntervalNanos = minFlushIntervalNanos;
    }

    public AcumBatchArrowBuilder(
            int initialCapacity,
            boolean compress,
            IValueColumn valueColumn,
            int minBatchSize,
            long minFlushIntervalNanos) {
        super(initialCapacity, compress, valueColumn);
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
