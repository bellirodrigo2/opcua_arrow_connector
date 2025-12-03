package com.opcua_arrow.batch_builder.arrow.columns;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.impl.UnionListReader;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class DoubleArrayValueColumnTest {

    private BufferAllocator allocator;
    private DoubleArrayValueColumn column;
    private ListVector vector;

    @BeforeEach
    void setUp() {
        allocator = new RootAllocator(Long.MAX_VALUE);
        column = new DoubleArrayValueColumn();

        ArrowType.FloatingPoint doubleType = new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE);
        Field elementField = Field.nullable("element", doubleType);
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
        column.set(0, new double[]{});
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
        column.set(0, new double[]{42.5});
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
        column.set(0, new double[]{1.1, 2.2, 3.3});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));

        UnionListReader reader = vector.getReader();
        reader.setPosition(0);
        assertEquals(3, reader.size());

        // Read the list values
        Float8Vector dataVector = (Float8Vector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);
        int endIndex = vector.getElementEndIndex(0);

        assertEquals(3, endIndex - startIndex);
        assertEquals(1.1, dataVector.get(startIndex), 0.0001);
        assertEquals(2.2, dataVector.get(startIndex + 1), 0.0001);
        assertEquals(3.3, dataVector.get(startIndex + 2), 0.0001);
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
        column.set(0, new double[]{10.5, 20.5});
        column.set(1, new double[]{-1.0, -2.0, -3.0});
        column.set(2, null);
        column.set(3, new double[]{99.9});
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
        Class<double[]> valueClass = column.getValueClass();

        // Then
        assertEquals(double[].class, valueClass);
    }

    @Test
    void testSetValue_NegativeValues() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new double[]{-10.5, -20.5, -30.5});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));

        Float8Vector dataVector = (Float8Vector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);
        int endIndex = vector.getElementEndIndex(0);

        assertEquals(3, endIndex - startIndex);
        assertEquals(-10.5, dataVector.get(startIndex), 0.0001);
        assertEquals(-20.5, dataVector.get(startIndex + 1), 0.0001);
        assertEquals(-30.5, dataVector.get(startIndex + 2), 0.0001);
    }

    @Test
    void testSetValue_ZeroValues() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new double[]{0.0, 0.0, 0.0});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));

        Float8Vector dataVector = (Float8Vector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);
        int endIndex = vector.getElementEndIndex(0);

        assertEquals(3, endIndex - startIndex);
        for (int i = startIndex; i < endIndex; i++) {
            assertEquals(0.0, dataVector.get(i), 0.0001);
        }
    }

    @Test
    void testSetValue_MixedValues() {
        // Given
        column.bind(vector);

        // When
        column.set(0, new double[]{-1.5, 0.0, 1.5, Double.MAX_VALUE, Double.MIN_VALUE});
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));

        Float8Vector dataVector = (Float8Vector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);

        assertEquals(-1.5, dataVector.get(startIndex), 0.0001);
        assertEquals(0.0, dataVector.get(startIndex + 1), 0.0001);
        assertEquals(1.5, dataVector.get(startIndex + 2), 0.0001);
        assertEquals(Double.MAX_VALUE, dataVector.get(startIndex + 3), 0.0);
        assertEquals(Double.MIN_VALUE, dataVector.get(startIndex + 4), 0.0);
    }

    @Test
    void testRealloc_CopiesExistingData() {
        // Given
        column.bind(vector);
        column.set(0, new double[]{1.1, 2.2});
        column.set(1, new double[]{3.3, 4.4, 5.5});
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
        column.set(0, new double[]{1.0});
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
        column.set(0, new double[]{1.0});
        vector.setValueCount(1);

        // When
        column.realloc(10, allocator);
        column.set(1, new double[]{2.0, 3.0});
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
        double[] largeArray = new double[100];
        for (int i = 0; i < 100; i++) {
            largeArray[i] = i * 1.5;
        }

        // When
        column.set(0, largeArray);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        UnionListReader reader = vector.getReader();
        reader.setPosition(0);
        assertEquals(100, reader.size());

        // Verify some values
        Float8Vector dataVector = (Float8Vector) vector.getDataVector();
        int startIndex = vector.getElementStartIndex(0);
        assertEquals(0.0, dataVector.get(startIndex), 0.0001);
        assertEquals(1.5, dataVector.get(startIndex + 1), 0.0001);
        assertEquals(148.5, dataVector.get(startIndex + 99), 0.0001);
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
