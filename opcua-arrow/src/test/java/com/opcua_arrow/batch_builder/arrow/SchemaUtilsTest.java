package com.opcua_arrow.batch_builder.arrow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;

class SchemaUtilsTest {

    @Test
    void testCreateSchema_Double() {
        // When
        Schema schema = SchemaUtils.createSchema(Double.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        assertEquals(4, schema.getFields().size());

        // Verify field names
        assertEquals("pointid", schema.getFields().get(0).getName());
        assertEquals("timestamp", schema.getFields().get(1).getName());
        assertEquals("value", schema.getFields().get(2).getName());
        assertEquals("statuscode", schema.getFields().get(3).getName());

        // Verify value field type
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint fpType = (ArrowType.FloatingPoint) valueField.getType();
        assertEquals(FloatingPointPrecision.DOUBLE, fpType.getPrecision());
    }

    @Test
    void testCreateSchema_Boolean() {
        // When
        Schema schema = SchemaUtils.createSchema(Boolean.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertEquals(ArrowType.Bool.INSTANCE, valueField.getType());
    }

    @Test
    void testCreateSchema_String() {
        // When
        Schema schema = SchemaUtils.createSchema(String.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertEquals(ArrowType.Utf8.INSTANCE, valueField.getType());
    }

    @Test
    void testCreateSchema_Integer() {
        // When
        Schema schema = SchemaUtils.createSchema(Integer.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.Int);
        ArrowType.Int intType = (ArrowType.Int) valueField.getType();
        assertEquals(32, intType.getBitWidth());
        assertTrue(intType.getIsSigned());
    }

    @Test
    void testCreateSchema_Long() {
        // When
        Schema schema = SchemaUtils.createSchema(Long.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.Int);
        ArrowType.Int intType = (ArrowType.Int) valueField.getType();
        assertEquals(64, intType.getBitWidth());
        assertTrue(intType.getIsSigned());
    }

    @Test
    void testCreateSchema_Float() {
        // When
        Schema schema = SchemaUtils.createSchema(Float.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint fpType = (ArrowType.FloatingPoint) valueField.getType();
        assertEquals(FloatingPointPrecision.SINGLE, fpType.getPrecision());
    }

    @Test
    void testCreateSchema_DoubleArray() {
        // When
        Schema schema = SchemaUtils.createSchema(Double[].class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);

        // Should be a List type
        assertTrue(valueField.getType() instanceof ArrowType.List);

        // Verify children (list element)
        assertEquals(1, valueField.getChildren().size());
        Field elementField = valueField.getChildren().get(0);
        assertEquals("element", elementField.getName());

        assertTrue(elementField.getType() instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint fpType = (ArrowType.FloatingPoint) elementField.getType();
        assertEquals(FloatingPointPrecision.DOUBLE, fpType.getPrecision());
    }

    @Test
    void testCreateSchema_BooleanArray() {
        // When
        Schema schema = SchemaUtils.createSchema(Boolean[].class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);

        assertTrue(valueField.getType() instanceof ArrowType.List);
        Field elementField = valueField.getChildren().get(0);
        assertEquals(ArrowType.Bool.INSTANCE, elementField.getType());
    }

    @Test
    void testCreateSchema_IntegerArray() {
        // When
        Schema schema = SchemaUtils.createSchema(Integer[].class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);

        assertTrue(valueField.getType() instanceof ArrowType.List);
        Field elementField = valueField.getChildren().get(0);
        assertTrue(elementField.getType() instanceof ArrowType.Int);
    }

    @Test
    void testCreateSchema_PointIdField() {
        // When
        Schema schema = SchemaUtils.createSchema(Double.class, new HashMap<>());

        // Then
        Field pointIdField = schema.getFields().get(0);
        assertEquals("pointid", pointIdField.getName());
        assertFalse(pointIdField.isNullable());
        assertTrue(pointIdField.getType() instanceof ArrowType.Int);
        ArrowType.Int intType = (ArrowType.Int) pointIdField.getType();
        assertEquals(32, intType.getBitWidth());
    }

    @Test
    void testCreateSchema_TimestampField() {
        // When
        Schema schema = SchemaUtils.createSchema(Double.class, new HashMap<>());

        // Then
        Field timestampField = schema.getFields().get(1);
        assertEquals("timestamp", timestampField.getName());
        assertFalse(timestampField.isNullable());
        assertTrue(timestampField.getType() instanceof ArrowType.Timestamp);

        ArrowType.Timestamp tsType = (ArrowType.Timestamp) timestampField.getType();
        assertEquals(TimeUnit.NANOSECOND, tsType.getUnit());
        assertEquals("UTC", tsType.getTimezone());
    }

    @Test
    void testCreateSchema_StatusCodeField() {
        // When
        Schema schema = SchemaUtils.createSchema(Double.class, new HashMap<>());

        // Then
        Field statusCodeField = schema.getFields().get(3);
        assertEquals("statuscode", statusCodeField.getName());
        assertFalse(statusCodeField.isNullable());
        assertEquals(ArrowType.Bool.INSTANCE, statusCodeField.getType());
    }

    @Test
    void testCreateSchema_ValueField_IsNullable() {
        // When
        Schema schema = SchemaUtils.createSchema(Double.class, new HashMap<>());

        // Then
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.isNullable());
    }

    @Test
    void testCreateSchema_ArrayValueField_IsNullable() {
        // When
        Schema schema = SchemaUtils.createSchema(Double[].class, new HashMap<>());

        // Then
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.isNullable());
    }

    @Test
    void testCreateSchema_UnsupportedType() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            SchemaUtils.createSchema(Object.class, new HashMap<>());
        });
    }

    @Test
    void testCreateSchema_UnsupportedArrayType() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            SchemaUtils.createSchema(Object[].class, new HashMap<>());
        });
    }

    @Test
    void testGetArrowType_DeprecatedMethod_ScalarType() {
        // When
        @SuppressWarnings("deprecation")
        ArrowType result = SchemaUtils.getArrowType(Double.class);

        // Then
        assertTrue(result instanceof ArrowType.FloatingPoint);
    }

    @Test
    void testGetArrowType_DeprecatedMethod_ArrayType() {
        // When
        @SuppressWarnings("deprecation")
        ArrowType result = SchemaUtils.getArrowType(Double[].class);

        // Then
        assertTrue(result instanceof ArrowType.List);
    }

    @Test
    void testCreateSchema_PrimitiveInt() {
        // When
        Schema schema = SchemaUtils.createSchema(int.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.Int);
        ArrowType.Int intType = (ArrowType.Int) valueField.getType();
        assertEquals(32, intType.getBitWidth());
    }

    @Test
    void testCreateSchema_PrimitiveLong() {
        // When
        Schema schema = SchemaUtils.createSchema(long.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.Int);
        ArrowType.Int intType = (ArrowType.Int) valueField.getType();
        assertEquals(64, intType.getBitWidth());
    }

    @Test
    void testCreateSchema_PrimitiveDouble() {
        // When
        Schema schema = SchemaUtils.createSchema(double.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.FloatingPoint);
    }

    @Test
    void testCreateSchema_PrimitiveBoolean() {
        // When
        Schema schema = SchemaUtils.createSchema(boolean.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertEquals(ArrowType.Bool.INSTANCE, valueField.getType());
    }

    @Test
    void testCreateSchema_PrimitiveFloat() {
        // When
        Schema schema = SchemaUtils.createSchema(float.class, new HashMap<>());

        // Then
        assertNotNull(schema);
        Field valueField = schema.getFields().get(2);
        assertTrue(valueField.getType() instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint fpType = (ArrowType.FloatingPoint) valueField.getType();
        assertEquals(FloatingPointPrecision.SINGLE, fpType.getPrecision());
    }

    @Test
    void testCreateSchema_FieldOrder() {
        // When
        Schema schema = SchemaUtils.createSchema(String.class, new HashMap<>());

        // Then: verify exact field order
        assertEquals("pointid", schema.getFields().get(0).getName());
        assertEquals("timestamp", schema.getFields().get(1).getName());
        assertEquals("value", schema.getFields().get(2).getName());
        assertEquals("statuscode", schema.getFields().get(3).getName());
    }
}
