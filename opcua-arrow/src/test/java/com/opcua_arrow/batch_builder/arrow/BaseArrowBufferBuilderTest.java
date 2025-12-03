package com.opcua_arrow.batch_builder.arrow;

import static org.junit.jupiter.api.Assertions.*;

import com.opcua_arrow.batch_builder.arrow.columns.DoubleValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.IntegerValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.StringValueColumn;
import com.opcua_arrow.data.TSValue;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

class BaseArrowBufferBuilderTest {

    @Test
    void testConstructor_CreatesVectorsCorrectly() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);

        // When
        try (BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
                BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                        schema, 10, allocator, false, valueColumn)) {

            // Then
            assertNotNull(builder);
            assertEquals(0, builder.size());
            assertEquals(10, builder.capacity());
        }
    }

    @Test
    void testAppendList_SingleValue() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 42.5, true, null));

            // When
            builder.appendList(values);

            // Then
            assertEquals(1, builder.size());
        }
    }

    @Test
    void testAppendList_MultipleValues() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null),
                    new TSValue(3, 3000000L, 30.5, false, null));

            // When
            builder.appendList(values);

            // Then
            assertEquals(3, builder.size());
        }
    }

    @Test
    void testFlush_EmptyBuffer_ReturnsNull() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            // When
            byte[] result = builder.flush();

            // Then
            assertNull(result);
        }
    }

    @Test
    void testFlush_WithData_ReturnsArrowIPC() throws Exception {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 42.5, true, null),
                    new TSValue(2, 2000000L, 99.9, false, null));
            builder.appendList(values);

            // When
            byte[] ipcBytes = builder.flush();

            // Then
            assertNotNull(ipcBytes);
            assertTrue(ipcBytes.length > 0);

            // Verify we can read the IPC data back
            try (BufferAllocator readAllocator = new RootAllocator(Long.MAX_VALUE);
                    ByteArrayInputStream bais = new ByteArrayInputStream(ipcBytes);
                    ArrowStreamReader reader = new ArrowStreamReader(bais, readAllocator)) {

                assertTrue(reader.loadNextBatch());
                VectorSchemaRoot root = reader.getVectorSchemaRoot();
                assertEquals(2, root.getRowCount());

                // Verify data
                assertEquals(1, root.getVector("pointid").getObject(0));
                assertEquals(2, root.getVector("pointid").getObject(1));
            }
        }
    }

    @Test
    void testFlush_ClearsBuffer() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 42.5, true, null));
            builder.appendList(values);
            assertEquals(1, builder.size());

            // When
            builder.flush();

            // Then
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testFlush_WithCompression() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, true, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 42.5, true, null));
            builder.appendList(values);

            // When
            byte[] compressedBytes = builder.flush();

            // Then
            assertNotNull(compressedBytes);

            // Verify it's compressed by attempting to decompress
            assertDoesNotThrow(() -> {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
                        GZIPInputStream gzis = new GZIPInputStream(bais);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = gzis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    assertTrue(baos.size() > 0);
                }
            });
        }
    }

    @Test
    void testReset_ClearsBuffer() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 42.5, true, null));
            builder.appendList(values);
            assertEquals(1, builder.size());

            // When
            builder.reset();

            // Then
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testCapacityReallocation_ExceedsInitialCapacity() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 2, allocator, false, valueColumn)) {

            assertEquals(2, builder.capacity());

            // Create more values than initial capacity
            List<TSValue> values = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                values.add(new TSValue(i, i * 1000000L, i * 10.5, true, null));
            }

            // When
            builder.appendList(values);

            // Then
            assertEquals(5, builder.size());
            assertTrue(builder.capacity() >= 5, "Capacity should have increased");
        }
    }

    @Test
    void testMultipleFlushCycles() throws Exception {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            // First cycle
            builder.appendList(List.of(new TSValue(1, 1000000L, 10.0, true, null)));
            byte[] batch1 = builder.flush();
            assertNotNull(batch1);
            assertEquals(0, builder.size());

            // Second cycle
            builder.appendList(List.of(new TSValue(2, 2000000L, 20.0, true, null)));
            byte[] batch2 = builder.flush();
            assertNotNull(batch2);
            assertEquals(0, builder.size());

            // Verify both batches are valid
            try (BufferAllocator readAllocator = new RootAllocator(Long.MAX_VALUE)) {
                try (ArrowStreamReader reader = new ArrowStreamReader(
                        new ByteArrayInputStream(batch1), readAllocator)) {
                    assertTrue(reader.loadNextBatch());
                    assertEquals(1, reader.getVectorSchemaRoot().getRowCount());
                }

                try (ArrowStreamReader reader = new ArrowStreamReader(
                        new ByteArrayInputStream(batch2), readAllocator)) {
                    assertTrue(reader.loadNextBatch());
                    assertEquals(1, reader.getVectorSchemaRoot().getRowCount());
                }
            }
        }
    }

    @Test
    void testWithIntegerValues() throws Exception {
        // Given
        IntegerValueColumn valueColumn = new IntegerValueColumn();
        Schema schema = SchemaUtils.createSchema(Integer.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 100, true, null),
                    new TSValue(2, 2000000L, 200, true, null));

            // When
            builder.appendList(values);
            byte[] ipcBytes = builder.flush();

            // Then
            assertNotNull(ipcBytes);

            // Verify data
            try (BufferAllocator readAllocator = new RootAllocator(Long.MAX_VALUE);
                    ArrowStreamReader reader = new ArrowStreamReader(
                            new ByteArrayInputStream(ipcBytes), readAllocator)) {

                assertTrue(reader.loadNextBatch());
                VectorSchemaRoot root = reader.getVectorSchemaRoot();
                assertEquals(2, root.getRowCount());
                assertEquals(100, root.getVector("value").getObject(0));
                assertEquals(200, root.getVector("value").getObject(1));
            }
        }
    }

    @Test
    void testWithStringValues() throws Exception {
        // Given
        StringValueColumn valueColumn = new StringValueColumn();
        Schema schema = SchemaUtils.createSchema(String.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, "hello", true, null),
                    new TSValue(2, 2000000L, "world", true, null));

            // When
            builder.appendList(values);
            byte[] ipcBytes = builder.flush();

            // Then
            assertNotNull(ipcBytes);

            // Verify data
            try (BufferAllocator readAllocator = new RootAllocator(Long.MAX_VALUE);
                    ArrowStreamReader reader = new ArrowStreamReader(
                            new ByteArrayInputStream(ipcBytes), readAllocator)) {

                assertTrue(reader.loadNextBatch());
                VectorSchemaRoot root = reader.getVectorSchemaRoot();
                assertEquals(2, root.getRowCount());
                assertEquals("hello", root.getVector("value").getObject(0).toString());
                assertEquals("world", root.getVector("value").getObject(1).toString());
            }
        }
    }

    @Test
    void testWithNullValues() throws Exception {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, null, true, null),
                    new TSValue(3, 3000000L, 30.5, true, null));

            // When
            builder.appendList(values);
            byte[] ipcBytes = builder.flush();

            // Then
            assertNotNull(ipcBytes);

            // Verify data
            try (BufferAllocator readAllocator = new RootAllocator(Long.MAX_VALUE);
                    ArrowStreamReader reader = new ArrowStreamReader(
                            new ByteArrayInputStream(ipcBytes), readAllocator)) {

                assertTrue(reader.loadNextBatch());
                VectorSchemaRoot root = reader.getVectorSchemaRoot();
                assertEquals(3, root.getRowCount());
                assertFalse(root.getVector("value").isNull(0));
                assertTrue(root.getVector("value").isNull(1));
                assertFalse(root.getVector("value").isNull(2));
            }
        }
    }

    @Test
    void testStatusCodeField() throws Exception {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, false, null));

            // When
            builder.appendList(values);
            byte[] ipcBytes = builder.flush();

            // Then
            try (BufferAllocator readAllocator = new RootAllocator(Long.MAX_VALUE);
                    ArrowStreamReader reader = new ArrowStreamReader(
                            new ByteArrayInputStream(ipcBytes), readAllocator)) {

                assertTrue(reader.loadNextBatch());
                VectorSchemaRoot root = reader.getVectorSchemaRoot();
                assertEquals(true, root.getVector("statuscode").getObject(0));
                assertEquals(false, root.getVector("statuscode").getObject(1));
            }
        }
    }

    @Test
    void testPop_RemovesLastElement() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            builder.appendList(List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null)));
            assertEquals(2, builder.size());

            // When
            builder.pop();

            // Then
            assertEquals(1, builder.size());
        }
    }

    @Test
    void testPop_OnEmptyBuffer_DoesNothing() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            assertEquals(0, builder.size());

            // When
            builder.pop();

            // Then
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testLargeDataSet() throws Exception {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (BaseArrowBufferBuilder builder = new BaseArrowBufferBuilder(
                schema, 10, allocator, false, valueColumn)) {

            // Create 1000 values
            List<TSValue> values = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                values.add(new TSValue(i, i * 1000000L, i * 1.5, true, null));
            }

            // When
            builder.appendList(values);
            byte[] ipcBytes = builder.flush();

            // Then
            assertNotNull(ipcBytes);

            // Verify data
            try (BufferAllocator readAllocator = new RootAllocator(Long.MAX_VALUE);
                    ArrowStreamReader reader = new ArrowStreamReader(
                            new ByteArrayInputStream(ipcBytes), readAllocator)) {

                assertTrue(reader.loadNextBatch());
                VectorSchemaRoot root = reader.getVectorSchemaRoot();
                assertEquals(1000, root.getRowCount());
            }
        }
    }
}
