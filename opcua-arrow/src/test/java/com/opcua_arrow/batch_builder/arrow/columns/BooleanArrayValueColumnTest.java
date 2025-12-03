package com.opcua_arrow.batch_builder.arrow.columns;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.impl.UnionListReader;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class BooleanArrayValueColumnTest {

    private BufferAllocator allocator;
    private BooleanArrayValueColumn column;
    private ListVector vector;

    @BeforeEach
    void setUp() {
        allocator = new RootAllocator(Long.MAX_VALUE);
        column = new BooleanArrayValueColumn();

        Field elementField = Field.nullable("element", ArrowType.Bool.INSTANCE);
        Field listField = new Field("value",
                new FieldType(true, new ArrowType.List(), null),
                Collections.singletonList(elementField));

        vector = (ListVector) listField.createVector(allocator);
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
    void testSetValue_EmptyArray() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new boolean[]{});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        UnionListReader reader = vector.getReader();
        reader.setPosition(0);
        assertEquals(0, reader.size());
    }

    @Test
    void testSetValue_SingleElementArray() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new boolean[]{true});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        UnionListReader reader = vector.getReader();
        reader.setPosition(0);
        assertEquals(1, reader.size());
    }

    @Test
    void testSetValue_MultipleElementsArray() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new boolean[]{true, false, true});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));

        UnionListReader reader = vector.getReader();
        reader.setPosition(0);
        assertEquals(3, reader.size());

        // Read the list values
        BitVector dataVector = (BitVector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);
        int endIndex = vector.getElementEndIndex(0);

        assertEquals(3, endIndex - startIndex);
        assertEquals(1, dataVector.get(startIndex));
        assertEquals(0, dataVector.get(startIndex + 1));
        assertEquals(1, dataVector.get(startIndex + 2));
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
    void testSetValue_MultipleArrays() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new boolean[]{true, false});
        column.set(1, new boolean[]{false, false, true});
        column.set(2, null);
        column.set(3, new boolean[]{true});
        vector.setValueCount(4);

        // Then
        assertFalse(vector.isNull(0));
        assertFalse(vector.isNull(1));
        assertTrue(vector.isNull(2));
        assertFalse(vector.isNull(3));

        // Verify first array
        UnionListReader reader = vector.getReader();
        reader.setPosition(0);
        assertEquals(2, reader.size());

        // Verify second array
        reader.setPosition(1);
        assertEquals(3, reader.size());

        // Verify fourth array
        reader.setPosition(3);
        assertEquals(1, reader.size());
    }

    @Test
    void testGetValueClass() {
        // When
        Class<boolean[]> valueClass = column.getValueClass();

        // Then
        assertEquals(boolean[].class, valueClass);
    }

    @Test
    void testSetValue_AllTrue() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new boolean[]{true, true, true, true});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));

        BitVector dataVector = (BitVector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);
        int endIndex = vector.getElementEndIndex(0);

        assertEquals(4, endIndex - startIndex);
        for (int i = startIndex; i < endIndex; i++) {
            assertEquals(1, dataVector.get(i));
        }
    }

    @Test
    void testSetValue_AllFalse() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new boolean[]{false, false, false});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));

        BitVector dataVector = (BitVector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);
        int endIndex = vector.getElementEndIndex(0);

        assertEquals(3, endIndex - startIndex);
        for (int i = startIndex; i < endIndex; i++) {
            assertEquals(0, dataVector.get(i));
        }
    }

    @Test
    void testRealloc_CopiesExistingData() {
        // Given
        column.bind(vector);
        column.set(0, new boolean[]{true, false});
        column.set(1, new boolean[]{false, true, false});
        column.set(2, null);
        vector.setValueCount(3);

        // When
        ListVector newVector = (ListVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(3, newVector.getValueCount());

        // Verify first array was copied
        assertFalse(newVector.isNull(0));
        UnionListReader reader = newVector.getReader();
        reader.setPosition(0);
        assertEquals(2, reader.size());

        // Verify second array was copied
        assertFalse(newVector.isNull(1));
        reader.setPosition(1);
        assertEquals(3, reader.size());

        // Verify null was copied
        assertTrue(newVector.isNull(2));
    }

    @Test
    void testRealloc_UpdatesInternalVector() {
        // Given
        column.bind(vector);
        column.set(0, new boolean[]{true});
        vector.setValueCount(1);
        ListVector originalVector = (ListVector) column.getVector();

        // When
        ListVector newVector = (ListVector) column.realloc(10, allocator);

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
        ListVector newVector = (ListVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(0, newVector.getValueCount());
    }

    @Test
    void testRealloc_CanWriteToNewVector() {
        // Given
        column.bind(vector);
        column.set(0, new boolean[]{true});
        vector.setValueCount(1);

        // When
        column.realloc(10, allocator);
        column.set(1, new boolean[]{false, false});
        column.getVector().setValueCount(2);

        // Then
        ListVector currentVector = (ListVector) column.getVector();
        assertEquals(2, currentVector.getValueCount());

        assertFalse(currentVector.isNull(0));
        assertFalse(currentVector.isNull(1));

        UnionListReader reader = currentVector.getReader();
        reader.setPosition(0);
        assertEquals(1, reader.size());

        reader.setPosition(1);
        assertEquals(2, reader.size());
    }

    @Test
    void testSetValue_LargeArray() {
        // Given
        column.bind(vector);
        boolean[] largeArray = new boolean[100];
        for (int i = 0; i < 100; i++) {
            largeArray[i] = (i % 2 == 0);
        }

        // When
        column.set(0, largeArray);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        UnionListReader reader = vector.getReader();
        reader.setPosition(0);
        assertEquals(100, reader.size());
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
