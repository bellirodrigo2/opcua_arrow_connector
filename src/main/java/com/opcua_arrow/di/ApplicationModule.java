package com.opcua_arrow.di;

import com.google.inject.AbstractModule;

/**
 * Módulo principal que instala todos os outros módulos
 */
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new CoreModule());
        install(new ReaderModule());
        install(new TransformModule());
        install(new WriterModule());
        install(new FactoryModule());
        install(new SenderModule()); // Temporary bindings
        install(new DataProviderModule()); // Data source integration
    }
}