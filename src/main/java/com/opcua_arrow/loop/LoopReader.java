package com.opcua_arrow.loop;

import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.opcua_arrow.context.RunningState;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.di.FactoryModule.ReadTaskFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopReader implements ILoop {

    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    // private final ReadTaskRegistry readTaskRegistry;
    private final ConcurrentHashMap<DataReadGroup, IReader> readerMap = new ConcurrentHashMap<>();
    private final ReadTaskFactory factory;
    private final RunningState runningState;

    @Inject
    public LoopReader(ReadTaskFactory factory, RunningState runningState) {
        // this.readTaskRegistry = readTaskRegistry;
        this.factory = factory;
        this.runningState = runningState;
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        boolean[] created = { false };
        DataReadGroup group = dataPoint.getReadGroup();
        IReader reader = readerMap.computeIfAbsent(group, g -> {
            created[0] = true;
            return factory.createReader(group);
        });

        reader.addDataPoint(dataPoint);

        if (created[0] && runningState.isRunning()) {
            reader.start();
        }
    }

    @Override
    public void removeDataPoint(DataPoint DataPoint) {
        IReader readTask = readerMap.get(DataPoint.getReadGroup());
        if (readTask != null) {
            readTask.removeDataPoint(DataPoint);
            if (readTask.isEmpty()) {
                readTask.stop();
                readerMap.remove(DataPoint.getReadGroup());
                logger.info("Removed ReadTask for group: {}", DataPoint.getReadGroup());
            }
        }
    }

    @Override
    public void start() {
        for (IReader readTask : readerMap.values()) {
            readTask.start();
        }
    }

    @Override
    public void stop() {
        for (IReader readTask : readerMap.values()) {
            readTask.stop();
        }
    }
}
