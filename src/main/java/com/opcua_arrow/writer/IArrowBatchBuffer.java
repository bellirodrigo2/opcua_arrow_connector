package com.opcua_arrow.writer;

public interface IArrowBatchBuffer<T> extends AutoCloseable {

    /** Add one registry */
    void append(int id, long timestampNanos, T value, int statusCode);

    /** Remove last register */
    void pop();

    /** Serialize the current batch to Arrow IPC and clean the buffer */
    byte[] flush();

    /** Register size */
    int size();

    /** Current capacity */
    int capacity();

    /** Clean the counter */
    void reset();

    @Override
    void close();
}
