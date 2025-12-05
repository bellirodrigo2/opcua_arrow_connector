package com.opcua_arrow.batch_builder;

import java.util.List;

import com.opcua_arrow.data.TSValue;

public interface IBufferBuilder extends AutoCloseable {

    void appendList(List<TSValue> dataValues);

    byte[] flush();

    // void emitBatch() throws Exception;

    @Override
    void close();
}
