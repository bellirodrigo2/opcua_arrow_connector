package com.opcua_arrow.batch_builder.arrow.columns;

import com.opcua_arrow.batch_builder.arrow.IValueColumn;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float8Vector;

public class DoubleValueColumn implements IValueColumn {

    private Float8Vector vector;

    @Override
    public void bind(FieldVector vector) {
        this.vector = (Float8Vector) vector;
    }

    @Override
    public void set(int row, Object value) {
        if (value == null)
            vector.setNull(row);
        else
            vector.setSafe(row, (Double) value);
    }

    @Override
    public FieldVector realloc(int newCap, BufferAllocator alloc) {
        Float8Vector newVector = (Float8Vector) vector.getField().createVector(alloc);
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
    public Class<Double> getValueClass() {
        return Double.class;
    }
}
