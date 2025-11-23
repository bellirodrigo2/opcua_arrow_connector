package com.opcua_arrow.di;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.opcua_arrow.batch_builder.IArrowBatchBuffer;
import com.opcua_arrow.data_point.DataReadGroup;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.opcua.DataPointParams;
import com.opcua_arrow.data_point.opcua.DataValue;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.read.ReadTask;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
        // Binding dos tipos genéricos
        bind(new TypeLiteral<Map<String, DataPointParams>>() {
        })
                .toInstance(new ConcurrentHashMap<>());
    }

    @Provides
    @Singleton
    @ReaderToTransformQueue
    public BlockingQueue<List<IOPCUADataValue<?>>> provideReaderToTransformQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Provides
    @Singleton
    @TransformToWriterQueue
    public BlockingQueue<Map<DataWriteGroup, List<DataValue<?>>>> provideTransformToWriterQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Provides
    @Singleton
    public Map<DataReadGroup, ReadTask> provideReadersMap() {
        return new ConcurrentHashMap<>();
    }

    @Provides
    @Singleton
    public Map<DataWriteGroup, IArrowBatchBuffer> provideBatchBuffersMap() {
        return new ConcurrentHashMap<>();
    }
}