package com.opcua_arrow.arrow;

import org.apache.arrow.vector.util.TransferPair;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.types.pojo.ArrowType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class ArrowBatchBuilders {

    public static abstract class BaseArrowBatchBuilder<T> implements AutoCloseable {
        protected final BufferAllocator allocator;
        protected final Schema schema;
        protected final boolean pointIdIsInt;
        protected final boolean compress;

        protected VectorSchemaRoot root;
        protected IntVector pointidInt;
        protected VarCharVector pointidStr;
        protected TimeStampNanoTZVector timestampVector;
        protected FieldVector valueVector;
        protected IntVector statusCodeVector;

        protected int capacity;
        protected int count;

        public BaseArrowBatchBuilder(Schema schema, int initialCapacity, BufferAllocator allocator, boolean compress) {
            this.schema = schema;
            this.capacity = initialCapacity;
            this.allocator = allocator;
            this.compress = compress;
            this.pointIdIsInt = schema.findField("pointid").getType() instanceof ArrowType.Int;
            allocateVectors(initialCapacity);
        }

        public BaseArrowBatchBuilder(Schema schema, int initialCapacity, boolean compress) {
            this(schema, initialCapacity, new RootAllocator(), compress);
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

            if (pointIdIsInt) {
                pointidInt = (IntVector) root.getVector("pointid");
            } else {
                pointidStr = (VarCharVector) root.getVector("pointid");
            }

            timestampVector = (TimeStampNanoTZVector) root.getVector("timestamp");
            valueVector = root.getVector("value");
            statusCodeVector = (IntVector) root.getVector("statuscode");

            count = 0;
        }

        protected void ensureCapacityLocked() {
            if (count < capacity) return;

            int newCapacity = capacity * 2;

            if (pointIdIsInt) {
                pointidInt = reallocVector(pointidInt, newCapacity);
            } else {
                pointidStr = reallocVector(pointidStr, newCapacity);
            }

            timestampVector = reallocVector(timestampVector, newCapacity);
            valueVector = reallocVector(valueVector, newCapacity);
            statusCodeVector = reallocVector(statusCodeVector, newCapacity);

            root = new VectorSchemaRoot(
                    schema,
                    List.of(
                            pointIdIsInt ? pointidInt : pointidStr,
                            timestampVector,
                            valueVector,
                            statusCodeVector
                    ),
                    count
            );

            capacity = newCapacity;
        }

        @SuppressWarnings("unchecked")
        protected <V extends FieldVector> V reallocVector(V oldVector, int newCapacity) {
            V newVector = (V) oldVector.getField().createVector(allocator);
            newVector.setInitialCapacity(newCapacity);
            newVector.allocateNew();

            TransferPair tp = oldVector.makeTransferPair(newVector);
            for (int i = 0; i < count; i++) {
                tp.copyValueSafe(i, i);
            }
            newVector.setValueCount(count);

            oldVector.close();
            return newVector;
        }

        public BaseArrowBatchBuilder<T> append(Integer pointid, long timestampNanos, T value, int statusCode) {
            ensureCapacityLocked();

            int row = count;

            if (pointIdIsInt) {
                if (pointid == null) {
                    pointidInt.setNull(row);
                } else {
                    pointidInt.setSafe(row, pointid);
                }
            } else {
                if (pointid == null) {
                    pointidStr.setNull(row);
                } else {
                    byte[] bytes = pointid.toString().getBytes();
                    pointidStr.setSafe(row, bytes);
                }
            }

            timestampVector.setSafe(row, timestampNanos);
            setTypedValue(valueVector, row, value);
            statusCodeVector.setSafe(row, statusCode);

            count++;
            return this;
        }

        protected abstract void setTypedValue(FieldVector vector, int index, T value);

        public BaseArrowBatchBuilder<T> pop() {
            if (count > 0) {
                count--;
            }
            return this;
        }

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

            count = 0;
            root.setRowCount(0);

            if (!compress) {
                return ipcBytes;
            } else {
                return gzip(ipcBytes);
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

        public int size() {
            return count;
        }

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

    public static class FloatArrowBatchBuilder extends BaseArrowBatchBuilder<Double> {
        public FloatArrowBatchBuilder(Schema schema, int initialCapacity, BufferAllocator allocator, boolean compress) {
            super(schema, initialCapacity, allocator, compress);
        }

        public FloatArrowBatchBuilder(Schema schema, int initialCapacity, boolean compress) {
            super(schema, initialCapacity, compress);
        }

        @Override
        protected void setTypedValue(FieldVector vector, int index, Double value) {
            if (value == null) {
                vector.setNull(index);
            } else {
                ((Float8Vector) vector).setSafe(index, value);
            }
        }
    }

    public static class IntArrowBatchBuilder extends BaseArrowBatchBuilder<Integer> {
        public IntArrowBatchBuilder(Schema schema, int initialCapacity, BufferAllocator allocator, boolean compress) {
            super(schema, initialCapacity, allocator, compress);
        }

        public IntArrowBatchBuilder(Schema schema, int initialCapacity, boolean compress) {
            super(schema, initialCapacity, compress);
        }

        @Override
        protected void setTypedValue(FieldVector vector, int index, Integer value) {
            if (value == null) {
                vector.setNull(index);
            } else {
                ((IntVector) vector).setSafe(index, value);
            }
        }
    }

    public static class BoolArrowBatchBuilder extends BaseArrowBatchBuilder<Boolean> {
        public BoolArrowBatchBuilder(Schema schema, int initialCapacity, BufferAllocator allocator, boolean compress) {
            super(schema, initialCapacity, allocator, compress);
        }

        public BoolArrowBatchBuilder(Schema schema, int initialCapacity, boolean compress) {
            super(schema, initialCapacity, compress);
        }

        @Override
        protected void setTypedValue(FieldVector vector, int index, Boolean value) {
            if (value == null) {
                vector.setNull(index);
            } else {
                ((BitVector) vector).setSafe(index, value ? 1 : 0);
            }
        }
    }

    public static class StringArrowBatchBuilder extends BaseArrowBatchBuilder<String> {
        public StringArrowBatchBuilder(Schema schema, int initialCapacity, BufferAllocator allocator, boolean compress) {
            super(schema, initialCapacity, allocator, compress);
        }

        public StringArrowBatchBuilder(Schema schema, int initialCapacity, boolean compress) {
            super(schema, initialCapacity, compress);
        }

        @Override
        protected void setTypedValue(FieldVector vector, int index, String value) {
            if (value == null) {
                vector.setNull(index);
            } else {
                ((VarCharVector) vector).setSafe(index, value.getBytes());
            }
        }
    }
}