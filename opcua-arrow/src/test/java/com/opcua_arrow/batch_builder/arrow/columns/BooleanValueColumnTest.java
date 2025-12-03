package com.opcua_arrow.batch_builder.arrow.columns;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BooleanValueColumnTest {

    private BufferAllocator allocator;
    private BooleanValueColumn column;
    private BitVector vector;

    @BeforeEach
    void setUp() {
        allocator = new RootAllocator(Long.MAX_VALUE);
        column = new BooleanValueColumn();

        Field field = new Field("value", FieldType.nullable(ArrowType.Bool.INSTANCE), null);
        vector = (BitVector) field.createVector(allocator);
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
    void testSetValue_True() {
        // Given
        column.bind(vector);

        // When
        column.set(0, true);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(1, vector.get(0));
    }

    @Test
    void testSetValue_False() {
        // Given
        column.bind(vector);

        // When
        column.set(0, false);
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
        column.set(0, true);
        column.set(1, false);
        column.set(2, true);
        column.set(3, null);
        vector.setValueCount(4);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(1, vector.get(0));

        assertFalse(vector.isNull(1));
        assertEquals(0, vector.get(1));

        assertFalse(vector.isNull(2));
        assertEquals(1, vector.get(2));

        assertTrue(vector.isNull(3));
    }

    @Test
    void testGetValueClass() {
        // When
        Class<Boolean> valueClass = column.getValueClass();

        // Then
        assertEquals(Boolean.class, valueClass);
    }

    @Test
    void testRealloc_CopiesExistingData() {
        // Given
        column.bind(vector);
        column.set(0, true);
        column.set(1, false);
        column.set(2, null);
        vector.setValueCount(3);

        // When
        BitVector newVector = (BitVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(3, newVector.getValueCount());

        // Verify data was copied
        assertFalse(newVector.isNull(0));
        assertEquals(1, newVector.get(0));

        assertFalse(newVector.isNull(1));
        assertEquals(0, newVector.get(1));

        assertTrue(newVector.isNull(2));
    }

    @Test
    void testRealloc_UpdatesInternalVector() {
        // Given
        column.bind(vector);
        column.set(0, true);
        vector.setValueCount(1);
        BitVector originalVector = (BitVector) column.getVector();

        // When
        BitVector newVector = (BitVector) column.realloc(10, allocator);

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
        BitVector newVector = (BitVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(0, newVector.getValueCount());
    }

    @Test
    void testRealloc_CanWriteToNewVector() {
        // Given
        column.bind(vector);
        column.set(0, true);
        vector.setValueCount(1);

        // When
        column.realloc(10, allocator);
        column.set(1, false);
        column.getVector().setValueCount(2);

        // Then
        BitVector currentVector = (BitVector) column.getVector();
        assertEquals(2, currentVector.getValueCount());

        assertFalse(currentVector.isNull(0));
        assertEquals(1, currentVector.get(0));

        assertFalse(currentVector.isNull(1));
        assertEquals(0, currentVector.get(1));
    }

    @Test
    void testRealloc_IncreasedCapacity() {
        // Given
        column.bind(vector);
        int initialCapacity = vector.getValueCapacity();

        // When
        BitVector newVector = (BitVector) column.realloc(initialCapacity * 2, allocator);

        // Then
        assertTrue(newVector.getValueCapacity() >= initialCapacity * 2);
    }

    @Test
    void testSetValue_AlternatingTrueFalse() {
        // Given
        column.bind(vector);

        // When
        for (int i = 0; i < 10; i++) {
            column.set(i, i % 2 == 0);
        }
        vector.setValueCount(10);

        // Then
        for (int i = 0; i < 10; i++) {
            assertFalse(vector.isNull(i));
            assertEquals(i % 2 == 0 ? 1 : 0, vector.get(i));
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
