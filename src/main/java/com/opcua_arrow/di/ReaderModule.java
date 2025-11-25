package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.read.LoopReader;
import com.opcua_arrow.registry.ReadTaskRegistry;

public class ReaderModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IReader.class).to(LoopReader.class);
    }

    @Provides
    @Singleton
    public LoopReader provideLoopReader(ReadTaskRegistry readTaskRegistry) {
        return new LoopReader(readTaskRegistry);
    }
}
