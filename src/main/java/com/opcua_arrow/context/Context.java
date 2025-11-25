package com.opcua_arrow.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.registry.BufferRegistry;
import com.opcua_arrow.registry.ReadTaskRegistry;
import com.opcua_arrow.registry.RunningState;
import com.opcua_arrow.writer.IWriter;

public class Context {

    private final Map<String, DataPoint> paramsMap;
    private final IReader reader;
    private final IWriter writer;

    private final ReadTaskRegistry readTaskRegistry;
    private final BufferRegistry bufferRegistry;
    private final RunningState runningState;

    @Inject
    public Context(Map<String, DataPoint> paramsMap,
            IReader reader, IWriter writer,
            ReadTaskRegistry readTaskRegistry, BufferRegistry bufferRegistry,
            RunningState runningState) {

        this.paramsMap = ensureConcurrent(paramsMap);
        this.reader = reader;
        this.writer = writer;
        this.readTaskRegistry = readTaskRegistry;
        this.bufferRegistry = bufferRegistry;
        this.runningState = runningState;
    }

    private <K, V> Map<K, V> ensureConcurrent(Map<K, V> map) {
        return (map instanceof ConcurrentHashMap)
                ? map
                : new ConcurrentHashMap<>(map);
    }

    public void update(DataPoint params) {
        String nodeId = params.getNodeId();

        DataPoint existing = paramsMap.get(nodeId);
        DataWriteGroup newWriteGroup = params.getWriteGroup();
        DataReadGroup newReadGroup = params.getReadGroup();

        if (existing == null) {
            paramsMap.put(nodeId, params);
            bufferRegistry.getOrCreate(newWriteGroup);
            readTaskRegistry.getOrCreate(params);
            return;
        }

        DataWriteGroup existingWriteGroup = existing.getWriteGroup();
        if (!existingWriteGroup.equals(newWriteGroup)) {
            if (!bufferRegistry.containsKey(existingWriteGroup)) {
                bufferRegistry.getOrCreate(newWriteGroup);
            }
        }

        DataReadGroup existingReadGroup = existing.getReadGroup();
        if (!existingReadGroup.equals(newReadGroup)) {
            reader.addDataPoint(params);
            reader.removeDataPoint(existing);
        }
    }

    public void delete(String nodeId) {
        DataPoint existing = paramsMap.remove(nodeId);
        if (existing != null) {
            reader.removeDataPoint(existing);
            DataWriteGroup writeGroup = existing.getWriteGroup();
            if (!hasDataWriteGroup(writeGroup)) {
                bufferRegistry.remove(writeGroup);
            }
        }
    }

    private boolean hasDataWriteGroup(DataWriteGroup writeGroup) {
        for (DataPoint params : paramsMap.values()) {
            if (params.getWriteGroup().equals(writeGroup)) {
                return true;
            }
        }
        return false;
    }

    public void start() {
        writer.write();
        reader.start();
        runningState.setRunning(true);
    }

    public void stop() {
        reader.stop();
        writer.stop();
        runningState.setRunning(false);
    }
}
