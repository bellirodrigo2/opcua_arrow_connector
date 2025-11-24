package com.opcua_arrow.read;

import com.opcua_arrow.data_point.DataReadGroup;
import com.opcua_arrow.maps.ReadTaskRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopReader implements IReader {

    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    private final ReadTaskRegistry readTaskRegistry;

    public LoopReader(ReadTaskRegistry readTaskRegistry) {
        this.readTaskRegistry = readTaskRegistry;
    }

    public void addNodeId(DataReadGroup readGroup, String nodeId) {
        readTaskRegistry.getOrCreate(readGroup, nodeId);
    }

    public void removeNodeId(DataReadGroup readGroup, String nodeId) {
        ReadTask readTask = readTaskRegistry.get(readGroup);
        if (readTask != null) {
            readTask.removeNodeId(nodeId);
            if (readTask.getNodeIds().isEmpty()) {
                readTask.stop();
                readTaskRegistry.remove(readGroup);
                logger.info("Removed ReadTask for group: {}", readGroup);
            }
        }
    }

    @Override
    public void read() {
        for (ReadTask readTask : readTaskRegistry.values()) {
            readTask.start();
        }
    }

    @Override
    public void stop() {
        for (ReadTask readTask : readTaskRegistry.values()) {
            readTask.stop();
        }
    }
}
