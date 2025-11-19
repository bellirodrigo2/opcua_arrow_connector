package com.opcua_arrow.arrow.builders;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;

public interface IIdColumn<U> {
    void bind(FieldVector vector);
    void set(int row, U id);
    FieldVector realloc(int newCapacity, BufferAllocator alloc);
    FieldVector getVector();
}