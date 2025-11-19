package com.opcua_arrow.arrow;

public interface IArrowBatchBuffer<TId, TValue> extends AutoCloseable {

    /** Add one registry */
    void append(TId id, long timestampNanos, TValue value, int statusCode);

    /** Remove last register*/
    void pop();

    /** Serialize the current batch to Arrow IPC and clean the  buffer */
    byte[] flush();

    /** Register size*/
    int size();

    /** Current capacity */
    int capacity();

    /** Clean the counter */
    void reset();

    @Override
    void close();

    Class<TValue> getValueClass();
}
