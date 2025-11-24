package com.opcua_arrow.di;

import java.util.List;
import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.data_point.DataPointParams;
import com.opcua_arrow.data_point.DataValue;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.transform.DataTransform;
import com.opcua_arrow.transform.ITransform;

public class TransformModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ITransform.class).to(DataTransform.class);
    }

    @Provides
    @Singleton
    public DataTransform provideDataTransform(
            IQueue<List<IOPCUADataValue>> source,
            IQueue<Map<DataWriteGroup, List<DataValue>>> sink,
            Map<String, DataPointParams> paramsMap) {

        return new DataTransform(source, sink, paramsMap);
    }
}
