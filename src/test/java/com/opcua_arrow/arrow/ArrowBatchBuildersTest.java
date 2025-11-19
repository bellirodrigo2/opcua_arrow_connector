package com.opcua_arrow.arrow;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArrowBatchBuildersTest {

    private Schema floatSchema;
    private Schema intSchema;
    private Schema boolSchema;
    private Schema stringSchema;

    @BeforeEach
    void setUp() {
        floatSchema = new Schema(List.of(
                new Field("pointid", FieldType.nullable(new ArrowType.Int(32, true)), null),
                new Field("timestamp", FieldType.nullable(new ArrowType.Timestamp(TimeUnit.NANOSECOND, "UTC")), null),
                new Field("value", FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE)), null),
                new Field("statuscode", FieldType.nullable(new ArrowType.Int(32, true)), null)
        ));

        intSchema = new Schema(List.of(
                new Field("pointid", FieldType.nullable(new ArrowType.Int(32, true)), null),
                new Field("timestamp", FieldType.nullable(new ArrowType.Timestamp(TimeUnit.NANOSECOND, "UTC")), null),
                new Field("value", FieldType.nullable(new ArrowType.Int(32, true)), null),
                new Field("statuscode", FieldType.nullable(new ArrowType.Int(32, true)), null)
        ));

        boolSchema = new Schema(List.of(
                new Field("pointid", FieldType.nullable(new ArrowType.Int(32, true)), null),
                new Field("timestamp", FieldType.nullable(new ArrowType.Timestamp(TimeUnit.NANOSECOND, "UTC")), null),
                new Field("value", FieldType.nullable(new ArrowType.Bool()), null),
                new Field("statuscode", FieldType.nullable(new ArrowType.Int(32, true)), null)
        ));

        stringSchema = new Schema(List.of(
                new Field("pointid", FieldType.nullable(new ArrowType.Int(32, true)), null),
                new Field("timestamp", FieldType.nullable(new ArrowType.Timestamp(TimeUnit.NANOSECOND, "UTC")), null),
                new Field("value", FieldType.nullable(new ArrowType.Utf8()), null),
                new Field("statuscode", FieldType.nullable(new ArrowType.Int(32, true)), null)
        ));
    }

    @Test
    void testFloatArrowBatchBuilder() {
        try (ArrowBatchBuilders.FloatArrowBatchBuilder builder = 
                new ArrowBatchBuilders.FloatArrowBatchBuilder(floatSchema, 10, false)) {
            
            builder.append(1, 1000000000L, 3.14, 0)
                   .append(2, 2000000000L, 2.71, 0)
                   .append(3, 3000000000L, null, 1);

            assertEquals(3, builder.size());
            
            byte[] result = builder.flush();
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testIntArrowBatchBuilder() {
        try (ArrowBatchBuilders.IntArrowBatchBuilder builder = 
                new ArrowBatchBuilders.IntArrowBatchBuilder(intSchema, 10, false)) {
            
            builder.append(1, 1000000000L, 42, 0)
                   .append(2, 2000000000L, -123, 0)
                   .append(3, 3000000000L, null, 1);

            assertEquals(3, builder.size());
            
            byte[] result = builder.flush();
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testBoolArrowBatchBuilder() {
        try (ArrowBatchBuilders.BoolArrowBatchBuilder builder = 
                new ArrowBatchBuilders.BoolArrowBatchBuilder(boolSchema, 10, false)) {
            
            builder.append(1, 1000000000L, true, 0)
                   .append(2, 2000000000L, false, 0)
                   .append(3, 3000000000L, null, 1);

            assertEquals(3, builder.size());
            
            byte[] result = builder.flush();
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testStringArrowBatchBuilder() {
        try (ArrowBatchBuilders.StringArrowBatchBuilder builder = 
                new ArrowBatchBuilders.StringArrowBatchBuilder(stringSchema, 10, false)) {
            
            builder.append(1, 1000000000L, "hello", 0)
                   .append(2, 2000000000L, "world", 0)
                   .append(3, 3000000000L, null, 1);

            assertEquals(3, builder.size());
            
            byte[] result = builder.flush();
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testPopOperation() {
        try (ArrowBatchBuilders.IntArrowBatchBuilder builder = 
                new ArrowBatchBuilders.IntArrowBatchBuilder(intSchema, 10, false)) {
            
            builder.append(1, 1000000000L, 42, 0)
                   .append(2, 2000000000L, 123, 0);

            assertEquals(2, builder.size());
            
            builder.pop();
            assertEquals(1, builder.size());
            
            builder.pop();
            assertEquals(0, builder.size());
            
            builder.pop(); // Should not go below 0
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testCapacityExpansion() {
        try (ArrowBatchBuilders.IntArrowBatchBuilder builder = 
                new ArrowBatchBuilders.IntArrowBatchBuilder(intSchema, 2, false)) {
            
            assertEquals(2, builder.capacity());
            
            // Add more than initial capacity to trigger reallocation
            builder.append(1, 1000000000L, 1, 0)
                   .append(2, 2000000000L, 2, 0)
                   .append(3, 3000000000L, 3, 0); // This should trigger reallocation

            assertEquals(3, builder.size());
            assertEquals(4, builder.capacity()); // Should have doubled
        }
    }

    @Test
    void testCompressionEnabled() {
        try (ArrowBatchBuilders.FloatArrowBatchBuilder builder = 
                new ArrowBatchBuilders.FloatArrowBatchBuilder(floatSchema, 10, true)) {
            
            builder.append(1, 1000000000L, 3.14, 0);
            
            byte[] compressed = builder.flush();
            assertNotNull(compressed);
            assertTrue(compressed.length > 0);
        }
    }

    @Test
    void testEmptyFlush() {
        try (ArrowBatchBuilders.IntArrowBatchBuilder builder = 
                new ArrowBatchBuilders.IntArrowBatchBuilder(intSchema, 10, false)) {
            
            byte[] result = builder.flush();
            assertNotNull(result);
            assertEquals(0, result.length);
        }
    }

    @Test
    void testNullPointId() {
        try (ArrowBatchBuilders.IntArrowBatchBuilder builder = 
                new ArrowBatchBuilders.IntArrowBatchBuilder(intSchema, 10, false)) {
            
            builder.append(null, 1000000000L, 42, 0);
            assertEquals(1, builder.size());
            
            byte[] result = builder.flush();
            assertNotNull(result);
            assertTrue(result.length > 0);
        }
    }

    @Test
    void testStringPointIdSchema() {
        Schema stringPointIdSchema = new Schema(List.of(
                new Field("pointid", FieldType.nullable(new ArrowType.Utf8()), null),
                new Field("timestamp", FieldType.nullable(new ArrowType.Timestamp(TimeUnit.NANOSECOND, "UTC")), null),
                new Field("value", FieldType.nullable(new ArrowType.Int(32, true)), null),
                new Field("statuscode", FieldType.nullable(new ArrowType.Int(32, true)), null)
        ));

        try (ArrowBatchBuilders.IntArrowBatchBuilder builder = 
                new ArrowBatchBuilders.IntArrowBatchBuilder(stringPointIdSchema, 10, false)) {
            
            builder.append(123, 1000000000L, 42, 0);
            assertEquals(1, builder.size());
            
            byte[] result = builder.flush();
            assertNotNull(result);
            assertTrue(result.length > 0);
        }
    }

    @Test
    void testMultipleFlushes() {
        try (ArrowBatchBuilders.IntArrowBatchBuilder builder = 
                new ArrowBatchBuilders.IntArrowBatchBuilder(intSchema, 10, false)) {
            
            // First batch
            builder.append(1, 1000000000L, 42, 0);
            byte[] batch1 = builder.flush();
            assertNotNull(batch1);
            assertTrue(batch1.length > 0);
            assertEquals(0, builder.size());
            
            // Second batch
            builder.append(2, 2000000000L, 123, 0);
            byte[] batch2 = builder.flush();
            assertNotNull(batch2);
            assertTrue(batch2.length > 0);
            assertEquals(0, builder.size());
        }
    }
}