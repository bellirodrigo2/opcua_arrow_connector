package com.opcua_arrow.arrow.builders;

import com.opcua_arrow.arrow.IArrowBatchBuffer;
import com.opcua_arrow.arrow.SchemaUtils;


import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.TimeStampNanoTZVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.pojo.Schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class BaseArrowBatchBuilder<TId, TValue> implements IArrowBatchBuffer<TId, TValue> {

    protected final BufferAllocator allocator;
    protected final Schema schema;
    protected final boolean compress;

    protected final IIdColumn<TId> idColumn;
    protected final IValueColumn<TValue> valueColumn;

    protected VectorSchemaRoot root;
    protected TimeStampNanoTZVector timestampVector;
    protected IntVector statusCodeVector;

    protected int capacity;
    protected int count;

    public BaseArrowBatchBuilder(
            Schema schema,
            int initialCapacity,
            BufferAllocator allocator,
            boolean compress,
            IIdColumn<TId> idColumn,
            IValueColumn<TValue> valueColumn
    ) {
        this.schema = schema;
        this.capacity = initialCapacity;
        this.allocator = allocator;
        this.compress = compress;
        this.idColumn = idColumn;
        this.valueColumn = valueColumn;
        allocateVectors(initialCapacity);
    }

    public BaseArrowBatchBuilder(
            int initialCapacity,
            boolean compress,
            IIdColumn<TId> idColumn,
            IValueColumn<TValue> valueColumn
    ) {
        this(SchemaUtils.createSchema(valueColumn.getClass(),idColumn.getClass()), initialCapacity, new RootAllocator(), compress, idColumn, valueColumn);
    }

    @Override
    public Class<TValue> getValueClass() {
        return valueColumn.getValueClass();
    }

    protected void allocateVectors(int initialCapacity) {
        List<FieldVector> vectors = schema.getFields()
                .stream()
                .map(f -> {
                    FieldVector v = (FieldVector) f.createVector(allocator);
                    v.setInitialCapacity(initialCapacity);
                    v.allocateNew();
                    return v;
                })
                .collect(Collectors.toList());

        root = new VectorSchemaRoot(schema.getFields(), vectors, 0);

        // bind nas colunas específicas pelo nome (pelo seu SchemaUtils)
        idColumn.bind(root.getVector("pointid"));
        timestampVector = (TimeStampNanoTZVector) root.getVector("timestamp");
        valueColumn.bind(root.getVector("value"));
        statusCodeVector = (IntVector) root.getVector("statuscode");

        count = 0;
    }

    @SuppressWarnings("unchecked")
    protected <V extends FieldVector> V reallocVector(V oldVector, int newCapacity) {
        V newVector = (V) oldVector.getField().createVector(allocator);
        newVector.setInitialCapacity(newCapacity);
        newVector.allocateNew();

        // copia apenas as linhas válidas (0..count-1)
        for (int i = 0; i < count; i++) {
            newVector.copyFromSafe(i, i, oldVector);
        }
        newVector.setValueCount(count);

        oldVector.close();
        return newVector;
    }

    protected void ensureCapacityLocked() {
        if (count < capacity) return;

        int newCapacity = capacity * 2;

        // ID e VALUE usam suas abstrações
        idColumn.realloc(newCapacity, allocator);
        valueColumn.realloc(newCapacity, allocator);

        // timestamp e statuscode realocados diretamente
        timestampVector = reallocVector(timestampVector, newCapacity);
        statusCodeVector = reallocVector(statusCodeVector, newCapacity);

        // Recria o root com os novos vetores
        root = new VectorSchemaRoot(
                schema,
                List.of(
                        idColumn.getVector(),      // "pointid"
                        timestampVector,           // "timestamp"
                        valueColumn.getVector(),   // "value"
                        statusCodeVector           // "statuscode"
                ),
                count
        );

        capacity = newCapacity;
    }

    @Override
    public void append(TId id, long timestampNanos, TValue value, int statusCode) {
        ensureCapacityLocked();

        int row = count;

        idColumn.set(row, id);
        timestampVector.setSafe(row, timestampNanos);
        valueColumn.set(row, value);
        statusCodeVector.setSafe(row, statusCode);

        count++;
    }

    @Override
    public void pop() {
        if (count > 0) {
            count--;
        }
    }

    @Override
    public void reset() {
        count = 0;
        if (root != null) {
            root.setRowCount(0);
        }
    }

    @Override
    public byte[] flush() {
        if (count == 0) {
            return new byte[0];
        }

        root.setRowCount(count);

        byte[] ipcBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ArrowStreamWriter writer = new ArrowStreamWriter(
                     root,
                     null,
                     Channels.newChannel(baos))) {

            writer.start();
            writer.writeBatch();
            writer.end();

            ipcBytes = baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao serializar Arrow batch para IPC stream", e);
        }

        // após flush, apenas zeramos o contador (como você pediu)
        count = 0;
        root.setRowCount(0);

        return compress ? gzip(ipcBytes) : ipcBytes;
    }

    protected byte[] gzip(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
            gzip.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao comprimir com GZIP", e);
        }
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void close() {
        if (root != null) {
            root.close();
            root = null;
        }
        allocator.close();
    }
}

