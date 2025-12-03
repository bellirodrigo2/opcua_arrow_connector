package com.opcua_arrow.batch_builder.arrow.columns;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class StringValueColumnTest {

    private BufferAllocator allocator;
    private StringValueColumn column;
    private VarCharVector vector;

    @BeforeEach
    void setUp() {
        allocator = new RootAllocator(Long.MAX_VALUE);
        column = new StringValueColumn();

        Field field = new Field("value", FieldType.nullable(ArrowType.Utf8.INSTANCE), null);
        vector = (VarCharVector) field.createVector(allocator);
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
    void testSetValue_SimpleString() {
        // Given
        column.bind(vector);

        // When
        column.set(0, "Hello");
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals("Hello", new String(vector.get(0), StandardCharsets.UTF_8));
    }

    @Test
    void testSetValue_EmptyString() {
        // Given
        column.bind(vector);

        // When
        column.set(0, "");
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals("", new String(vector.get(0), StandardCharsets.UTF_8));
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
    void testSetValue_MultipleStrings() {
        // Given
        column.bind(vector);

        // When
        column.set(0, "first");
        column.set(1, "second");
        column.set(2, null);
        column.set(3, "third");
        vector.setValueCount(4);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals("first", new String(vector.get(0), StandardCharsets.UTF_8));

        assertFalse(vector.isNull(1));
        assertEquals("second", new String(vector.get(1), StandardCharsets.UTF_8));

        assertTrue(vector.isNull(2));

        assertFalse(vector.isNull(3));
        assertEquals("third", new String(vector.get(3), StandardCharsets.UTF_8));
    }

    @Test
    void testGetValueClass() {
        // When
        Class<String> valueClass = column.getValueClass();

        // Then
        assertEquals(String.class, valueClass);
    }

    @Test
    void testSetValue_UTF8Characters() {
        // Given
        column.bind(vector);

        // When - Test various UTF-8 characters
        column.set(0, "Hello 世界");  // Chinese
        column.set(1, "Привет мир");  // Russian
        column.set(2, "مرحبا بالعالم");  // Arabic
        column.set(3, "🌍🌎🌏");  // Emojis
        vector.setValueCount(4);

        // Then
        assertEquals("Hello 世界", new String(vector.get(0), StandardCharsets.UTF_8));
        assertEquals("Привет мир", new String(vector.get(1), StandardCharsets.UTF_8));
        assertEquals("مرحبا بالعالم", new String(vector.get(2), StandardCharsets.UTF_8));
        assertEquals("🌍🌎🌏", new String(vector.get(3), StandardCharsets.UTF_8));
    }

    @Test
    void testSetValue_SpecialCharacters() {
        // Given
        column.bind(vector);

        // When
        column.set(0, "Line1\nLine2");
        column.set(1, "Tab\tSeparated");
        column.set(2, "Quote\"Test");
        vector.setValueCount(3);

        // Then
        assertEquals("Line1\nLine2", new String(vector.get(0), StandardCharsets.UTF_8));
        assertEquals("Tab\tSeparated", new String(vector.get(1), StandardCharsets.UTF_8));
        assertEquals("Quote\"Test", new String(vector.get(2), StandardCharsets.UTF_8));
    }

    @Test
    void testSetValue_LongString() {
        // Given
        column.bind(vector);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("word");
        }
        String longString = sb.toString();

        // When
        column.set(0, longString);
        vector.setValueCount(1);

        // Then
        assertFalse(vector.isNull(0));
        assertEquals(longString, new String(vector.get(0), StandardCharsets.UTF_8));
    }

    @Test
    void testRealloc_CopiesExistingData() {
        // Given
        column.bind(vector);
        column.set(0, "alpha");
        column.set(1, "beta");
        column.set(2, null);
        vector.setValueCount(3);

        // When
        VarCharVector newVector = (VarCharVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(3, newVector.getValueCount());

        // Verify data was copied
        assertFalse(newVector.isNull(0));
        assertEquals("alpha", new String(newVector.get(0), StandardCharsets.UTF_8));

        assertFalse(newVector.isNull(1));
        assertEquals("beta", new String(newVector.get(1), StandardCharsets.UTF_8));

        assertTrue(newVector.isNull(2));
    }

    @Test
    void testRealloc_UpdatesInternalVector() {
        // Given
        column.bind(vector);
        column.set(0, "test");
        vector.setValueCount(1);
        VarCharVector originalVector = (VarCharVector) column.getVector();

        // When
        VarCharVector newVector = (VarCharVector) column.realloc(10, allocator);

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
        VarCharVector newVector = (VarCharVector) column.realloc(10, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(0, newVector.getValueCount());
    }

    @Test
    void testRealloc_CanWriteToNewVector() {
        // Given
        column.bind(vector);
        column.set(0, "before");
        vector.setValueCount(1);

        // When
        column.realloc(10, allocator);
        column.set(1, "after");
        column.getVector().setValueCount(2);

        // Then
        VarCharVector currentVector = (VarCharVector) column.getVector();
        assertEquals(2, currentVector.getValueCount());

        assertFalse(currentVector.isNull(0));
        assertEquals("before", new String(currentVector.get(0), StandardCharsets.UTF_8));

        assertFalse(currentVector.isNull(1));
        assertEquals("after", new String(currentVector.get(1), StandardCharsets.UTF_8));
    }

    @Test
    void testRealloc_WithVariableLengthData() {
        // Given
        column.bind(vector);
        column.set(0, "short");
        column.set(1, "a much longer string with more content");
        column.set(2, "x");
        vector.setValueCount(3);

        // When
        VarCharVector newVector = (VarCharVector) column.realloc(20, allocator);

        // Then
        assertNotNull(newVector);
        assertEquals(3, newVector.getValueCount());

        assertEquals("short", new String(newVector.get(0), StandardCharsets.UTF_8));
        assertEquals("a much longer string with more content", new String(newVector.get(1), StandardCharsets.UTF_8));
        assertEquals("x", new String(newVector.get(2), StandardCharsets.UTF_8));
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

    @Test
    void testSetValue_WhitespaceStrings() {
        // Given
        column.bind(vector);

        // When
        column.set(0, " ");
        column.set(1, "  ");
        column.set(2, "\t");
        vector.setValueCount(3);

        // Then
        assertEquals(" ", new String(vector.get(0), StandardCharsets.UTF_8));
        assertEquals("  ", new String(vector.get(1), StandardCharsets.UTF_8));
        assertEquals("\t", new String(vector.get(2), StandardCharsets.UTF_8));
    }
}
