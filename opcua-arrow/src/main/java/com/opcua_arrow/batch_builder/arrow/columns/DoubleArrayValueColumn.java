package com.opcua_arrow.batch_builder.arrow.columns;

import com.opcua_arrow.batch_builder.arrow.IValueColumn;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.impl.UnionListWriter;

/**
 * Versão simplificada para arrays de double compatível com Arrow 14.0.2
 */
public class DoubleArrayValueColumn implements IValueColumn {

    private ListVector vector;

    @Override
    public void bind(FieldVector vector) {
        this.vector = (ListVector) vector;
        // O ListVector já deve vir configurado com o schema correto
        // criado pelo BaseArrowBufferBuilder usando o Schema
    }

    @Override
    public void set(int row, Object value) {
        if (value == null) {
            // Marcar como null se não houver valor
            vector.setNull(row);
        } else {
            double[] array = (double[]) value;

            // Usar o writer para escrever a lista
            UnionListWriter writer = vector.getWriter();
            writer.setPosition(row);
            writer.startList();

            // Escrever cada elemento do array
            for (double d : array) {
                writer.float8().writeFloat8(d);
            }

            writer.endList();
        }
    }

    @Override
    public FieldVector realloc(int newCap, BufferAllocator alloc) {
        // Para realocar, criar um novo ListVector com a mesma estrutura
        ListVector newVector = ListVector.empty("value", alloc);

        // Copiar a estrutura de campos do vetor antigo
        newVector.initializeChildrenFromFields(vector.getField().getChildren());
        newVector.setInitialCapacity(newCap);
        newVector.allocateNew();

        // Copiar os dados existentes
        int count = vector.getValueCount();
        for (int i = 0; i < count; i++) {
            newVector.copyFromSafe(i, i, vector);
        }

        newVector.setValueCount(count);

        // Fechar o vetor antigo e substituir
        vector.close();
        vector = newVector;
        return newVector;
    }

    @Override
    public FieldVector getVector() {
        return vector;
    }

    @Override
    public Class<double[]> getValueClass() {
        return double[].class;
    }
}
