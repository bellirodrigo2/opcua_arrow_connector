package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.opcua.milo.MiloOPCUAConnection;
import com.opcua_arrow.opcua.milo.MiloOPCUAReader;
import com.opcua_arrow.opcua.milo.MiloOPCUASubscription;
import com.opcua_arrow.opcua.milo.TSValueFactory;
import com.opcua_arrow.opcua.retry.IRetryPolicy;
import com.opcua_arrow.opcua.retry.resilience4j.Resilience4jRetryPolicy;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.read.ISubscriber;

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
    public MiloOPCUAConnection provideOPCUAConnection(
            OPCUAClientConfig config,
            IRetryPolicy retryPolicy) {
        return new MiloOPCUAConnection(config, retryPolicy);
    }

    @Provides
    public IReader provideOPCUAReader(
            MiloOPCUAConnection connection,
            IRetryPolicy retryPolicy,
            TSValueFactory tsValueFactory) {
        return new MiloOPCUAReader(connection, retryPolicy, tsValueFactory);
    }

    @Provides
    @Singleton
    public ISubscriber provideOPCUASubscriber(
            MiloOPCUAConnection connection,
            OPCUAClientConfig config) {
        return new MiloOPCUASubscription(
                connection, null);
    }
}
