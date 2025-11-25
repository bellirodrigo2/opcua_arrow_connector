package com.opcua_arrow.di;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.data_point.TSValue;
import com.opcua_arrow.maps.BufferRegistry;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.writer.IWriter;
import com.opcua_arrow.writer.LoopWriter;

public class WriterModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IWriter.class).to(LoopWriter.class);
    }

    @Provides
    @Singleton
    public LoopWriter provideQueueWriter(
            BufferRegistry bufferRegistry,
            IQueue<List<TSValue>> source,
            ISend sender) {
        return new LoopWriter(bufferRegistry, source, sender);
    }
}
