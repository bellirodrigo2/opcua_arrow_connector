package com.opcua_arrow.writer.arrow;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;

public interface IValueColumn<T> {
    void bind(FieldVector vector);

    void set(int row, T value);

    FieldVector realloc(int newCapacity, BufferAllocator alloc);

    FieldVector getVector();

    Class<T> getValueClass();
}
