package com.opcua_arrow.di;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.batch_builder.IArrowBatchBuffer;
import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.opcua.DataValue;
import com.opcua_arrow.writer.IWriter;
import com.opcua_arrow.writer.QueueWriter;

public class WriterModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IWriter.class).to(QueueWriter.class);
    }

    @Provides
    @Singleton
    public QueueWriter provideQueueWriter(
            Map<DataWriteGroup, IArrowBatchBuffer> batchBuffers,
            @TransformToWriterQueue BlockingQueue<Map<DataWriteGroup, List<DataValue<?>>>> source,
            ISend sender) {
        
        long pollTimeoutSeconds = 5L; // Configurável via properties
        return new QueueWriter(batchBuffers, source, pollTimeoutSeconds, sender);
    }
}