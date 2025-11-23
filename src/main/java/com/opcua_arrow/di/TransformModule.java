package com.opcua_arrow.di;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.opcua.DataPointParams;
import com.opcua_arrow.data_point.opcua.DataValue;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.transform.ITransform;
import com.opcua_arrow.transform.opcua.DataTransform;

public class TransformModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ITransform.class).to(DataTransform.class);
    }

    @Provides
    @Singleton
    public DataTransform provideDataTransform(
            @ReaderToTransformQueue BlockingQueue<List<IOPCUADataValue<?>>> source,
            @TransformToWriterQueue BlockingQueue<Map<DataWriteGroup, List<DataValue<?>>>> sink,
            Map<String, DataPointParams> paramsMap) {

        long pollTimeoutSeconds = 5L; // Configurável via properties
        return new DataTransform(source, pollTimeoutSeconds, sink, paramsMap);
    }
}