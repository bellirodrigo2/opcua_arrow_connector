package com.opcua_arrow.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.opcua_arrow.batch_builder.IArrowBatchBuffer;
import com.opcua_arrow.data_point.DataReadGroup;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.opcua.DataPointParams;
import com.opcua_arrow.di.FactoryModule.BatchBufferFactory;
import com.opcua_arrow.di.FactoryModule.ReadTaskFactory;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.read.ReadTask;
import com.opcua_arrow.transform.ITransform;
import com.opcua_arrow.writer.IWriter;

public class Context {

    private final Map<String, DataPointParams> paramsMap;
    private final IReader reader;
    private final IWriter writer;
    private final ITransform transform;

    // Factories injetadas diretamente
    private final ReadTaskFactory readTaskFactory;
    private final BatchBufferFactory batchBufferFactory;

    // Mapas compartilhados injetados
    private final Map<DataReadGroup, ReadTask> readersMap;
    private final Map<DataWriteGroup, IArrowBatchBuffer> batchBuffers;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Inject
    public Context(Map<String, DataPointParams> paramsMap,
            IReader reader, IWriter writer, ITransform transform,
            ReadTaskFactory readTaskFactory, BatchBufferFactory batchBufferFactory,
            Map<DataReadGroup, ReadTask> readersMap,
            Map<DataWriteGroup, IArrowBatchBuffer> batchBuffers) {

        this.paramsMap = ensureConcurrent(paramsMap);
        this.reader = reader;
        this.writer = writer;
        this.transform = transform;
        this.readTaskFactory = readTaskFactory;
        this.batchBufferFactory = batchBufferFactory;
        this.readersMap = readersMap;
        this.batchBuffers = batchBuffers;
    }

    private <K, V> Map<K, V> ensureConcurrent(Map<K, V> map) {
        return (map instanceof ConcurrentHashMap)
                ? map
                : new ConcurrentHashMap<>(map);
    }

    public void delete(String nodeId) {
        DataPointParams existing = paramsMap.remove(nodeId);
        if (existing != null) {
            DataReadGroup readGroup = existing.getReadGroup();
            reader.removeNodeId(readGroup, nodeId);
            DataWriteGroup writeGroup = existing.getWriteGroup();
            if (!hasDataWriteGroup(writeGroup)) {
                batchBuffers.remove(writeGroup);
            }
        }
    }

    private boolean hasDataWriteGroup(DataWriteGroup writeGroup) {
        for (DataPointParams params : paramsMap.values()) {
            if (params.getWriteGroup().equals(writeGroup)) {
                return true;
            }
        }
        return false;
    }

    public void update(DataPointParams params) {
        String nodeId = params.getNodeId();

        DataPointParams existing = paramsMap.get(nodeId);
        DataWriteGroup newWriteGroup = params.getWriteGroup();
        DataReadGroup newReadGroup = params.getReadGroup();

        if (existing == null) {
            paramsMap.put(nodeId, params);

            writerFactory(newWriteGroup); // Implementado diretamente
            readerFactory(newReadGroup, nodeId); // Implementado diretamente
            return;
        }
        DataWriteGroup existingWriteGroup = existing.getWriteGroup();
        if (!existingWriteGroup.equals(newWriteGroup)) {
            if (!batchBuffers.containsKey(existingWriteGroup)) {
                writerFactory(newWriteGroup); // Implementado diretamente
            }
        }

        DataReadGroup existingReadGroup = existing.getReadGroup();
        if (!existingReadGroup.equals(newReadGroup)) {
            reader.addNodeId(newReadGroup, nodeId);
            reader.removeNodeId(existingReadGroup, nodeId);
            // se o READER FICAR VAZIO, remove
        }
    }

    public void start() {
        writer.write();
        transform.transform();
        reader.read();
        running.set(true);

    }

    public void stop() {
        reader.stop();
        writer.stop();
        running.set(false);
    }

    /**
     * Factory para criar ReadTask - implementa o que estava faltando
     */
    private void readerFactory(DataReadGroup readGroup, String nodeId) {
        if (!readersMap.containsKey(readGroup)) {
            Long intervalSeconds = readGroup.getInterval();
            ReadTask readTask = readTaskFactory.createReadTask(intervalSeconds);
            readTask.addNodeId(nodeId);
            readersMap.put(readGroup, readTask);
            if (running.get()) {
                readTask.start();
            }
        } else {
            readersMap.get(readGroup).addNodeId(nodeId);
        }
    }

    /**
     * Factory para criar BatchBuffer - implementa o que estava faltando
     */
    private void writerFactory(DataWriteGroup writeGroup) {
        if (!batchBuffers.containsKey(writeGroup)) {
            IArrowBatchBuffer batchBuffer = batchBufferFactory.createBatchBuffer(writeGroup);
            batchBuffers.put(writeGroup, batchBuffer);
        }
    }
}
