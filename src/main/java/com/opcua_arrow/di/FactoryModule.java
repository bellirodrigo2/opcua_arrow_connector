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
import com.opcua_arrow.batch_builder.arrow.columns.BooleanValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.DoubleValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.IntegerValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.StringValueColumn;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.InfraConfig;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.read.ReadTask;

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
            IQueue<List<IOPCUADataValue>> queue,
            Provider<IOPCUAReader> opcuaReaderProvider) {
        return new ReadTaskFactory(queue, opcuaReaderProvider);
    }

    @Provides
    @Singleton
    public BatchBufferFactory provideBatchBufferFactory(ConfigProvider configProvider) {
        return new BatchBufferFactory(configProvider.getInfraConfig());
    }

    public static class ReadTaskFactory {
        private final IQueue<List<IOPCUADataValue>> queue;
        private final Provider<IOPCUAReader> opcuaReaderProvider;

        @Inject
        public ReadTaskFactory(
                IQueue<List<IOPCUADataValue>> queue,
                Provider<IOPCUAReader> opcuaReaderProvider) {
            this.queue = queue;
            this.opcuaReaderProvider = opcuaReaderProvider;
        }

        public ReadTask createReadTask(Long intervalSeconds) {
            IOPCUAReader opcuaReader = opcuaReaderProvider.get();
            return new ReadTask(opcuaReader, intervalSeconds, queue);
        }
    }

    public static class BatchBufferFactory {

        private final InfraConfig infraConfig;

        public BatchBufferFactory(InfraConfig infraConfig) {
            this.infraConfig = infraConfig;
        }

        public IBufferBuilder createBatchBuffer(DataWriteGroup group) {
            Class<?> valueType = group.getValueType();
            IValueColumn valueColumn = createValueColumn(valueType);
            return new AcumBatchArrowBuilder(
                    infraConfig.getInitialBufferBuilderCapacity(),
                    infraConfig.isBufferCompressionEnabled(),
                    valueColumn,
                    infraConfig.getMinBufferFlushSize(),
                    infraConfig.getMinFlushIntervalNanos());
        }

        private IValueColumn createValueColumn(Class<?> valueType) {
            if (valueType == Double.class || valueType == double.class) {
                return new DoubleValueColumn();
            } else if (valueType == Integer.class || valueType == int.class) {
                return new IntegerValueColumn();
            } else if (valueType == Boolean.class || valueType == boolean.class) {
                return new BooleanValueColumn();
            } else if (valueType == String.class) {
                return new StringValueColumn();
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + valueType);
            }
        }
    }
}
