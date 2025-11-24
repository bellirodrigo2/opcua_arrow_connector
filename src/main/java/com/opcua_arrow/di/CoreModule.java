package com.opcua_arrow.di;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.data_point.DataPointParams;
import com.opcua_arrow.data_point.DataReadGroup;
import com.opcua_arrow.data_point.DataValue;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.queues.QueueWrapper;
import com.opcua_arrow.read.ReadTask;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<Map<String, DataPointParams>>() {
        }).toInstance(new ConcurrentHashMap<>());

        bind(new TypeLiteral<IQueue<List<IOPCUADataValue>>>() {
        }).toInstance(new QueueWrapper<>(1000, 5000));

        bind(new TypeLiteral<IQueue<Map<DataWriteGroup, List<DataValue>>>>() {
        }).toInstance(new QueueWrapper<>(1000, 5000));
    }

    @Provides
    @Singleton
    public Map<DataReadGroup, ReadTask> provideReadersMap() {
        return new ConcurrentHashMap<>();
    }

    @Provides
    @Singleton
    public Map<DataWriteGroup, IBufferBuilder> provideBatchBuffersMap() {
        return new ConcurrentHashMap<>();
    }
}
