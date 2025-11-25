package com.opcua_arrow.maps.v2;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.opcua_arrow.data_point.DataReadGroup;
import com.opcua_arrow.data_point.equals2.DataPointParams;
import com.opcua_arrow.di.FactoryModule2.ReadTaskFactory;
import com.opcua_arrow.maps.IRegistry;
import com.opcua_arrow.maps.RunningState;
import com.opcua_arrow.read.v2.ReadTask;

/**
 * Thread-safe registry for managing ReadTask instances keyed by DataReadGroup.
 */
@Singleton
public class ReadTaskRegistry implements IRegistry<DataReadGroup, ReadTask> {

    private final ConcurrentHashMap<DataReadGroup, ReadTask> map = new ConcurrentHashMap<>();
    private final ReadTaskFactory factory;
    private final RunningState runningState;

    @Inject
    public ReadTaskRegistry(ReadTaskFactory factory, RunningState runningState) {
        this.factory = factory;
        this.runningState = runningState;
    }

    /**
     * Gets or creates a ReadTask for the given group, adding the nodeId to it.
     * If a new task is created and the application is running, the task is started.
     *
     * @param group  the read group key
     * @param nodeId the node ID to add to the task
     */
    public void getOrCreate(DataPointParams dataPointParam) {
        boolean[] created = { false };
        DataReadGroup group = dataPointParam.getReadGroup();
        ReadTask task = map.computeIfAbsent(group, g -> {
            created[0] = true;
            return factory.createReadTask(group);
        });

        task.addDataPoint(dataPointParam);

        if (created[0] && runningState.isRunning()) {
            task.start();
        }
    }

    @Override
    public ReadTask get(DataReadGroup key) {
        return map.get(key);
    }

    @Override
    public void remove(DataReadGroup key) {
        map.remove(key);
    }

    @Override
    public boolean containsKey(DataReadGroup key) {
        return map.containsKey(key);
    }

    @Override
    public Collection<ReadTask> values() {
        return map.values();
    }
}
