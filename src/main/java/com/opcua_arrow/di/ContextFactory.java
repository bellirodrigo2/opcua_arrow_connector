package com.opcua_arrow.di;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.opcua_arrow.context.Context;

/**
 * Factory simplificada - agora o Context é injetado diretamente pelo Guice
 */
public class ContextFactory {

    private final Injector injector;

    @Inject
    public ContextFactory(Injector injector) {
        this.injector = injector;
    }

    public Context createContext() {
        return injector.getInstance(Context.class);
    }
}