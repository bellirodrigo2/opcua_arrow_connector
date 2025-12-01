package com.opcua_arrow.loop;

import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.data.DataWriteGroup;

public interface IBatchBufferFactory {
    IBufferBuilder createBatchBuffer(DataWriteGroup group);
}
