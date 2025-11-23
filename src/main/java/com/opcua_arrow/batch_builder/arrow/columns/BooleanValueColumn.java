package com.opcua_arrow.batch_builder.arrow.columns;

import com.opcua_arrow.batch_builder.arrow.IValueColumn;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;

public class BooleanValueColumn implements IValueColumn {

    private BitVector vector;

    @Override
    public void bind(FieldVector vector) {
        this.vector = (BitVector) vector;
    }

    @Override
    public void set(int row, Object value) {
        if (value == null)
            vector.setNull(row);
        else
            vector.setSafe(row, (Boolean) value ? 1 : 0);
    }

    @Override
    public FieldVector realloc(int newCap, BufferAllocator alloc) {
        BitVector newVector = (BitVector) vector.getField().createVector(alloc);
        newVector.setInitialCapacity(newCap);
        newVector.allocateNew();

        int count = vector.getValueCount();
        for (int i = 0; i < count; i++) {
            newVector.copyFromSafe(i, i, vector);
        }

        newVector.setValueCount(count);
        vector.close();
        vector = newVector;
        return newVector;
    }

    @Override
    public FieldVector getVector() {
        return vector;
    }

    @Override
    public Class<Boolean> getValueClass() {
        return Boolean.class;
    }

}
