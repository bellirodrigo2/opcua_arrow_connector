package com.opcua_arrow.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.opcua_arrow.data_point.DataReadGroup;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.equals2.DataPointParams;
import com.opcua_arrow.maps.RunningState;
import com.opcua_arrow.maps.v2.BufferRegistry;
import com.opcua_arrow.maps.v2.ReadTaskRegistry;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.transform.ITransform;
import com.opcua_arrow.writer.IWriter;

public class Context2 {

    private final Map<String, DataPointParams> paramsMap;
    private final IReader reader;
    private final IWriter writer;
    private final ITransform transform;

    private final ReadTaskRegistry readTaskRegistry;
    private final BufferRegistry bufferRegistry;
    private final RunningState runningState;

    @Inject
    public Context2(Map<String, DataPointParams> paramsMap,
            IReader reader, IWriter writer, ITransform transform,
            ReadTaskRegistry readTaskRegistry, BufferRegistry bufferRegistry,
            RunningState runningState) {

        this.paramsMap = ensureConcurrent(paramsMap);
        this.reader = reader;
        this.writer = writer;
        this.transform = transform;
        this.readTaskRegistry = readTaskRegistry;
        this.bufferRegistry = bufferRegistry;
        this.runningState = runningState;
    }

    private <K, V> Map<K, V> ensureConcurrent(Map<K, V> map) {
        return (map instanceof ConcurrentHashMap)
                ? map
                : new ConcurrentHashMap<>(map);
    }

    public void update(DataPointParams params) {
        String nodeId = params.getNodeId();

        DataPointParams existing = paramsMap.get(nodeId);
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
            reader.addNodeId(newReadGroup, nodeId);
            reader.removeNodeId(existingReadGroup, nodeId);
        }
    }

    public void delete(String nodeId) {
        DataPointParams existing = paramsMap.remove(nodeId);
        if (existing != null) {
            DataReadGroup readGroup = existing.getReadGroup();
            reader.removeNodeId(readGroup, nodeId);
            DataWriteGroup writeGroup = existing.getWriteGroup();
            if (!hasDataWriteGroup(writeGroup)) {
                bufferRegistry.remove(writeGroup);
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

    public void start() {
        writer.write();
        transform.transform();
        reader.read();
        runningState.setRunning(true);
    }

    public void stop() {
        reader.stop();
        writer.stop();
        runningState.setRunning(false);
    }
}
