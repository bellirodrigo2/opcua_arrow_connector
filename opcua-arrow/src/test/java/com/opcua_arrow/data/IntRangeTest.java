package com.opcua_arrow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IntRangeTest {

    @Test
    void testConstructorAndGetters() {
        IntRange range = new IntRange(10, 100);
        assertEquals(10, range.getMin());
        assertEquals(100, range.getMax());
    }

    @Test
    void testNegativeRange() {
        IntRange range = new IntRange(-50, -10);
        assertEquals(-50, range.getMin());
        assertEquals(-10, range.getMax());
    }

    @Test
    void testZeroRange() {
        IntRange range = new IntRange(0, 0);
        assertEquals(0, range.getMin());
        assertEquals(0, range.getMax());
    }

    @Test
    void testInvertedRange() {
        IntRange range = new IntRange(100, 10);
        assertEquals(100, range.getMin());
        assertEquals(10, range.getMax());
    }

    @Test
    void testMaxIntegerRange() {
        IntRange range = new IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, range.getMin());
        assertEquals(Integer.MAX_VALUE, range.getMax());
    }
}
