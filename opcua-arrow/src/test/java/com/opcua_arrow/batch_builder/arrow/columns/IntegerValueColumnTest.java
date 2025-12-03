package com.opcua_arrow.batch_builder.arrow.columns;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegerValueColumnTest {

    private BufferAllocator allocator;
    private IntegerValueColumn column;
    private IntVector vector;

    @BeforeEach
    void setUp() {
        allocator = new RootAllocator(Long.MAX_VALUE);
        column = new IntegerValueColumn();

        ArrowType.Int intType = new ArrowType.Int(32, true);
        Field field = new Field("value", FieldType.nullable(intType), null);
        vector = (IntVector) field.createVector(allocator);
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
    void testSetValue_PositiveInteger() {
        // Given
        column.bind(vector);

        // When
        column.set(0, 42);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(42, vector.get(0));
    }

    @Test
    void testSetValue_NegativeInteger() {
        // Given
        column.bind(vector);

        // When
        column.set(0, -123);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(-123, vector.get(0));
    }

    @Test
    void testSetValue_Zero() {
        // Given
        column.bind(vector);

        // When
        column.set(0, 0);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(0, vector.get(0));
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
        column.set(0, 10);
        column.set(1, 20);
        column.set(2, null);
        column.set(3, -30);
        vector.setValueCount(4);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(10, vector.get(0));

        assertFalse(vector.isNull(1));
        assertEquals(20, vector.get(1));

        assertTrue(vector.isNull(2));

        assertFalse(vector.isNull(3));
        assertEquals(-30, vector.get(3));
    }

    @Test
    void testGetValueClass() {
        // When
        Class<Integer> valueClass = column.getValueClass();

        // Then
        assertEquals(Integer.class, valueClass);
    }

    @Test
    void testRealloc_CopiesExistingData() {
        // Given
        column.bind(vector);
        column.set(0, 100);
        column.set(1, 200);
        column.set(2, null);
        vector.setValueCount(3);

        // When
        IntVector newVector = (IntVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(3, newVector.getValueCount());

        // Verify data was copied
        assertFalse(newVector.isNull(0));
        assertEquals(100, newVector.get(0));

        assertFalse(newVector.isNull(1));
        assertEquals(200, newVector.get(1));

        assertTrue(newVector.isNull(2));
    }

    @Test
    void testRealloc_UpdatesInternalVector() {
        // Given
        column.bind(vector);
        column.set(0, 42);
        vector.setValueCount(1);
        IntVector originalVector = (IntVector) column.getVector();

        // When
        IntVector newVector = (IntVector) column.realloc(10, allocator);

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
        IntVector newVector = (IntVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(0, newVector.getValueCount());
    }

    @Test
    void testRealloc_CanWriteToNewVector() {
        // Given
        column.bind(vector);
        column.set(0, 1);
        vector.setValueCount(1);

        // When
        column.realloc(10, allocator);
        column.set(1, 2);
        column.getVector().setValueCount(2);

        // Then
        IntVector currentVector = (IntVector) column.getVector();
        assertEquals(2, currentVector.getValueCount());

        assertFalse(currentVector.isNull(0));
        assertEquals(1, currentVector.get(0));

        assertFalse(currentVector.isNull(1));
        assertEquals(2, currentVector.get(1));
    }

    @Test
    void testRealloc_IncreasedCapacity() {
        // Given
        column.bind(vector);
        int initialCapacity = vector.getValueCapacity();

        // When
        IntVector newVector = (IntVector) column.realloc(initialCapacity * 2, allocator);

        // Then
        assertTrue(newVector.getValueCapacity() >= initialCapacity * 2);
    }

    @Test
    void testSetValue_MaxValue() {
        // Given
        column.bind(vector);

        // When
        column.set(0, Integer.MAX_VALUE);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(Integer.MAX_VALUE, vector.get(0));
    }

    @Test
    void testSetValue_MinValue() {
        // Given
        column.bind(vector);

        // When
        column.set(0, Integer.MIN_VALUE);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(Integer.MIN_VALUE, vector.get(0));
    }

    @Test
    void testSetValue_SequentialIntegers() {
        // Given
        column.bind(vector);

        // When
        for (int i = 0; i < 10; i++) {
            column.set(i, i * 100);
        }
        vector.setValueCount(10);

        // Then
        for (int i = 0; i < 10; i++) {
            assertFalse(vector.isNull(i));
            assertEquals(i * 100, vector.get(i));
        }
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
