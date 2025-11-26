package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.opcua.IOPCUASubscriber;
import com.opcua_arrow.opcua.milo.MiloOPCUAConnection;
import com.opcua_arrow.opcua.milo.MiloOPCUAReader;
import com.opcua_arrow.opcua.milo.MiloOPCUASubscription;
import com.opcua_arrow.opcua.milo.TSValueAlarmFactory;
import com.opcua_arrow.opcua.milo.TSValueFactory;
import com.opcua_arrow.retry.IRetryPolicy;
import com.opcua_arrow.retry.resilience4j.Resilience4jRetryPolicy;

public class OPCUAModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bindings
    }

    @Provides
    public OPCUAClientConfig provideOPCUAClientConfig(ConfigProvider configProvider) {
        return configProvider.getOPCUAClientConfig();
    }

    @Provides
    public IRetryPolicy provideRetryPolicy(ConfigProvider configProvider) {
        return new Resilience4jRetryPolicy(configProvider.getRetryPolicyConfig());
    }

    @Provides
    public TSValueFactory provideTSValueFactory() {
        return new TSValueFactory();
    }

    @Provides
    public TSValueFactory provideTSValueAlarmFactory() {
        return new TSValueFactory();
    }

    @Provides
    public IOPCUAConnection provideOPCUAConnection(
            OPCUAClientConfig config,
            IRetryPolicy retryPolicy) {
        return new MiloOPCUAConnection(config, retryPolicy);
    }

    @Provides
    public IOPCUAReader provideOPCUAReader(
            IOPCUAConnection connection,
            IRetryPolicy retryPolicy,
            TSValueFactory tsValueFactory) {
        return new MiloOPCUAReader(connection, retryPolicy, tsValueFactory);
    }

    @Provides
    @Singleton
    public IOPCUASubscriber provideOPCUASubscriber(
            IOPCUAConnection connection,
            TSValueFactory tsValueFactory,
            TSValueAlarmFactory alarmTsValueFactory,
            OPCUAClientConfig config) {
        int queueSize = config.getSubscriberQueueSize();
        return new MiloOPCUASubscription(
                connection,
                tsValueFactory,
                alarmTsValueFactory,
                queueSize);
    }
}
