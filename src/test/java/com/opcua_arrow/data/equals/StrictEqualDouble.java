package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StrictEqualDoubleTest {

    private StrictEqualDouble strictEqualDouble;

    @BeforeEach
    void setUp() {
        strictEqualDouble = new StrictEqualDouble();
    }

    @Test
    @DisplayName("Should return true for equal double values")
    void testEqualDoubles() {
        Double value1 = 100.5;
        Double value2 = 100.5;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return false for different double values")
    void testDifferentDoubles() {
        Double value1 = 100.5;
        Double value2 = 100.6;

        assertFalse(strictEqualDouble.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return true when both values are NaN")
    void testBothNaN() {
        Double value1 = Double.NaN;
        Double value2 = Double.NaN;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return false when only new value is NaN")
    void testOnlyNewValueNaN() {
        Double value1 = 100.0;
        Double value2 = Double.NaN;

        assertFalse(strictEqualDouble.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return false when only old value is NaN")
    void testOnlyOldValueNaN() {
        Double value1 = Double.NaN;
        Double value2 = 100.0;

        assertFalse(strictEqualDouble.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should handle Integer values correctly")
    void testIntegerValues() {
        Integer value1 = 100;
        Integer value2 = 100;
        Integer value3 = 101;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle Float values correctly")
    void testFloatValues() {
        Float value1 = 100.5f;
        Float value2 = 100.5f;
        Float value3 = 100.6f;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle Long values correctly")
    void testLongValues() {
        Long value1 = 1000000L;
        Long value2 = 1000000L;
        Long value3 = 1000001L;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle Short values correctly")
    void testShortValues() {
        Short value1 = 100;
        Short value2 = 100;
        Short value3 = 101;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle Byte values correctly")
    void testByteValues() {
        Byte value1 = 100;
        Byte value2 = 100;
        Byte value3 = 101;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle Float NaN correctly")
    void testFloatNaN() {
        Float value1 = Float.NaN;
        Float value2 = Float.NaN;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));

        Float value3 = 100.0f;
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
        assertFalse(strictEqualDouble.isSameValue(value1, value3));
    }

    @Test
    @DisplayName("Should handle positive infinity correctly")
    void testPositiveInfinity() {
        Double value1 = Double.POSITIVE_INFINITY;
        Double value2 = Double.POSITIVE_INFINITY;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));

        Double value3 = Double.NEGATIVE_INFINITY;
        assertFalse(strictEqualDouble.isSameValue(value3, value1));

        Double value4 = 100.0;
        assertFalse(strictEqualDouble.isSameValue(value4, value1));
    }

    @Test
    @DisplayName("Should handle negative infinity correctly")
    void testNegativeInfinity() {
        Double value1 = Double.NEGATIVE_INFINITY;
        Double value2 = Double.NEGATIVE_INFINITY;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));

        Double value3 = Double.POSITIVE_INFINITY;
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle zero values correctly")
    void testZeroValues() {
        Double value1 = 0.0;
        Double value2 = 0.0;
        Double value3 = -0.0; // Negative zero

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
        assertTrue(strictEqualDouble.isSameValue(value3, value1)); // 0.0 == -0.0 in Java
    }

    @Test
    @DisplayName("Should handle very small differences")
    void testVerySmallDifferences() {
        Double value1 = 1.0;
        Double value2 = 1.0000000000000001; // Smallest representable difference

        // These might be equal due to floating point precision
        // But with strict equality, they should be different if representable
        boolean result = strictEqualDouble.isSameValue(value2, value1);
        // The result depends on floating point representation

        Double value3 = 1.0 + Math.ulp(1.0); // Next representable double
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle very large values correctly")
    void testVeryLargeValues() {
        Double value1 = Double.MAX_VALUE;
        Double value2 = Double.MAX_VALUE;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));

        // Note: Double.MAX_VALUE - 1 results in the same value due to floating point
        // precision
        // The difference of 1 is too small relative to MAX_VALUE to be represented
        // Use a more significant difference
        Double value3 = Double.MAX_VALUE / 2;
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @Test
    @DisplayName("Should handle very small values correctly")
    void testVerySmallValues() {
        Double value1 = Double.MIN_VALUE;
        Double value2 = Double.MIN_VALUE;
        Double value3 = Double.MIN_VALUE * 2;

        assertTrue(strictEqualDouble.isSameValue(value2, value1));
        assertFalse(strictEqualDouble.isSameValue(value3, value1));
    }

    @ParameterizedTest
    @CsvSource({
            "100.0, 100.0, true",
            "100.0, 100.1, false",
            "-100.0, -100.0, true",
            "-100.0, 100.0, false",
            "0.0, 0.0, true",
            "0.0, 0.1, false"
    })
    @DisplayName("Parameterized test for various double combinations")
    void testDoubleComparison(double val1, double val2, boolean expected) {
        assertEquals(expected, strictEqualDouble.isSameValue(val2, val1));
    }

    @Test
    @DisplayName("Should be consistent with multiple calls")
    void testConsistency() {
        Double value1 = 123.456;
        Double value2 = 123.456;
        Double value3 = 123.457;

        // Multiple calls should return same result
        for (int i = 0; i < 10; i++) {
            assertTrue(strictEqualDouble.isSameValue(value2, value1));
            assertFalse(strictEqualDouble.isSameValue(value3, value1));
        }
    }

    @Test
    @DisplayName("Complex scenario with different Number types")
    void testComplexScenarioMixedTypes() {
        // Integer to Double comparison
        Integer intVal = 100;
        Double doubleVal = 100.0;
        assertTrue(strictEqualDouble.isSameValue(doubleVal, intVal));

        // Float to Long comparison
        Float floatVal = 100.0f;
        Long longVal = 100L;
        assertTrue(strictEqualDouble.isSameValue(longVal, floatVal));

        // Mixed with slightly different values
        Float floatVal2 = 100.1f;
        assertFalse(strictEqualDouble.isSameValue(floatVal2, longVal));

        // Byte to Short comparison
        Byte byteVal = 127;
        Short shortVal = 127;
        assertTrue(strictEqualDouble.isSameValue(shortVal, byteVal));
    }

    @Test
    @DisplayName("Should handle BigDecimal conversion if cast as Number")
    void testBigDecimalAsNumber() {
        // If BigDecimal is used as Number
        Number bd1 = new java.math.BigDecimal("100.5");
        Number bd2 = new java.math.BigDecimal("100.5");
        Number bd3 = new java.math.BigDecimal("100.6");

        assertTrue(strictEqualDouble.isSameValue(bd2, bd1));
        assertFalse(strictEqualDouble.isSameValue(bd3, bd1));
    }

    @Test
    @DisplayName("Edge case with Float infinity")
    void testFloatInfinity() {
        Float floatInf = Float.POSITIVE_INFINITY;
        Double doubleInf = Double.POSITIVE_INFINITY;

        // Both convert to positive infinity as double
        assertTrue(strictEqualDouble.isSameValue(floatInf, doubleInf));

        Float floatNegInf = Float.NEGATIVE_INFINITY;
        assertFalse(strictEqualDouble.isSameValue(floatNegInf, doubleInf));
    }
}
