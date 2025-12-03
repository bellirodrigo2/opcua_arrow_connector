package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StrictEqualDoubleArrayTest {

    private StrictEqualDoubleArray strictEqualDoubleArray;

    @BeforeEach
    void setUp() {
        strictEqualDoubleArray = new StrictEqualDoubleArray();
    }

    @Test
    @DisplayName("Should return true for identical arrays with same values")
    void testIdenticalArrays() {
        double[] array1 = { 1.0, 2.5, 3.7, 4.9 };
        double[] array2 = { 1.0, 2.5, 3.7, 4.9 };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return false for arrays with different values")
    void testDifferentValues() {
        double[] array1 = { 1.0, 2.5, 3.7, 4.9 };
        double[] array2 = { 1.0, 2.5, 3.8, 4.9 }; // Third element different

        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return false for arrays with different lengths")
    void testDifferentLengths() {
        double[] array1 = { 1.0, 2.5, 3.7 };
        double[] array2 = { 1.0, 2.5, 3.7, 4.9 };

        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return true for empty arrays")
    void testEmptyArrays() {
        double[] array1 = {};
        double[] array2 = {};

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return true for single element arrays with same value")
    void testSingleElementArraysSame() {
        double[] array1 = { 42.0 };
        double[] array2 = { 42.0 };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return false for single element arrays with different values")
    void testSingleElementArraysDifferent() {
        double[] array1 = { 42.0 };
        double[] array2 = { 43.0 };

        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return true for same array reference")
    void testSameReference() {
        double[] array = { 1.0, 2.0, 3.0 };

        assertTrue(strictEqualDoubleArray.isSameValue(array, array));
    }

    @Test
    @DisplayName("Should handle null arrays correctly")
    void testNullArrays() {
        // Both null - Arrays.equals returns true for this case
        assertTrue(strictEqualDoubleArray.isSameValue(null, null));

        // One null, one not null
        double[] array = { 1.0, 2.0 };
        assertFalse(strictEqualDoubleArray.isSameValue(null, array));
        assertFalse(strictEqualDoubleArray.isSameValue(array, null));
    }

    @Test
    @DisplayName("Should handle arrays with NaN values correctly")
    void testArraysWithNaN() {
        // Arrays.equals considers NaN equal to NaN for doubles
        double[] array1 = { 1.0, Double.NaN, 3.0 };
        double[] array2 = { 1.0, Double.NaN, 3.0 };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));

        // Different position of NaN
        double[] array3 = { Double.NaN, 1.0, 3.0 };
        assertFalse(strictEqualDoubleArray.isSameValue(array3, array1));
    }

    @Test
    @DisplayName("Should handle arrays with infinity values correctly")
    void testArraysWithInfinity() {
        double[] array1 = { 1.0, Double.POSITIVE_INFINITY, 3.0 };
        double[] array2 = { 1.0, Double.POSITIVE_INFINITY, 3.0 };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));

        // Different infinity
        double[] array3 = { 1.0, Double.NEGATIVE_INFINITY, 3.0 };
        assertFalse(strictEqualDoubleArray.isSameValue(array3, array1));
    }

    @Test
    @DisplayName("Should handle arrays with negative infinity correctly")
    void testArraysWithNegativeInfinity() {
        double[] array1 = { Double.NEGATIVE_INFINITY, 0.0, Double.NEGATIVE_INFINITY };
        double[] array2 = { Double.NEGATIVE_INFINITY, 0.0, Double.NEGATIVE_INFINITY };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should handle arrays with zero and negative zero")
    void testZeroAndNegativeZero() {
        double[] array1 = { 0.0, -0.0, 1.0 };
        double[] array2 = { 0.0, -0.0, 1.0 };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));

        // Arrays.equals considers 0.0 and -0.0 as equal for doubles
        double[] array3 = { -0.0, 0.0, 1.0 };
        double[] array4 = { 0.0, -0.0, 1.0 };
        // Arrays.equals returns false here because it compares bit patterns
        // 0.0 and -0.0 have different bit patterns even though they are numerically
        // equal
        assertFalse(strictEqualDoubleArray.isSameValue(array4, array3));
    }

    @Test
    @DisplayName("Should handle large arrays correctly")
    void testLargeArrays() {
        int size = 10000;
        double[] array1 = new double[size];
        double[] array2 = new double[size];

        // Fill with values
        for (int i = 0; i < size; i++) {
            array1[i] = Math.sin(i);
            array2[i] = Math.sin(i);
        }

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));

        // Change one element
        array2[size / 2] = -999.999;
        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should detect difference at beginning of array")
    void testDifferenceAtBeginning() {
        double[] array1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] array2 = { 1.1, 2.0, 3.0, 4.0 }; // First element different

        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should detect difference at end of array")
    void testDifferenceAtEnd() {
        double[] array1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] array2 = { 1.0, 2.0, 3.0, 4.1 }; // Last element different

        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should handle arrays with very small differences")
    void testVerySmallDifferences() {
        double[] array1 = { 1.0, 2.0, 3.0 };
        double[] array2 = { 1.0, 2.0, 3.0 + Double.MIN_VALUE };

        // Arrays.equals uses exact bit comparison
        // Double.MIN_VALUE is so small that when added to 3.0,
        // it might not change the representation due to floating point precision
        // The result depends on whether the difference is representable
        boolean result = strictEqualDoubleArray.isSameValue(array2, array1);
        // This could be true or false depending on floating point representation

        // Use a more significant difference to ensure it's detectable
        double[] array3 = { 1.0, 2.0, 3.0 };
        double[] array4 = { 1.0, 2.0, 3.0000000000000004 }; // Next representable double after 3.0
        assertFalse(strictEqualDoubleArray.isSameValue(array4, array3));
    }

    @Test
    @DisplayName("Should handle arrays with special values mixed")
    void testMixedSpecialValues() {
        double[] array1 = {
                0.0,
                -0.0,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                -123.456
        };

        double[] array2 = {
                0.0,
                -0.0,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                -123.456
        };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));

        // Change one special value
        array2[2] = 0.0; // Replace NaN with 0
        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should be consistent with multiple calls")
    void testConsistency() {
        double[] array1 = { 1.1, 2.2, 3.3 };
        double[] array2 = { 1.1, 2.2, 3.3 };
        double[] array3 = { 1.1, 2.2, 3.4 };

        // Multiple calls should return same result
        for (int i = 0; i < 10; i++) {
            assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));
            assertFalse(strictEqualDoubleArray.isSameValue(array3, array1));
        }
    }

    @Test
    @DisplayName("Should handle arrays created different ways")
    void testArrayCreationMethods() {
        // Array literal
        double[] literal = { 1.5, 2.5, 3.5 };

        // Array created with new and filled
        double[] created = new double[3];
        created[0] = 1.5;
        created[1] = 2.5;
        created[2] = 3.5;

        // Array cloned
        double[] cloned = literal.clone();

        assertTrue(strictEqualDoubleArray.isSameValue(created, literal));
        assertTrue(strictEqualDoubleArray.isSameValue(cloned, literal));
        assertTrue(strictEqualDoubleArray.isSameValue(cloned, created));
    }

    @Test
    @DisplayName("Complex scenario with various configurations")
    void testComplexScenario() {
        // Empty arrays
        double[] empty1 = {};
        double[] empty2 = new double[0];
        assertTrue(strictEqualDoubleArray.isSameValue(empty2, empty1));

        // Single element
        double[] single1 = { Math.PI };
        double[] single2 = { Math.PI };
        assertTrue(strictEqualDoubleArray.isSameValue(single2, single1));

        // Mathematical constants
        double[] constants1 = { Math.PI, Math.E, Math.sqrt(2) };
        double[] constants2 = { Math.PI, Math.E, Math.sqrt(2) };
        assertTrue(strictEqualDoubleArray.isSameValue(constants2, constants1));

        // Computed values
        double[] computed1 = new double[5];
        double[] computed2 = new double[5];
        for (int i = 0; i < 5; i++) {
            computed1[i] = Math.cos(i);
            computed2[i] = Math.cos(i);
        }
        assertTrue(strictEqualDoubleArray.isSameValue(computed2, computed1));

        // All NaN array
        double[] allNaN1 = { Double.NaN, Double.NaN, Double.NaN };
        double[] allNaN2 = { Double.NaN, Double.NaN, Double.NaN };
        assertTrue(strictEqualDoubleArray.isSameValue(allNaN2, allNaN1));
    }

    @Test
    @DisplayName("Should handle subnormal values correctly")
    void testSubnormalValues() {
        double[] array1 = { Double.MIN_NORMAL, Double.MIN_NORMAL / 2, 0.0 };
        double[] array2 = { Double.MIN_NORMAL, Double.MIN_NORMAL / 2, 0.0 };

        assertTrue(strictEqualDoubleArray.isSameValue(array2, array1));

        array2[1] = Double.MIN_NORMAL / 3;
        assertFalse(strictEqualDoubleArray.isSameValue(array2, array1));
    }
}
