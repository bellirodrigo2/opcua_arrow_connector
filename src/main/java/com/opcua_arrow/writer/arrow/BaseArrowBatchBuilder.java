package com.opcua_arrow.writer.arrow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import com.opcua_arrow.writer.IArrowBatchBuffer;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.TimeStampNanoTZVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.pojo.Schema;

public class BaseArrowBatchBuilder<T> implements IArrowBatchBuffer<T> {

    protected final BufferAllocator allocator;
    protected final Schema schema;
    protected final boolean compress;

    protected final IValueColumn<T> valueColumn;

    protected VectorSchemaRoot root;
    protected IntVector idVector;
    protected TimeStampNanoTZVector timestampVector;
    protected IntVector statusCodeVector;

    protected int capacity;
    protected int count;

    public BaseArrowBatchBuilder(
            Schema schema,
            int initialCapacity,
            BufferAllocator allocator,
            boolean compress,
            IValueColumn<T> valueColumn) {
        this.schema = schema;
        this.capacity = initialCapacity;
        this.allocator = allocator;
        this.compress = compress;
        this.valueColumn = valueColumn;
        allocateVectors(initialCapacity);
    }

    public BaseArrowBatchBuilder(
            int initialCapacity,
            boolean compress,
            IValueColumn<T> valueColumn) {
        this(SchemaUtils.createSchema(valueColumn.getClass()), initialCapacity,
                new RootAllocator(), compress, valueColumn);
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
        timestampVector = (TimeStampNanoTZVector) root.getVector("timestamp");
        valueColumn.bind(root.getVector("value"));
        statusCodeVector = (IntVector) root.getVector("statuscode");
        idVector = (IntVector) root.getVector("pointid");

        count = 0;
    }

    @SuppressWarnings("unchecked")
    protected <V extends FieldVector> V reallocVector(V oldVector, int newCapacity) {
        V newVector = (V) oldVector.getField().createVector(allocator);
        newVector.setInitialCapacity(newCapacity);
        newVector.allocateNew();

        boolean success = false;
        try {
            for (int i = 0; i < count; i++) {
                newVector.copyFromSafe(i, i, oldVector);
            }
            newVector.setValueCount(count);
            success = true;
        } finally {
            if (!success) {
                newVector.close();
            }
        }

        oldVector.close();
        return newVector;
    }

    protected void ensureCapacityLocked() {
        if (count < capacity)
            return;

        int newCapacity = capacity * 2;

        // ID e VALUE usam suas abstrações
        valueColumn.realloc(newCapacity, allocator);

        // timestamp e statuscode realocados diretamente
        idVector = reallocVector(idVector, newCapacity);
        timestampVector = reallocVector(timestampVector, newCapacity);
        statusCodeVector = reallocVector(statusCodeVector, newCapacity);

        // Recria o root com os novos vetores
        root = new VectorSchemaRoot(
                schema,
                List.of(
                        idVector, // "pointid"
                        timestampVector, // "timestamp"
                        valueColumn.getVector(), // "value"
                        statusCodeVector // "statuscode"
                ),
                count);

        capacity = newCapacity;
    }

    @Override
    public void append(int id, long timestampNanos, T value, int statusCode) {
        ensureCapacityLocked();

        int row = count;

        idVector.setSafe(row, id);
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
            return null;
        }
        try {
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
                return compress ? gzip(ipcBytes) : ipcBytes;

            } catch (IOException e) {
                throw new RuntimeException("Erro ao serializar Arrow batch para IPC stream", e);
            }

        } finally {
            count = 0;
            root.setRowCount(0);
        }
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
        try {
            if (root != null) {
                root.close();
            }
        } finally {
            root = null;
            allocator.close(); // sempre executa
        }
    }
}
