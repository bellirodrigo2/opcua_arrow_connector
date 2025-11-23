package com.opcua_arrow.batch_builder.arrow;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;

public interface IValueColumn {
    void bind(FieldVector vector);

    void set(int row, Object value);

    FieldVector realloc(int newCapacity, BufferAllocator alloc);

    FieldVector getVector();

    Class<?> getValueClass();
}
