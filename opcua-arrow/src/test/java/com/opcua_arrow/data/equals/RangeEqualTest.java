package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RangeEqualValueTest {

    private RangeEqualValue rangeEqualValue;

    @Test
    @DisplayName("Constructor should handle positive range")
    void testConstructorPositiveRange() {
        rangeEqualValue = new RangeEqualValue(0.1);
        assertNotNull(rangeEqualValue);
    }

    @Test
    @DisplayName("Constructor should handle negative range (converts to absolute)")
    void testConstructorNegativeRange() {
        rangeEqualValue = new RangeEqualValue(-0.1);
        assertNotNull(rangeEqualValue);

        // Test that absolute value is used
        Double value1 = 100.0;
        Double value2 = 109.0; // 9% difference
        assertTrue(rangeEqualValue.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Constructor should handle zero range")
    void testConstructorZeroRange() {
        rangeEqualValue = new RangeEqualValue(0.0);

        Double value1 = 100.0;
        Double value2 = 100.0;
        assertTrue(rangeEqualValue.isSameValue(value2, value1));

        Double value3 = 100.00001;
        assertFalse(rangeEqualValue.isSameValue(value3, value1));
    }

    @ParameterizedTest
    @CsvSource({
            "100.0, 110.0, 0.1, true", // Exactly at 10% range
            "100.0, 111.0, 0.1, false", // Just over 10% range
            "100.0, 109.0, 0.1, true", // Within 10% range
            "100.0, 90.0, 0.1, true", // Negative difference within range
            "100.0, 89.0, 0.1, false", // Negative difference over range
            "-100.0, -110.0, 0.1, true", // Negative values within range
            "-100.0, -111.0, 0.1, false" // Negative values over range
    })
    @DisplayName("Should correctly compare Double values within range")
    void testDoubleComparison(double oldVal, double newVal, double range, boolean expected) {
        rangeEqualValue = new RangeEqualValue(range);

        assertEquals(expected, rangeEqualValue.isSameValue(newVal, oldVal));
    }

    @Test
    @DisplayName("Should handle different Number types")
    void testDifferentNumberTypes() {
        rangeEqualValue = new RangeEqualValue(0.1);

        // Test with Integer
        Integer intOld = 100;
        Integer intNew = 110;
        assertTrue(rangeEqualValue.isSameValue(intNew, intOld));

        Integer intNew2 = 111;
        assertFalse(rangeEqualValue.isSameValue(intNew2, intOld));

        // Test with Float
        Float floatOld = 100.0f;
        Float floatNew = 110.0f;
        assertTrue(rangeEqualValue.isSameValue(floatNew, floatOld));

        Float floatNew2 = 111.0f;
        assertFalse(rangeEqualValue.isSameValue(floatNew2, floatOld));

        // Test with Long
        Long longOld = 100L;
        Long longNew = 110L;
        assertTrue(rangeEqualValue.isSameValue(longNew, longOld));

        Long longNew2 = 111L;
        assertFalse(rangeEqualValue.isSameValue(longNew2, longOld));
    }

    @Test
    @DisplayName("Should return false when new value is NaN")
    void testNewValueNaN() {
        rangeEqualValue = new RangeEqualValue(0.1);

        assertFalse(rangeEqualValue.isSameValue(Double.NaN, 100.0));
    }

    @Test
    @DisplayName("Should return false when old value is NaN")
    void testOldValueNaN() {
        rangeEqualValue = new RangeEqualValue(0.1);

        assertFalse(rangeEqualValue.isSameValue(100.0, Double.NaN));
    }

    @Test
    @DisplayName("Should return false when both values are NaN")
    void testBothValuesNaN() {
        rangeEqualValue = new RangeEqualValue(0.1);

        assertFalse(rangeEqualValue.isSameValue(Double.NaN, Double.NaN));
    }

    @Test
    @DisplayName("Should handle positive infinity correctly")
    void testPositiveInfinity() {
        rangeEqualValue = new RangeEqualValue(0.1);

        // Same infinity
        assertTrue(rangeEqualValue.isSameValue(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        // Different infinities
        assertFalse(rangeEqualValue.isSameValue(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));

        // Infinity vs regular number
        assertFalse(rangeEqualValue.isSameValue(Double.POSITIVE_INFINITY, 100.0));
        assertFalse(rangeEqualValue.isSameValue(100.0, Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("Should handle negative infinity correctly")
    void testNegativeInfinity() {
        rangeEqualValue = new RangeEqualValue(0.1);

        // Same infinity
        assertTrue(rangeEqualValue.isSameValue(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));

        // Different infinities
        assertFalse(rangeEqualValue.isSameValue(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

        // Infinity vs regular number
        assertFalse(rangeEqualValue.isSameValue(Double.NEGATIVE_INFINITY, -100.0));
        assertFalse(rangeEqualValue.isSameValue(-100.0, Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Should handle zero base value correctly")
    void testZeroBaseValue() {
        rangeEqualValue = new RangeEqualValue(0.1); // 0.1 absolute when base is zero

        // When old value is 0, use absolute range
        assertTrue(rangeEqualValue.isSameValue(0.09, 0.0));
        assertTrue(rangeEqualValue.isSameValue(0.1, 0.0));
        assertFalse(rangeEqualValue.isSameValue(0.11, 0.0));

        // Negative side
        assertTrue(rangeEqualValue.isSameValue(-0.09, 0.0));
        assertTrue(rangeEqualValue.isSameValue(-0.1, 0.0));
        assertFalse(rangeEqualValue.isSameValue(-0.11, 0.0));
    }

    @Test
    @DisplayName("Should handle very small values correctly")
    void testVerySmallValues() {
        rangeEqualValue = new RangeEqualValue(0.1);

        double tiny = 1e-10;
        double tinyPlus = tiny * 1.05; // 5% difference

        assertTrue(rangeEqualValue.isSameValue(tinyPlus, tiny));

        double tinyPlus15 = tiny * 1.15; // 15% difference
        assertFalse(rangeEqualValue.isSameValue(tinyPlus15, tiny));
    }

    @Test
    @DisplayName("Should handle very large values correctly")
    void testVeryLargeValues() {
        rangeEqualValue = new RangeEqualValue(0.1);

        double huge = 1e308;
        double hugePlus = huge * 1.05; // 5% difference

        assertTrue(rangeEqualValue.isSameValue(hugePlus, huge));

        double hugePlus15 = huge * 1.15; // 15% difference
        assertFalse(rangeEqualValue.isSameValue(hugePlus15, huge));
    }

    @Test
    @DisplayName("Should handle edge case at exact range boundary")
    void testExactBoundary() {
        rangeEqualValue = new RangeEqualValue(0.1);

        double base = 100.0;
        // Use direct value instead of multiplication to avoid floating point errors
        double exactBoundary = 110.0; // Exactly 10% more than 100

        // The diff will be exactly 10.0, and allowed will be exactly 10.0
        // So diff <= allowed should be true
        assertTrue(rangeEqualValue.isSameValue(exactBoundary, base));

        // Test with value clearly over boundary
        double overBoundary = 110.1; // Slightly over 10%
        assertFalse(rangeEqualValue.isSameValue(overBoundary, base));

        // Test with value just under boundary
        double underBoundary = 109.9; // Just under 10%
        assertTrue(rangeEqualValue.isSameValue(underBoundary, base));
    }

    @Test
    @DisplayName("Should handle mixed sign values correctly")
    void testMixedSigns() {
        rangeEqualValue = new RangeEqualValue(0.1);

        // Crossing zero
        assertFalse(rangeEqualValue.isSameValue(5.0, -5.0));

        // Small values around zero
        assertFalse(rangeEqualValue.isSameValue(0.05, -0.05));
    }

    @Test
    @DisplayName("Complex scenario with different number types")
    void testComplexScenarioMixedTypes() {
        rangeEqualValue = new RangeEqualValue(0.05); // 5% range

        // Integer to Double
        Integer intVal = 100;
        Double doubleVal = 104.9;
        assertTrue(rangeEqualValue.isSameValue(doubleVal, intVal));

        // Float to Long
        Float floatVal = 100.0f;
        Long longVal = 105L;
        // 105 is exactly 5% more than 100, so at the boundary (should be true with <=
        // comparison)
        assertTrue(rangeEqualValue.isSameValue(longVal, floatVal));

        // Now test just over the range
        Long longVal2 = 106L; // 6% more, over the range
        assertFalse(rangeEqualValue.isSameValue(longVal2, floatVal));

        // Short to Byte (if needed)
        Short shortVal = 100;
        Byte byteVal = 104;
        assertTrue(rangeEqualValue.isSameValue(byteVal, shortVal));
    }

    @Test
    @DisplayName("Should handle Float special values")
    void testFloatSpecialValues() {
        rangeEqualValue = new RangeEqualValue(0.1);

        // Float infinity
        assertTrue(rangeEqualValue.isSameValue(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        assertFalse(rangeEqualValue.isSameValue(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY));

        // Float NaN
        assertFalse(rangeEqualValue.isSameValue(Float.NaN, Float.NaN));
        assertFalse(rangeEqualValue.isSameValue(Float.NaN, 100.0f));
    }
}
