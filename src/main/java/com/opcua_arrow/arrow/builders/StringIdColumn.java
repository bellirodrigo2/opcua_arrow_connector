
package com.opcua_arrow.arrow.builders;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VarCharVector;

public class StringIdColumn implements IIdColumn<String> {

    private VarCharVector vector;

    @Override
    public void bind(FieldVector vector) {
        this.vector = (VarCharVector) vector;
    }

    @Override
    public void set(int row, String value) {
        if (value == null) vector.setNull(row);
        else vector.setSafe(row, value.getBytes());
    }

    @Override
    public FieldVector realloc(int newCap, BufferAllocator alloc) {
        VarCharVector newVector = (VarCharVector) vector.getField().createVector(alloc);
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
}
