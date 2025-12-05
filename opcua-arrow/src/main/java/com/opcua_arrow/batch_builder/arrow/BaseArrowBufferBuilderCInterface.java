// package com.opcua_arrow.batch_builder.arrow;

// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.nio.channels.Channels;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;
// import java.util.zip.GZIPOutputStream;

// import com.opcua_arrow.batch_builder.IBufferBuilder;
// import com.opcua_arrow.data.TSValue;

// import org.apache.arrow.c.ArrowArray;
// import org.apache.arrow.c.ArrowSchema;
// import org.apache.arrow.c.Data;
// import org.apache.arrow.memory.BufferAllocator;
// import org.apache.arrow.memory.RootAllocator;
// import org.apache.arrow.vector.BitVector;
// import org.apache.arrow.vector.FieldVector;
// import org.apache.arrow.vector.IntVector;
// import org.apache.arrow.vector.TimeStampNanoTZVector;
// import org.apache.arrow.vector.VectorSchemaRoot;
// import org.apache.arrow.vector.ipc.ArrowStreamWriter;
// import org.apache.arrow.vector.types.pojo.Schema;

// /**
// * Refatoração com Arrow C Data Interface para zero-copy
// *
// * Principais mudanças:
// * 1. flush() retorna ArrowBatch ao invés de byte[]
// * 2. Zero-copy transfer via C Data Interface
// * 3. Opcional: Queue interna para desacoplamento
// */
// public class BaseArrowBufferBuilderCInterface implements IBufferBuilder {

// /**
// * Classe para encapsular um batch Arrow com zero-copy
// * Esta classe mantém ownership dos recursos até ser consumida
// */
// public static class ArrowBatch implements AutoCloseable {
// private final ArrowArray array;
// private final ArrowSchema schema;
// private final int rowCount;
// private final long createdNanos;
// private volatile boolean consumed = false;

// public ArrowBatch(ArrowArray array, ArrowSchema schema, int rowCount) {
// this.array = array;
// this.schema = schema;
// this.rowCount = rowCount;
// this.createdNanos = System.nanoTime();
// }

// public VectorSchemaRoot importToRoot(BufferAllocator allocator) {
// if (consumed) {
// throw new IllegalStateException("Batch already consumed");
// }

// try {
// // Zero-copy import - apenas mapeia a memória existente
// VectorSchemaRoot root = Data.importVectorSchemaRoot(
// allocator,
// array,
// schema,
// null);
// consumed = true;
// return root;
// } finally {
// // Fecha os recursos C após importar
// close();
// }
// }

// protected byte[] gzip(byte[] data) {
// try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
// GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
// gzip.write(data);
// gzip.finish();
// return baos.toByteArray();
// } catch (IOException e) {
// throw new RuntimeException("Erro ao comprimir com GZIP", e);
// }
// }

// public byte[] serialize(BufferAllocator allocator, boolean compress) throws
// IOException {
// if (consumed) {
// throw new IllegalStateException("Batch already consumed");
// }

// // Primeiro importa com zero-copy local
// try (VectorSchemaRoot root = Data.importVectorSchemaRoot(
// allocator, array, schema, null)) {

// // Serializa para Arrow IPC Stream format
// try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

// // Arrow Stream Writer para serialização
// try (ArrowStreamWriter writer = new ArrowStreamWriter(
// root,
// null,
// Channels.newChannel(baos))) {

// writer.start();
// writer.writeBatch();
// writer.end();
// }

// byte[] serialized = baos.toByteArray();

// // Opcionalmente comprime
// if (compress) {
// return gzip(serialized);
// }

// return serialized;
// }
// } finally {
// consumed = true;
// close();
// }
// }

// public int getRowCount() {
// return rowCount;
// }

// public long getLatencyNanos() {
// return System.nanoTime() - createdNanos;
// }

// public boolean isConsumed() {
// return consumed;
// }

// @Override
// public void close() {
// if (array != null)
// array.close();
// if (schema != null)
// schema.close();
// }
// }

// // Campos originais mantidos
// protected final BufferAllocator allocator;
// protected final Schema schema;
// protected final IValueColumn valueColumn;

// protected VectorSchemaRoot root;
// protected IntVector idVector;
// protected TimeStampNanoTZVector timestampVector;
// protected BitVector statusCodeVector;

// protected int capacity;
// protected int count;
// boolean compress;

// public BaseArrowBufferBuilderCInterface(
// Schema schema,
// int initialCapacity,
// BufferAllocator allocator,
// IValueColumn valueColumn, boolean compress) {
// this.schema = schema;
// this.capacity = initialCapacity;
// this.allocator = allocator;
// this.valueColumn = valueColumn;
// this.compress = compress;
// allocateVectors(initialCapacity);
// }

// public BaseArrowBufferBuilderCInterface(
// int initialCapacity,
// IValueColumn valueColumn,
// Map<String, String> metadata, boolean compress) {
// this(SchemaUtils.createSchema(valueColumn.getClass(), metadata),
// initialCapacity,
// new RootAllocator(),
// valueColumn, compress);
// }

// protected void allocateVectors(int initialCapacity) {
// List<FieldVector> vectors = schema.getFields()
// .stream()
// .map(f -> {
// FieldVector v = (FieldVector) f.createVector(allocator);
// v.setInitialCapacity(initialCapacity);
// v.allocateNew();
// return v;
// })
// .collect(Collectors.toList());

// root = new VectorSchemaRoot(schema.getFields(), vectors, 0);

// timestampVector = (TimeStampNanoTZVector) root.getVector("timestamp");
// valueColumn.bind(root.getVector("value"));
// statusCodeVector = (BitVector) root.getVector("statuscode");
// idVector = (IntVector) root.getVector("pointid");

// count = 0;
// }

// @SuppressWarnings("unchecked")
// protected <V extends FieldVector> V reallocVector(V oldVector, int
// newCapacity) {
// V newVector = (V) oldVector.getField().createVector(allocator);
// newVector.setInitialCapacity(newCapacity);
// newVector.allocateNew();

// boolean success = false;
// try {
// for (int i = 0; i < count; i++) {
// newVector.copyFromSafe(i, i, oldVector);
// }
// newVector.setValueCount(count);
// success = true;
// } finally {
// if (!success) {
// newVector.close();
// }
// }

// oldVector.close();
// return newVector;
// }

// protected void ensureCapacityLocked(int additionalCount) {
// int finalCapacity = count + additionalCount;
// if (finalCapacity < capacity)
// return;

// int newCapacity = capacity * 2;
// if (newCapacity < finalCapacity) {
// newCapacity = finalCapacity;
// }

// valueColumn.realloc(newCapacity, allocator);
// idVector = reallocVector(idVector, newCapacity);
// timestampVector = reallocVector(timestampVector, newCapacity);
// statusCodeVector = reallocVector(statusCodeVector, newCapacity);

// root = new VectorSchemaRoot(
// schema,
// List.of(
// idVector,
// timestampVector,
// valueColumn.getVector(),
// statusCodeVector),
// count);

// capacity = newCapacity;
// }

// @Override
// public void appendList(List<TSValue> dataValues) {
// ensureCapacityLocked(dataValues.size());
// for (TSValue dv : dataValues)
// append(dv.id, dv.timestamp, dv.value, dv.isGood);
// }

// private void append(int id, long timestampNanos, Object value, boolean
// statusCode) {
// int row = count;
// idVector.setSafe(row, id);
// timestampVector.setSafe(row, timestampNanos);
// valueColumn.set(row, value);
// statusCodeVector.setSafe(row, statusCode ? 1 : 0);
// count++;
// }

// /**
// * NOVO: flush() com Arrow C Data Interface - retorna ArrowBatch
// * Zero-copy, sem serialização
// */
// public byte[] flush() {
// if (count == 0) {
// return null;
// }

// try {
// root.setRowCount(count);

// // Aloca estruturas C para exportação
// ArrowArray cArray = ArrowArray.allocateNew(allocator);
// ArrowSchema cSchema = ArrowSchema.allocateNew(allocator);

// // Exporta com zero-copy - transfere ownership
// Data.exportVectorSchemaRoot(allocator, root, null, cArray, cSchema);

// // Cria o batch encapsulado
// ArrowBatch batch = new ArrowBatch(cArray, cSchema, count);

// // IMPORTANTE: Após exportar, precisamos realocar os vetores
// // pois o ownership foi transferido para o ArrowBatch
// allocateVectors(capacity);
// count = 0;
// try {
// return batch.serialize(allocator, compress);
// } finally {
// batch.close();
// }
// } catch (Exception e) {
// throw new RuntimeException("Erro ao criar ArrowBatch via C Data Interface",
// e);
// }
// }

// public void pop() {
// if (count > 0) {
// count--;
// }
// }

// public void reset() {
// count = 0;
// if (root != null) {
// root.setRowCount(0);
// }
// }

// public int size() {
// return count;
// }

// public int capacity() {
// return capacity;
// }

// @Override
// public void close() {
// try {

// if (root != null) {
// root.close();
// }
// } finally {
// root = null;
// allocator.close();
// }
// }
// }
