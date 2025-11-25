package com.opcua_arrow.maps;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.opcua_arrow.data.DataPointParams;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.di.FactoryModule.ReadTaskFactory;
import com.opcua_arrow.read.IReader;

/**
 * Thread-safe registry for managing IReader instances keyed by DataReadGroup.
 */
@Singleton
public class ReadTaskRegistry implements IRegistry<DataReadGroup, IReader> {

    private final ConcurrentHashMap<DataReadGroup, IReader> map = new ConcurrentHashMap<>();
    private final ReadTaskFactory factory;
    private final RunningState runningState;

    @Inject
    public ReadTaskRegistry(ReadTaskFactory factory, RunningState runningState) {
        this.factory = factory;
        this.runningState = runningState;
    }

    /**
     * Gets or creates an IReader for the given group, adding the dataPoint to it.
     * If a new reader is created and the application is running, the reader is
     * started.
     *
     * @param dataPointParam the data point parameters
     */
    public void getOrCreate(DataPointParams dataPointParam) {
        boolean[] created = { false };
        DataReadGroup group = dataPointParam.getReadGroup();
        IReader reader = map.computeIfAbsent(group, g -> {
            created[0] = true;
            return factory.createReader(group);
        });

        reader.addDataPoint(dataPointParam);

        if (created[0] && runningState.isRunning()) {
            reader.start();
        }
    }

    @Override
    public IReader get(DataReadGroup key) {
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
    public Collection<IReader> values() {
        return map.values();
    }
}
