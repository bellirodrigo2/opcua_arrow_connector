package com.opcua_arrow.di;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.batch_builder.arrow.AcumBatchArrowBuilder;
import com.opcua_arrow.batch_builder.arrow.IValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.BooleanArrayValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.BooleanValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.DoubleArrayValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.DoubleValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.StringValueColumn;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.InfraConfig;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.EDataType;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.loop.IReader;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.opcua.IOPCUASubscriber;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.read.ReadTask;
import com.opcua_arrow.read.SubscribeTask;

/**
 * Modulo para factories que criam objetos dinamicos
 */
public class FactoryModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bindings para factories
    }

    @Provides
    @Singleton
    public ReadTaskFactory provideReadTaskFactory(
            IQueue<List<TSValue>> queue,
            Provider<IOPCUAReader> opcuaReaderProvider,
            Provider<IOPCUASubscriber> opcuaSubscriberProvider) {

        return new ReadTaskFactory(queue, opcuaReaderProvider, opcuaSubscriberProvider);
    }

    @Provides
    @Singleton
    public BatchBufferFactory provideBatchBufferFactory(ConfigProvider configProvider) {
        return new BatchBufferFactory(configProvider.getInfraConfig());
    }

    public static class ReadTaskFactory {
        private final IQueue<List<TSValue>> queue;
        private final Provider<IOPCUAReader> opcuaReaderProvider;
        private final Provider<IOPCUASubscriber> opcuaSubscriberProvider;

        @Inject
        public ReadTaskFactory(
                IQueue<List<TSValue>> queue,
                Provider<IOPCUAReader> opcuaReaderProvider,
                Provider<IOPCUASubscriber> opcuaSubscriberProvider) {
            this.queue = queue;
            this.opcuaReaderProvider = opcuaReaderProvider;
            this.opcuaSubscriberProvider = opcuaSubscriberProvider;
        }

        public IReader createReader(DataReadGroup readGroup) {
            Long intervalSeconds = readGroup.getInterval();
            EReadMode readMode = readGroup.getReadMode();
            switch (readMode) {
                case EReadMode.READ:
                    return new ReadTask(opcuaReaderProvider.get(), intervalSeconds, queue);
                case EReadMode.SUBSCRIBE:
                    return new SubscribeTask(opcuaSubscriberProvider.get(), readGroup, queue);
                case EReadMode.EVENTS:
                    return new SubscribeTask(opcuaSubscriberProvider.get(), readGroup, queue);
                default:
                    break;
            }
            throw new IllegalArgumentException("Unsupported read mode: " + readGroup.getReadMode());
        }
    }

    public static class BatchBufferFactory {

        private final InfraConfig infraConfig;

        public BatchBufferFactory(InfraConfig infraConfig) {
            this.infraConfig = infraConfig;
        }

        public IBufferBuilder createBatchBuffer(DataWriteGroup group) {
            EDataType dataType = group.getDataType();
            IValueColumn valueColumn = createValueColumn(dataType);
            return new AcumBatchArrowBuilder(
                    infraConfig.getInitialBufferBuilderCapacity(),
                    infraConfig.isBufferCompressionEnabled(),
                    valueColumn,
                    infraConfig.getMinBufferFlushSize(),
                    infraConfig.getMinFlushIntervalNanos());
        }

        private IValueColumn createValueColumn(EDataType dataType) {
            switch (dataType) {
                case EDataType.NUMERIC:
                    return new DoubleValueColumn();
                case EDataType.BOOLEAN:
                    return new BooleanValueColumn();
                case EDataType.STRING:
                    return new StringValueColumn();
                case EDataType.BOOLEAN_ARRAY:
                    return new BooleanArrayValueColumn();
                case EDataType.NUMERIC_ARRAY:
                    return new DoubleArrayValueColumn();
                default:
                    break;
            }
            throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }
    }

}
