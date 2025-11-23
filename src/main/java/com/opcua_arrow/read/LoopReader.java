package com.opcua_arrow.read;

import java.util.Map;

import com.opcua_arrow.data_point.DataReadGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopReader implements IReader {

    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    private final Map<DataReadGroup, ReadTask> readersMap;

    public LoopReader(Map<DataReadGroup, ReadTask> readersMap) {
        this.readersMap = readersMap;
    }

    public void addNodeId(DataReadGroup readGroup, String nodeId) {
        ReadTask readTask = readersMap.get(readGroup);
        if (readTask != null) {
            readTask.addNodeId(nodeId);
            return;
        }
        logger.info("Creating new ReadTask for group: {}", readGroup);
        // Falta criar aqui
    }

    public void removeNodeId(DataReadGroup readGroup, String nodeId) {
        ReadTask readTask = readersMap.get(readGroup);
        if (readTask != null) {
            readTask.removeNodeId(nodeId);
            if (readTask.getNodeIds().isEmpty()) {
                readTask.stop();
                readersMap.remove(readGroup);
                logger.info("Removed ReadTask for group: {}", readGroup);
            }
        }
    }

    @Override
    public void read() {
        for (ReadTask readTask : readersMap.values()) {
            readTask.start();
        }
    }

    @Override
    public void stop() {
        for (ReadTask readTask : readersMap.values()) {
            readTask.stop();
        }
    }

}
