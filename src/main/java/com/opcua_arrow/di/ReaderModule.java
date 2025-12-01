package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.opcua_arrow.RunningState;
import com.opcua_arrow.loop.ILoop;
import com.opcua_arrow.loop.IReadTaskFactory;
import com.opcua_arrow.loop.LoopReader;

public class ReaderModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ILoop.class)
                .annotatedWith(Names.named("reader"))
                .to(LoopReader.class)
                .in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    public LoopReader provideLoopReader(RunningState runningState, IReadTaskFactory factory) {
        return new LoopReader(factory, runningState);
    }
}
