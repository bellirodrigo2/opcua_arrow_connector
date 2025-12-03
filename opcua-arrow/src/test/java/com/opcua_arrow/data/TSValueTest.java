package com.opcua_arrow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TSValueTest {

    private DataWriteGroup testGroup;

    @BeforeEach
    void setUp() {
        IntRange range = new IntRange(1, 10);
        testGroup = new DataWriteGroup(EDataType.NUMERIC, range);
    }

    @Test
    void testConstructorAndFields() {
        TSValue value = new TSValue(123, 1000L, "test", true, testGroup);

        assertEquals(123, value.id);
        assertEquals(1000L, value.timestamp);
        assertEquals("test", value.value);
        assertTrue(value.isGood);
        assertSame(testGroup, value.writeGroup);
    }

    @Test
    void testIsConsistentWithPositiveTimestamp() {
        TSValue value = new TSValue(1, 100L, "data", true, testGroup);
        assertTrue(value.isConsistent());
    }

    @Test
    void testIsConsistentWithZeroTimestamp() {
        TSValue value = new TSValue(1, 0L, "data", false, testGroup);
        assertTrue(value.isConsistent());
    }

    @Test
    void testIsConsistentWithNegativeTimestamp() {
        TSValue value = new TSValue(1, -1L, "data", true, testGroup);
        assertFalse(value.isConsistent());
    }

    @Test
    void testIsConsistentWithLargeNegativeTimestamp() {
        TSValue value = new TSValue(1, Long.MIN_VALUE, "data", true, testGroup);
        assertFalse(value.isConsistent());
    }

    @Test
    void testWithNullValue() {
        TSValue value = new TSValue(456, 2000L, null, false, testGroup);

        assertEquals(456, value.id);
        assertEquals(2000L, value.timestamp);
        assertNull(value.value);
        assertFalse(value.isGood);
        assertTrue(value.isConsistent());
    }

    @Test
    void testWithNullWriteGroup() {
        TSValue value = new TSValue(789, 3000L, "test", true, null);

        assertEquals(789, value.id);
        assertEquals(3000L, value.timestamp);
        assertEquals("test", value.value);
        assertTrue(value.isGood);
        assertNull(value.writeGroup);
        assertTrue(value.isConsistent());
    }

    @Test
    void testWithNumericValue() {
        TSValue value = new TSValue(1, 1000L, 42.5, true, testGroup);

        assertEquals(1, value.id);
        assertEquals(1000L, value.timestamp);
        assertEquals(42.5, value.value);
        assertTrue(value.isGood);
    }

    @Test
    void testWithBooleanValue() {
        TSValue value = new TSValue(2, 2000L, Boolean.TRUE, false, testGroup);

        assertEquals(2, value.id);
        assertEquals(2000L, value.timestamp);
        assertEquals(Boolean.TRUE, value.value);
        assertFalse(value.isGood);
    }

    @Test
    void testWithMaxTimestamp() {
        TSValue value = new TSValue(1, Long.MAX_VALUE, "max", true, testGroup);

        assertEquals(Long.MAX_VALUE, value.timestamp);
        assertTrue(value.isConsistent());
    }

    @Test
    void testWithMinId() {
        TSValue value = new TSValue(Integer.MIN_VALUE, 1000L, "min", true, testGroup);

        assertEquals(Integer.MIN_VALUE, value.id);
        assertTrue(value.isConsistent());
    }

    @Test
    void testWithMaxId() {
        TSValue value = new TSValue(Integer.MAX_VALUE, 1000L, "max", true, testGroup);

        assertEquals(Integer.MAX_VALUE, value.id);
        assertTrue(value.isConsistent());
    }

    @Test
    void testFieldsArePublicFinal() {
        TSValue value = new TSValue(1, 1000L, "test", true, testGroup);

        // Verifica que os campos são públicos e finais (não podem ser modificados após
        // construção)
        assertNotNull(value.id);
        assertNotNull(value.timestamp);
        assertNotNull(value.value);
        assertNotNull(value.isGood);
        assertNotNull(value.writeGroup);
    }
}
