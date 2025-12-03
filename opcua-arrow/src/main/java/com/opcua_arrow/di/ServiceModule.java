package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.opcua_arrow.service.DataPointLoader;

/**
 * Module for application services
 */
public class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DataPointLoader.class).asEagerSingleton();
    }
}