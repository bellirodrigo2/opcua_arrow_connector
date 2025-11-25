package com.opcua_arrow.read;

import com.opcua_arrow.data_point.DataPointParams;
import com.opcua_arrow.maps.ReadTaskRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopReader implements IReader {

    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    private final ReadTaskRegistry readTaskRegistry;

    public LoopReader(ReadTaskRegistry readTaskRegistry) {
        this.readTaskRegistry = readTaskRegistry;
    }

    public void addDataPoint(DataPointParams dataPointParams) {
        readTaskRegistry.getOrCreate(dataPointParams);
    }

    public void removeDataPoint(DataPointParams dataPointParams) {
        ReadTask readTask = readTaskRegistry.get(dataPointParams.getReadGroup());
        if (readTask != null) {
            readTask.removeDataPoint(dataPointParams);
            if (readTask.geDataPointParams().isEmpty()) {
                readTask.stop();
                readTaskRegistry.remove(dataPointParams.getReadGroup());
                logger.info("Removed ReadTask for group: {}", dataPointParams.getReadGroup());
            }
        }
    }

    @Override
    public void start() {
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
