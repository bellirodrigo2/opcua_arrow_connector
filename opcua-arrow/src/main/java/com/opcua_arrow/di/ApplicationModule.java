package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.opcua_arrow.config.AppConfig;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.MetricsConfig;
import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.PostgreSQLConfig;
import com.opcua_arrow.config.RetryPolicyConfig;

/**
 * Módulo principal que instala todos os outros módulos
 */
public class ApplicationModule extends AbstractModule {

    private final ConfigProvider configProvider;

    public ApplicationModule(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    protected void configure() {
        // Bind ConfigProvider as singleton
        bind(ConfigProvider.class).toInstance(configProvider);
        bind(AppConfig.class).toInstance(configProvider.getAppConfig());

        // Bind typed configs
        bind(MetricsConfig.class).toInstance(configProvider.getMetricsConfig());
        bind(OPCUAClientConfig.class).toInstance(configProvider.getOPCUAClientConfig());
        bind(PostgreSQLConfig.class).toInstance(configProvider.getPostgreSQLConfig());
        bind(RetryPolicyConfig.class).toInstance(configProvider.getRetryPolicyConfig());

        install(new CoreModule(configProvider));
        install(new OPCUAModule());
        install(new ReaderModule());
        install(new MetricsModule());
        install(new WriterModule());
        install(new FactoryModule());
        install(new DataProviderModule()); // Data source integration
        install(new ServiceModule()); // Application services
    }
}
