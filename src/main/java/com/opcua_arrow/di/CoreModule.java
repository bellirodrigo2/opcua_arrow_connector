package com.opcua_arrow.di;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.opcua_arrow.IContext;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.InfraConfig;
import com.opcua_arrow.context.Context;
import com.opcua_arrow.data.BufferPackage;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.queues.QueueWrapper;

public class CoreModule extends AbstractModule {

    private final ConfigProvider configProvider;

    public CoreModule(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    protected void configure() {

        bind(IContext.class).to(Context.class);

        bind(new TypeLiteral<Map<String, DataPoint>>() {
        }).toInstance(new ConcurrentHashMap<>());

        InfraConfig infraConfig = configProvider.getInfraConfig();
        int capacity = infraConfig.getInitialQueueCapacity();
        int timeoutMs = infraConfig.getQueueTimeoutMs();

        bind(new TypeLiteral<IQueue<List<TSValue>>>() {
        }).toInstance(new QueueWrapper<>(capacity, timeoutMs, null));

        bind(new TypeLiteral<IQueue<BufferPackage>>() {
        }).toInstance(new QueueWrapper<>(capacity, timeoutMs, null));

    }
}
