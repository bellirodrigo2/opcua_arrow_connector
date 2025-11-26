package com.opcua_arrow.di;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.opcua_arrow.data.BufferPackage;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.di.FactoryModule.BatchBufferFactory;
import com.opcua_arrow.loop.ILoop;
import com.opcua_arrow.loop.LoopWriter;
import com.opcua_arrow.queues.IQueue;

public class WriterModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ILoop.class)
                .annotatedWith(Names.named("writer"))
                .to(LoopWriter.class)
                .in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    public LoopWriter provideLoopWriter(
            IQueue<List<TSValue>> source,
            IQueue<BufferPackage> sink,
            BatchBufferFactory factory) {
        return new LoopWriter(source, sink, factory);
    }
}
