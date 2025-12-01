package com.opcua_arrow.di;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.batch_builder.AcumJsonBufferBuilder;
import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.batch_builder.arrow.AcumBatchArrowBuilder;
import com.opcua_arrow.batch_builder.arrow.IValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.BooleanArrayValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.BooleanValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.DoubleArrayValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.DoubleValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.StringValueColumn;
import com.opcua_arrow.config.AppConfig;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.EDataType;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.loop.IBatchBufferFactory;
import com.opcua_arrow.loop.IReadTaskFactory;
import com.opcua_arrow.loop.IReaderTask;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.read.ISubscriber;
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
    public IReadTaskFactory provideReadTaskFactory(
            IQueue<List<TSValue>> queue,
            Provider<IReader> opcuaReaderProvider,
            Provider<ISubscriber> opcuaSubscriberProvider) {

        return new ReadTaskFactory(queue, opcuaReaderProvider, opcuaSubscriberProvider);
    }

    @Provides
    @Singleton
    public BatchBufferFactory provideBatchBufferFactory(ConfigProvider configProvider) {
        return new BatchBufferFactory(configProvider.getInfraConfig());
    }

    public static class ReadTaskFactory implements IReadTaskFactory {
        private final IQueue<List<TSValue>> queue;
        private final Provider<IReader> opcuaReaderProvider;
        private final Provider<ISubscriber> opcuaSubscriberProvider;

        @Inject
        public ReadTaskFactory(
                IQueue<List<TSValue>> queue,
                Provider<IReader> opcuaReaderProvider,
                Provider<ISubscriber> opcuaSubscriberProvider) {
            this.queue = queue;
            this.opcuaReaderProvider = opcuaReaderProvider;
            this.opcuaSubscriberProvider = opcuaSubscriberProvider;
        }

        public IReaderTask createReader(DataReadGroup readGroup) {
            Long intervalSeconds = readGroup.getInterval();
            EReadMode readMode = readGroup.getReadMode();
            switch (readMode) {
                case EReadMode.READ:
                    return new ReadTask(opcuaReaderProvider.get(), intervalSeconds, queue, null);
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

    public static class BatchBufferFactory implements IBatchBufferFactory {

        private final AppConfig infraConfig;

        public BatchBufferFactory(AppConfig infraConfig) {
            this.infraConfig = infraConfig;
        }

        public IBufferBuilder createBatchBuffer(DataWriteGroup group) {
            EDataType dataType = group.getDataType();
            int minBufferFlushSize = infraConfig.getMinBufferFlushSize();
            long minFlushIntervalNanos = infraConfig.getMinFlushIntervalNanos();

            if (dataType == EDataType.EVENTS)
                return new AcumJsonBufferBuilder("NameSource", minBufferFlushSize, minFlushIntervalNanos);

            IValueColumn valueColumn = createValueColumn(dataType);
            return new AcumBatchArrowBuilder(
                    infraConfig.getInitialBufferBuilderCapacity(),
                    infraConfig.isBufferCompressionEnabled(),
                    valueColumn,
                    minBufferFlushSize,
                    minFlushIntervalNanos);

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
