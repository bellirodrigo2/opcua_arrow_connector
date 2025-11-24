package com.opcua_arrow.di;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.InfraConfig;
import com.opcua_arrow.data_point.DataPointParams;
import com.opcua_arrow.data_point.DataValue;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.queues.QueueWrapper;

public class CoreModule extends AbstractModule {

    private final ConfigProvider configProvider;

    public CoreModule(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    protected void configure() {
        bind(new TypeLiteral<Map<String, DataPointParams>>() {
        }).toInstance(new ConcurrentHashMap<>());

        InfraConfig infraConfig = configProvider.getInfraConfig();
        int capacity = infraConfig.getInitialQueueCapacity();
        int timeoutMs = infraConfig.getQueueTimeoutMs();

        bind(new TypeLiteral<IQueue<List<IOPCUADataValue>>>() {
        }).toInstance(new QueueWrapper<>(capacity, timeoutMs));

        bind(new TypeLiteral<IQueue<Map<DataWriteGroup, List<DataValue>>>>() {
        }).toInstance(new QueueWrapper<>(capacity, timeoutMs));
    }
}
