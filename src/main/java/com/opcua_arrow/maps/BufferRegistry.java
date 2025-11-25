package com.opcua_arrow.maps;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.di.FactoryModule.BatchBufferFactory;

/**
 * Thread-safe registry for managing IBufferBuilder instances keyed by
 * DataWriteGroup.
 */
@Singleton
public class BufferRegistry implements IRegistry<DataWriteGroup, IBufferBuilder> {

    private final ConcurrentHashMap<DataWriteGroup, IBufferBuilder> map = new ConcurrentHashMap<>();
    private final BatchBufferFactory factory;

    @Inject
    public BufferRegistry(BatchBufferFactory factory) {
        this.factory = factory;
    }

    /**
     * Ensures an IBufferBuilder exists for the given group, creating one if absent.
     *
     * @param group the write group key
     */
    public void getOrCreate(DataWriteGroup group) {
        map.computeIfAbsent(group, factory::createBatchBuffer);
    }

    @Override
    public IBufferBuilder get(DataWriteGroup key) {
        return map.get(key);
    }

    @Override
    public void remove(DataWriteGroup key) {
        map.remove(key);
    }

    @Override
    public boolean containsKey(DataWriteGroup key) {
        return map.containsKey(key);
    }

    @Override
    public Collection<IBufferBuilder> values() {
        return map.values();
    }
}
