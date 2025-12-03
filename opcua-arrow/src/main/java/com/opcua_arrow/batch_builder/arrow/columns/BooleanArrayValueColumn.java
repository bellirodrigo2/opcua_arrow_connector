package com.opcua_arrow.batch_builder.arrow.columns;

import com.opcua_arrow.batch_builder.arrow.IValueColumn;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.impl.UnionListWriter;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;

public class BooleanArrayValueColumn implements IValueColumn {

    private ListVector vector;

    @Override
    public void bind(FieldVector vector) {
        this.vector = (ListVector) vector;

        // Para Arrow 14.x, o campo filho já deve estar configurado no schema
        // Se precisar inicializar, use:
        if (this.vector.getDataVector() == null) {
            FieldType childFieldType = new FieldType(true, ArrowType.Bool.INSTANCE, null);
            this.vector.initializeChildrenFromFields(
                    java.util.Collections.singletonList(
                            new Field("element", childFieldType, null)));
        }
    }

    @Override
    public void set(int row, Object value) {
        if (value == null) {
            vector.setNull(row);
        } else {
            boolean[] array = (boolean[]) value;
            UnionListWriter writer = vector.getWriter();
            writer.setPosition(row);
            writer.startList();

            for (boolean b : array) {
                writer.bit().writeBit(b ? 1 : 0);
            }

            writer.endList();
        }
    }

    @Override
    public FieldVector realloc(int newCap, BufferAllocator alloc) {
        // Criar novo vetor com schema apropriado
        Field elementField = Field.nullable("element", ArrowType.Bool.INSTANCE);
        Field listField = new Field("value",
                new FieldType(true, new ArrowType.List(), null),
                java.util.Collections.singletonList(elementField));

        ListVector newVector = (ListVector) listField.createVector(alloc);
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
    public Class<boolean[]> getValueClass() {
        return boolean[].class;
    }
}
