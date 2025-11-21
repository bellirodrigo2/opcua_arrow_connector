package com.opcua_arrow.writer.arrow.columns;

import com.opcua_arrow.writer.arrow.IValueColumn;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.IntVector;

public class IntegerValueColumn implements IValueColumn {

    private IntVector vector;

    @Override
    public void bind(FieldVector vector) {
        this.vector = (IntVector) vector;
    }

    @Override
    public void set(int row, Object value) {
        if (value == null)
            this.vector.setNull(row);
        else
            this.vector.setSafe(row, (Integer) value);
    }

    @Override
    public FieldVector realloc(int newCap, BufferAllocator alloc) {
        IntVector newVector = (IntVector) vector.getField().createVector(alloc);
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
    public Class<Integer> getValueClass() {
        return Integer.class;
    }
}
