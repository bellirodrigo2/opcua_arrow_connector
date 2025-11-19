
package com.opcua_arrow.arrow.builders;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.IntVector;


public class IntIdColumn implements IIdColumn<Integer> {

    private IntVector vector;

    @Override
    public void bind(FieldVector vector) {
        this.vector = (IntVector) vector;
    }

    @Override
    public void set(int row, Integer value) {
        if (value == null) vector.setNull(row);
        else vector.setSafe(row, value);
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
}
