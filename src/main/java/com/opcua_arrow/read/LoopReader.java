package com.opcua_arrow.read;

import java.util.List;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.registry.ReadTaskRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopReader implements IReader {

    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    private final ReadTaskRegistry readTaskRegistry;

    public LoopReader(ReadTaskRegistry readTaskRegistry) {
        this.readTaskRegistry = readTaskRegistry;
    }

    @Override
    public List<DataPoint> getDataPoint() {
        return readTaskRegistry.values().stream()
                .flatMap(reader -> reader.getDataPoint().stream())
                .toList();
    }

    @Override
    public void addDataPoint(DataPoint DataPoint) {
        readTaskRegistry.getOrCreate(DataPoint);
    }

    @Override
    public void removeDataPoint(DataPoint DataPoint) {
        IReader readTask = readTaskRegistry.get(DataPoint.getReadGroup());
        if (readTask != null) {
            readTask.removeDataPoint(DataPoint);
            if (readTask.getDataPoint().isEmpty()) {
                readTask.stop();
                readTaskRegistry.remove(DataPoint.getReadGroup());
                logger.info("Removed ReadTask for group: {}", DataPoint.getReadGroup());
            }
        }
    }

    @Override
    public void start() {
        for (IReader readTask : readTaskRegistry.values()) {
            readTask.start();
        }
    }

    @Override
    public void stop() {
        for (IReader readTask : readTaskRegistry.values()) {
            readTask.stop();
        }
    }
}
