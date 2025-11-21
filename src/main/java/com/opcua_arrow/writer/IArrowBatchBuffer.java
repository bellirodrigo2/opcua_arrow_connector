package com.opcua_arrow.writer;

public interface IArrowBatchBuffer extends AutoCloseable {

    /** Add one registry */
    void append(int id, long timestampNanos, Object value, int statusCode);

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
