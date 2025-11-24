package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.opcua_arrow.config.ConfigProvider;

/**
 * Módulo principal que instala todos os outros módulos
 */
public class ApplicationModule extends AbstractModule {

    private final ConfigProvider configProvider;

    public ApplicationModule() {
        this.configProvider = new ConfigProvider();
    }

    public ApplicationModule(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    protected void configure() {
        // Bind ConfigProvider as singleton
        bind(ConfigProvider.class).toInstance(configProvider);

        install(new CoreModule(configProvider));
        install(new ReaderModule());
        install(new TransformModule());
        install(new WriterModule());
        install(new FactoryModule());
        install(new SenderModule()); // Temporary bindings
        install(new DataProviderModule()); // Data source integration
        install(new ServiceModule()); // Application services
    }
}