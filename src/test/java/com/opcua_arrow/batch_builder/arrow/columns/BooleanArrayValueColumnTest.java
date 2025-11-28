package com.opcua_arrow.batch_builder.arrow.columns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BooleanArrayValueColumnTest {

    private BooleanArrayValueColumn column;
    private BufferAllocator allocator;
    private ListVector vector;

    @BeforeEach
    void setUp() {
        column = new BooleanArrayValueColumn();
        allocator = new RootAllocator();

        // Create ListVector with boolean element type
        Field elementField = Field.nullable("element", ArrowType.Bool.INSTANCE);
        Field listField = new Field("value",
                new FieldType(true, new ArrowType.List(), null),
                Collections.singletonList(elementField));

        vector = (ListVector) listField.createVector(allocator);
        vector.allocateNew();
    }

    @AfterEach
    void tearDown() {
        if (vector != null) {
            vector.close();
        }
        if (allocator != null) {
            allocator.close();
        }
    }

    @Test
    void testBind() {
        column.bind(vector);
        assertSame(vector, column.getVector());
    }

    @Test
    void testBindWithNullDataVector() {
        // Create empty ListVector without initialized children
        ListVector emptyVector = ListVector.empty("test", allocator);

        column.bind(emptyVector);
        assertSame(emptyVector, column.getVector());

        // Should initialize children after binding
        assertNotNull(emptyVector.getDataVector());

        emptyVector.close();
    }

    @Test
    void testBindWithWrongVectorType() {
        Field field = Field.nullable("test", ArrowType.Bool.INSTANCE);
        FieldVector boolVector = field.createVector(allocator);

        assertThrows(ClassCastException.class, () -> {
            column.bind(boolVector);
        });

        boolVector.close();
    }

    @Test
    void testSetWithBooleanArray() {
        column.bind(vector);

        boolean[] array = { true, false, true, true, false };
        column.set(0, array);

        // Verify the list was written correctly
        assertFalse(vector.isNull(0));
        assertEquals(5, vector.getListSize(0));
    }

    @Test
    void testSetWithNullValue() {
        column.bind(vector);
        column.set(0, null);

        assertTrue(vector.isNull(0));
    }

    @Test
    void testSetWithEmptyArray() {
        column.bind(vector);

        boolean[] emptyArray = new boolean[0];
        column.set(0, emptyArray);

        assertFalse(vector.isNull(0));
        assertEquals(0, vector.getListSize(0));
    }

    @Test
    void testSetWithSingleElementArray() {
        column.bind(vector);

        boolean[] singleElement = { true };
        column.set(0, singleElement);

        assertFalse(vector.isNull(0));
        assertEquals(1, vector.getListSize(0));
    }

    @Test
    void testSetMultipleArrays() {
        column.bind(vector);

        boolean[] array1 = { true, true };
        boolean[] array2 = { false, false, false };
        boolean[] array3 = { true };

        column.set(0, array1);
        column.set(1, array2);
        column.set(2, null);
        column.set(3, array3);

        assertEquals(2, vector.getListSize(0));
        assertEquals(3, vector.getListSize(1));
        assertTrue(vector.isNull(2));
        assertEquals(1, vector.getListSize(3));
    }

    @Test
    void testSetWithLargeArray() {
        column.bind(vector);

        boolean[] largeArray = new boolean[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i % 2 == 0;
        }

        column.set(0, largeArray);

        assertFalse(vector.isNull(0));
        assertEquals(1000, vector.getListSize(0));
    }

    @Test
    void testRealloc() {
        column.bind(vector);

        // Set some initial values
        boolean[] array1 = { true, false };
        boolean[] array2 = { true, true, true };

        column.set(0, array1);
        column.set(1, array2);
        column.set(2, null);
        vector.setValueCount(3);

        FieldVector newVector = column.realloc(20, allocator);

        assertNotNull(newVector);
        assertTrue(newVector instanceof ListVector);

        ListVector newListVector = (ListVector) newVector;
        assertEquals(2, newListVector.getListSize(0));
        assertEquals(3, newListVector.getListSize(1));
        assertTrue(newListVector.isNull(2));
        assertEquals(3, newListVector.getValueCount());

        assertSame(newVector, column.getVector());
    }

    @Test
    void testReallocWithEmptyVector() {
        column.bind(vector);
        vector.setValueCount(0);

        FieldVector newVector = column.realloc(100, allocator);

        assertNotNull(newVector);
        assertEquals(0, newVector.getValueCount());
    }

    @Test
    void testGetVectorBeforeBind() {
        assertNull(column.getVector());
    }

    @Test
    void testGetValueClass() {
        assertEquals(boolean[].class, column.getValueClass());
    }

    @Test
    void testSetAtHighIndex() {
        column.bind(vector);

        // Ensure enough capacity
        for (int i = 0; i < 9; i++) {
            column.set(i, new boolean[] { false });
        }

        boolean[] testArray = { true, false, true };
        column.set(9, testArray);

        assertEquals(3, vector.getListSize(9));
    }

    @Test
    void testReallocClosesOldVector() {
        column.bind(vector);

        boolean[] array = { true };
        column.set(0, array);
        vector.setValueCount(1);

        ListVector oldVector = (ListVector) column.getVector();
        FieldVector newVector = column.realloc(50, allocator);

        assertNotSame(oldVector, column.getVector());
        assertSame(newVector, column.getVector());
    }

    @Test
    void testAllTrueArray() {
        column.bind(vector);

        boolean[] allTrue = { true, true, true, true, true };
        column.set(0, allTrue);

        assertFalse(vector.isNull(0));
        assertEquals(5, vector.getListSize(0));
    }

    @Test
    void testAllFalseArray() {
        column.bind(vector);

        boolean[] allFalse = { false, false, false, false };
        column.set(0, allFalse);

        assertFalse(vector.isNull(0));
        assertEquals(4, vector.getListSize(0));
    }

    @Test
    void testAlternatingPattern() {
        column.bind(vector);

        boolean[] alternating = { true, false, true, false, true, false };
        column.set(0, alternating);

        assertFalse(vector.isNull(0));
        assertEquals(6, vector.getListSize(0));
    }

    @Test
    void testCastExceptionOnSet() {
        column.bind(vector);

        assertThrows(ClassCastException.class, () -> {
            column.set(0, "not a boolean array");
        });

        assertThrows(ClassCastException.class, () -> {
            column.set(0, new int[] { 1, 2, 3 });
        });

        assertThrows(ClassCastException.class, () -> {
            column.set(0, true);
        });
    }

    @Test
    void testMultipleSetOperations() {
        column.bind(vector);

        for (int i = 0; i < 10; i++) {
            if (i % 3 == 0) {
                column.set(i, null);
            } else {
                boolean[] array = new boolean[i];
                for (int j = 0; j < i; j++) {
                    array[j] = j % 2 == 0;
                }
                column.set(i, array);
            }
        }

        for (int i = 0; i < 10; i++) {
            if (i % 3 == 0) {
                assertTrue(vector.isNull(i));
            } else {
                assertEquals(i, vector.getListSize(i));
            }
        }
    }

    @Test
    void testReallocPreservesComplexPattern() {
        column.bind(vector);

        column.set(0, new boolean[] { true, false, true });
        column.set(1, null);
        column.set(2, new boolean[0]);
        column.set(3, new boolean[] { false });
        vector.setValueCount(4);

        ListVector newVector = (ListVector) column.realloc(20, allocator);

        assertEquals(3, newVector.getListSize(0));
        assertTrue(newVector.isNull(1));
        assertEquals(0, newVector.getListSize(2));
        assertEquals(1, newVector.getListSize(3));
    }
}
