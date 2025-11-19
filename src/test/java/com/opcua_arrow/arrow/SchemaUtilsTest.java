package com.opcua_arrow.arrow;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaUtilsTest {

    @Test
    void testCreateSchemaWithIdLookup() {
        Map<String, Integer> idLookup = new HashMap<>();
        idLookup.put("node1", 1);
        idLookup.put("node2", 2);
        
        Schema schema = SchemaUtils.createSchema(Double.class, idLookup != null);
        
        assertNotNull(schema);
        List<Field> fields = schema.getFields();
        assertEquals(4, fields.size());
        
        // Check pointid field (should be Int32 when idLookup is provided)
        Field pointidField = fields.get(0);
        assertEquals("pointid", pointidField.getName());
        assertFalse(pointidField.isNullable());
        assertTrue(pointidField.getType() instanceof ArrowType.Int);
        ArrowType.Int pointidType = (ArrowType.Int) pointidField.getType();
        assertEquals(32, pointidType.getBitWidth());
        assertTrue(pointidType.getIsSigned());
        
        // Check timestamp field
        Field timestampField = fields.get(1);
        assertEquals("timestamp", timestampField.getName());
        assertFalse(timestampField.isNullable());
        assertTrue(timestampField.getType() instanceof ArrowType.Timestamp);
        ArrowType.Timestamp timestampType = (ArrowType.Timestamp) timestampField.getType();
        assertEquals(TimeUnit.NANOSECOND, timestampType.getUnit());
        assertEquals("UTC", timestampType.getTimezone());
        
        // Check value field (Double)
        Field valueField = fields.get(2);
        assertEquals("value", valueField.getName());
        assertTrue(valueField.isNullable());
        assertTrue(valueField.getType() instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint valueType = (ArrowType.FloatingPoint) valueField.getType();
        assertEquals(FloatingPointPrecision.DOUBLE, valueType.getPrecision());
        
        // Check statuscode field
        Field statuscodeField = fields.get(3);
        assertEquals("statuscode", statuscodeField.getName());
        assertFalse(statuscodeField.isNullable());
        assertTrue(statuscodeField.getType() instanceof ArrowType.Int);
        ArrowType.Int statuscodeType = (ArrowType.Int) statuscodeField.getType();
        assertEquals(32, statuscodeType.getBitWidth());
        assertTrue(statuscodeType.getIsSigned());
    }

    @Test
    void testCreateSchemaWithoutIdLookup() {
        Schema schema = SchemaUtils.createSchema(String.class, false);
        
        assertNotNull(schema);
        List<Field> fields = schema.getFields();
        assertEquals(4, fields.size());
        
        // Check pointid field (should be UTF8 when no idLookup)
        Field pointidField = fields.get(0);
        assertEquals("pointid", pointidField.getName());
        assertFalse(pointidField.isNullable());
        assertEquals(ArrowType.Utf8.INSTANCE, pointidField.getType());
        
        // Check value field (String)
        Field valueField = fields.get(2);
        assertEquals("value", valueField.getName());
        assertTrue(valueField.isNullable());
        assertEquals(ArrowType.Utf8.INSTANCE, valueField.getType());
    }

    @Test
    void testGetArrowTypeDouble() {
        ArrowType doubleType = SchemaUtils.getArrowType(Double.class);
        assertTrue(doubleType instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint fpType = (ArrowType.FloatingPoint) doubleType;
        assertEquals(FloatingPointPrecision.DOUBLE, fpType.getPrecision());
        
        // Test primitive double
        ArrowType primitiveDoubleType = SchemaUtils.getArrowType(double.class);
        assertTrue(primitiveDoubleType instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint primitiveFpType = (ArrowType.FloatingPoint) primitiveDoubleType;
        assertEquals(FloatingPointPrecision.DOUBLE, primitiveFpType.getPrecision());
    }

    @Test
    void testGetArrowTypeFloat() {
        ArrowType floatType = SchemaUtils.getArrowType(Float.class);
        assertTrue(floatType instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint fpType = (ArrowType.FloatingPoint) floatType;
        assertEquals(FloatingPointPrecision.SINGLE, fpType.getPrecision());
        
        // Test primitive float
        ArrowType primitiveFloatType = SchemaUtils.getArrowType(float.class);
        assertTrue(primitiveFloatType instanceof ArrowType.FloatingPoint);
        ArrowType.FloatingPoint primitiveFpType = (ArrowType.FloatingPoint) primitiveFloatType;
        assertEquals(FloatingPointPrecision.SINGLE, primitiveFpType.getPrecision());
    }

    @Test
    void testGetArrowTypeBoolean() {
        ArrowType booleanType = SchemaUtils.getArrowType(Boolean.class);
        assertEquals(ArrowType.Bool.INSTANCE, booleanType);
        
        // Test primitive boolean
        ArrowType primitiveBooleanType = SchemaUtils.getArrowType(boolean.class);
        assertEquals(ArrowType.Bool.INSTANCE, primitiveBooleanType);
    }

    @Test
    void testGetArrowTypeString() {
        ArrowType stringType = SchemaUtils.getArrowType(String.class);
        assertEquals(ArrowType.Utf8.INSTANCE, stringType);
    }

    @Test
    void testGetArrowTypeInteger() {
        ArrowType integerType = SchemaUtils.getArrowType(Integer.class);
        assertTrue(integerType instanceof ArrowType.Int);
        ArrowType.Int intType = (ArrowType.Int) integerType;
        assertEquals(32, intType.getBitWidth());
        assertTrue(intType.getIsSigned());
        
        // Test primitive int
        ArrowType primitiveIntType = SchemaUtils.getArrowType(int.class);
        assertTrue(primitiveIntType instanceof ArrowType.Int);
        ArrowType.Int primitiveIntTypeConverted = (ArrowType.Int) primitiveIntType;
        assertEquals(32, primitiveIntTypeConverted.getBitWidth());
        assertTrue(primitiveIntTypeConverted.getIsSigned());
    }

    @Test
    void testGetArrowTypeLong() {
        ArrowType longType = SchemaUtils.getArrowType(Long.class);
        assertTrue(longType instanceof ArrowType.Int);
        ArrowType.Int longIntType = (ArrowType.Int) longType;
        assertEquals(64, longIntType.getBitWidth());
        assertTrue(longIntType.getIsSigned());
        
        // Test primitive long
        ArrowType primitiveLongType = SchemaUtils.getArrowType(long.class);
        assertTrue(primitiveLongType instanceof ArrowType.Int);
        ArrowType.Int primitiveLongIntType = (ArrowType.Int) primitiveLongType;
        assertEquals(64, primitiveLongIntType.getBitWidth());
        assertTrue(primitiveLongIntType.getIsSigned());
    }

    @Test
    void testGetArrowTypeUnsupported() {
        // Test with an unsupported type
        assertThrows(IllegalArgumentException.class, () -> {
            SchemaUtils.getArrowType(Object.class);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            SchemaUtils.getArrowType(java.util.Date.class);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            SchemaUtils.getArrowType(byte[].class);
        });
    }

    @Test
    void testCreateSchemaAllSupportedTypes() {
        // Test schema creation for all supported value types
        Class<?>[] supportedTypes = {
            Double.class, double.class,
            Float.class, float.class,
            Boolean.class, boolean.class,
            String.class,
            Integer.class, int.class,
            Long.class, long.class
        };
        
        Map<String, Integer> idLookup = new HashMap<>();
        idLookup.put("test", 1);
        
        for (Class<?> type : supportedTypes) {
            Schema schema = SchemaUtils.createSchema(type, idLookup!=null);
            assertNotNull(schema, "Schema should not be null for type: " + type.getSimpleName());
            assertEquals(4, schema.getFields().size(), "Schema should have 4 fields for type: " + type.getSimpleName());
            
            // Test without idLookup too
            Schema schemaNoLookup = SchemaUtils.createSchema(type, false);
            assertNotNull(schemaNoLookup, "Schema without lookup should not be null for type: " + type.getSimpleName());
            assertEquals(4, schemaNoLookup.getFields().size(), "Schema without lookup should have 4 fields for type: " + type.getSimpleName());
        }
    }

    @Test
    void testSchemaFieldOrder() {
        Schema schema = SchemaUtils.createSchema(Integer.class, false);
        List<Field> fields = schema.getFields();
        
        assertEquals("pointid", fields.get(0).getName());
        assertEquals("timestamp", fields.get(1).getName());
        assertEquals("value", fields.get(2).getName());
        assertEquals("statuscode", fields.get(3).getName());
    }

    @Test
    void testSchemaFieldNullability() {
        Schema schema = SchemaUtils.createSchema(Boolean.class, false);
        List<Field> fields = schema.getFields();
        
        // pointid should be non-nullable
        assertFalse(fields.get(0).isNullable());
        
        // timestamp should be non-nullable
        assertFalse(fields.get(1).isNullable());
        
        // value should be nullable (can be null for bad status)
        assertTrue(fields.get(2).isNullable());
        
        // statuscode should be non-nullable
        assertFalse(fields.get(3).isNullable());
    }

    @Test
    void testTimestampFieldConfiguration() {
        Schema schema = SchemaUtils.createSchema(String.class, false);
        Field timestampField = schema.getFields().get(1);
        
        assertTrue(timestampField.getType() instanceof ArrowType.Timestamp);
        ArrowType.Timestamp timestampType = (ArrowType.Timestamp) timestampField.getType();
        
        // Should use nanosecond precision
        assertEquals(TimeUnit.NANOSECOND, timestampType.getUnit());
        
        // Should specify UTC timezone
        assertEquals("UTC", timestampType.getTimezone());
    }

    @Test
    void testEmptyIdLookupVsNull() {
        // Empty map should still result in int32 pointid field
        Map<String, Integer> emptyIdLookup = new HashMap<>();
        Schema schemaWithEmpty = SchemaUtils.createSchema(Double.class, emptyIdLookup!=null);
        
        Schema schemaWithNull = SchemaUtils.createSchema(Double.class, false);
        
        // pointid field should be different
        Field pointidFieldEmpty = schemaWithEmpty.getFields().get(0);
        Field pointidFieldNull = schemaWithNull.getFields().get(0);
        
        assertTrue(pointidFieldEmpty.getType() instanceof ArrowType.Int);
        assertEquals(ArrowType.Utf8.INSTANCE, pointidFieldNull.getType());
    }
}