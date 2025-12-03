package com.opcua_arrow.batch_builder.arrow.columns;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DoubleValueColumnTest {

    private BufferAllocator allocator;
    private DoubleValueColumn column;
    private Float8Vector vector;

    @BeforeEach
    void setUp() {
        allocator = new RootAllocator(Long.MAX_VALUE);
        column = new DoubleValueColumn();

        ArrowType.FloatingPoint doubleType = new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE);
        Field field = new Field("value", FieldType.nullable(doubleType), null);
        vector = (Float8Vector) field.createVector(allocator);
        vector.allocateNew();
    }

    @AfterEach
    void tearDown() {
        // Close the current vector from the column (in case realloc was called)
        if (column != null && column.getVector() != null) {
            column.getVector().close();
        } else if (vector != null) {
            vector.close();
        }
        if (allocator != null) {
            allocator.close();
        }
    }

    @Test
    void testBind_VectorIsSetCorrectly() {
        // When
        column.bind(vector);

        // Then
        assertNotNull(column.getVector());
        assertEquals(vector, column.getVector());
    }

    @Test
    void testSetValue_PositiveDouble() {
        // Given
        column.bind(vector);

        // When
        column.set(0, 42.5);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(42.5, vector.get(0), 0.0001);
    }

    @Test
    void testSetValue_NegativeDouble() {
        // Given
        column.bind(vector);

        // When
        column.set(0, -123.456);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(-123.456, vector.get(0), 0.0001);
    }

    @Test
    void testSetValue_Zero() {
        // Given
        column.bind(vector);

        // When
        column.set(0, 0.0);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(0.0, vector.get(0), 0.0001);
    }

    @Test
    void testSetValue_Null() {
        // Given
        column.bind(vector);

        // When
        column.set(0, null);
        vector.setValueCount(1);

        // Then
        assertTrue(vector.isNull(0));
    }

    @Test
    void testSetValue_MultipleValues() {
        // Given
        column.bind(vector);

        // When
        column.set(0, 1.1);
        column.set(1, 2.2);
        column.set(2, null);
        column.set(3, -3.3);
        vector.setValueCount(4);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(1.1, vector.get(0), 0.0001);

        assertFalse(vector.isNull(1));
        assertEquals(2.2, vector.get(1), 0.0001);

        assertTrue(vector.isNull(2));

        assertFalse(vector.isNull(3));
        assertEquals(-3.3, vector.get(3), 0.0001);
    }

    @Test
    void testGetValueClass() {
        // When
        Class<Double> valueClass = column.getValueClass();

        // Then
        assertEquals(Double.class, valueClass);
    }

    @Test
    void testRealloc_CopiesExistingData() {
        // Given
        column.bind(vector);
        column.set(0, 10.5);
        column.set(1, 20.5);
        column.set(2, null);
        vector.setValueCount(3);

        // When
        Float8Vector newVector = (Float8Vector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(3, newVector.getValueCount());

        // Verify data was copied
        assertFalse(newVector.isNull(0));
        assertEquals(10.5, newVector.get(0), 0.0001);

        assertFalse(newVector.isNull(1));
        assertEquals(20.5, newVector.get(1), 0.0001);

        assertTrue(newVector.isNull(2));
    }

    @Test
    void testRealloc_UpdatesInternalVector() {
        // Given
        column.bind(vector);
        column.set(0, 42.0);
        vector.setValueCount(1);
        Float8Vector originalVector = (Float8Vector) column.getVector();

        // When
        Float8Vector newVector = (Float8Vector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertNotEquals(originalVector, newVector);
        assertEquals(newVector, column.getVector());
    }

    @Test
    void testRealloc_WithEmptyVector() {
        // Given
        column.bind(vector);
        vector.setValueCount(0);

        // When
        Float8Vector newVector = (Float8Vector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(0, newVector.getValueCount());
    }

    @Test
    void testRealloc_CanWriteToNewVector() {
        // Given
        column.bind(vector);
        column.set(0, 1.0);
        vector.setValueCount(1);

        // When
        column.realloc(10, allocator);
        column.set(1, 2.0);
        column.getVector().setValueCount(2);

        // Then
        Float8Vector currentVector = (Float8Vector) column.getVector();
        assertEquals(2, currentVector.getValueCount());

        assertFalse(currentVector.isNull(0));
        assertEquals(1.0, currentVector.get(0), 0.0001);

        assertFalse(currentVector.isNull(1));
        assertEquals(2.0, currentVector.get(1), 0.0001);
    }

    @Test
    void testRealloc_IncreasedCapacity() {
        // Given
        column.bind(vector);
        int initialCapacity = vector.getValueCapacity();

        // When
        Float8Vector newVector = (Float8Vector) column.realloc(initialCapacity * 2, allocator);

        // Then
        assertTrue(newVector.getValueCapacity() >= initialCapacity * 2);
    }

    @Test
    void testSetValue_VeryLargeNumber() {
        // Given
        column.bind(vector);

        // When
        column.set(0, Double.MAX_VALUE);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(Double.MAX_VALUE, vector.get(0), 0.0);
    }

    @Test
    void testSetValue_VerySmallNumber() {
        // Given
        column.bind(vector);

        // When
        column.set(0, Double.MIN_VALUE);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(Double.MIN_VALUE, vector.get(0), 0.0);
    }

    @Test
    void testSetValue_HighPrecision() {
        // Given
        column.bind(vector);
        double preciseValue = 3.141592653589793;

        // When
        column.set(0, preciseValue);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(preciseValue, vector.get(0), 1e-15);
    }

    @Test
    void testSetValue_AllNulls() {
        // Given
        column.bind(vector);

        // When
        for (int i = 0; i < 5; i++) {
            column.set(i, null);
        }
        vector.setValueCount(5);

        // Then
        for (int i = 0; i < 5; i++) {
            assertTrue(vector.isNull(i));
        }
    }
}
