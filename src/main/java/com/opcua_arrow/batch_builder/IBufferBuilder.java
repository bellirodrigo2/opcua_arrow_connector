package com.opcua_arrow.batch_builder;

import java.util.List;

import com.opcua_arrow.data.TSValue;

public interface IBufferBuilder extends AutoCloseable {

    /** Add one registry */
    void appendList(List<TSValue> dataValues);

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
