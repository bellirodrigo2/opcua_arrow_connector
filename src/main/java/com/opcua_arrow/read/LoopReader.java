package com.opcua_arrow.read;

import java.util.List;

import com.opcua_arrow.data.DataPointParams;
import com.opcua_arrow.maps.ReadTaskRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopReader implements IReader {

    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    private final ReadTaskRegistry readTaskRegistry;

    public LoopReader(ReadTaskRegistry readTaskRegistry) {
        this.readTaskRegistry = readTaskRegistry;
    }

    @Override
    public List<DataPointParams> getDataPointParams() {
        return readTaskRegistry.values().stream()
                .flatMap(reader -> reader.getDataPointParams().stream())
                .toList();
    }

    @Override
    public void addDataPoint(DataPointParams dataPointParams) {
        readTaskRegistry.getOrCreate(dataPointParams);
    }

    @Override
    public void removeDataPoint(DataPointParams dataPointParams) {
        IReader readTask = readTaskRegistry.get(dataPointParams.getReadGroup());
        if (readTask != null) {
            readTask.removeDataPoint(dataPointParams);
            if (readTask.getDataPointParams().isEmpty()) {
                readTask.stop();
                readTaskRegistry.remove(dataPointParams.getReadGroup());
                logger.info("Removed ReadTask for group: {}", dataPointParams.getReadGroup());
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
