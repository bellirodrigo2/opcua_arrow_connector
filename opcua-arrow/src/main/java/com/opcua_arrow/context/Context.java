package com.opcua_arrow.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.opcua_arrow.IContext;
import com.opcua_arrow.RunningState;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.loop.ILoop;

@Singleton
public class Context implements IContext {

    private final Map<String, DataPoint> paramsMap;
    private final ILoop reader;
    private final ILoop writer;

    private final RunningState runningState;

    @Inject
    public Context(Map<String, DataPoint> paramsMap,
            @Named("reader") ILoop reader,
            @Named("writer") ILoop writer,
            RunningState runningState) {

        this.paramsMap = ensureConcurrent(paramsMap);
        this.reader = reader;
        this.writer = writer;
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
            writer.addDataPoint(params);
            reader.addDataPoint(params);
            return;
        }

        DataWriteGroup existingWriteGroup = existing.getWriteGroup();
        if (!existingWriteGroup.equals(newWriteGroup)) {
            if (existingWriteGroup.getDataType() != newWriteGroup.getDataType())
                throw new IllegalArgumentException("Cannot change data type of existing DataWriteGroup");

            writer.addDataPoint(params);
            if (DataWriteGroupCount(existingWriteGroup) == 1) {
                writer.removeDataPoint(existing);
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
            if (!hasDataWriteGroup(writeGroup))
                writer.removeDataPoint(existing);
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

    private int DataWriteGroupCount(DataWriteGroup writeGroup) {
        int count = 0;
        for (DataPoint params : paramsMap.values()) {
            if (params.getWriteGroup().equals(writeGroup)) {
                count++;
            }
        }
        return count;
    }

    public void start() {
        writer.start();
        reader.start();
        runningState.setRunning(true);
    }

    public void stop() {
        reader.stop();
        writer.stop();
        runningState.setRunning(false);
    }
}
