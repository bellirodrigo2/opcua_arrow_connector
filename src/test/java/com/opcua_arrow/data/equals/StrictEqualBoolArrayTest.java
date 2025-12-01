package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StrictEqualBooleanArrayTest {

    private StrictEqualBooleanArray strictEqualBooleanArray;

    @BeforeEach
    void setUp() {
        strictEqualBooleanArray = new StrictEqualBooleanArray();
    }

    @Test
    @DisplayName("Should return true for identical arrays with same values")
    void testIdenticalArrays() {
        boolean[] array1 = { true, false, true, false };
        boolean[] array2 = { true, false, true, false };

        assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return false for arrays with different values")
    void testDifferentValues() {
        boolean[] array1 = { true, false, true, false };
        boolean[] array2 = { true, false, false, false }; // Third element different

        assertFalse(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return false for arrays with different lengths")
    void testDifferentLengths() {
        boolean[] array1 = { true, false, true };
        boolean[] array2 = { true, false, true, false };

        assertFalse(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return true for empty arrays")
    void testEmptyArrays() {
        boolean[] array1 = {};
        boolean[] array2 = {};

        assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return true for single element arrays with same value")
    void testSingleElementArraysSame() {
        boolean[] array1 = { true };
        boolean[] array2 = { true };

        assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));

        boolean[] array3 = { false };
        boolean[] array4 = { false };

        assertTrue(strictEqualBooleanArray.isSameValue(array4, array3));
    }

    @Test
    @DisplayName("Should return false for single element arrays with different values")
    void testSingleElementArraysDifferent() {
        boolean[] array1 = { true };
        boolean[] array2 = { false };

        assertFalse(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should return true for same array reference")
    void testSameReference() {
        boolean[] array = { true, false, true };

        assertTrue(strictEqualBooleanArray.isSameValue(array, array));
    }

    @Test
    @DisplayName("Should handle null arrays correctly")
    void testNullArrays() {
        // Both null - Arrays.equals returns true for this case
        assertTrue(strictEqualBooleanArray.isSameValue(null, null));

        // One null, one not null
        boolean[] array = { true, false };
        assertFalse(strictEqualBooleanArray.isSameValue(null, array));
        assertFalse(strictEqualBooleanArray.isSameValue(array, null));
    }

    @Test
    @DisplayName("Should handle large arrays correctly")
    void testLargeArrays() {
        int size = 10000;
        boolean[] array1 = new boolean[size];
        boolean[] array2 = new boolean[size];

        // Fill with pattern
        for (int i = 0; i < size; i++) {
            array1[i] = i % 2 == 0;
            array2[i] = i % 2 == 0;
        }

        assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));

        // Change one element
        array2[size / 2] = !array2[size / 2];
        assertFalse(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should handle all true arrays")
    void testAllTrueArrays() {
        boolean[] array1 = { true, true, true, true };
        boolean[] array2 = { true, true, true, true };

        assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should handle all false arrays")
    void testAllFalseArrays() {
        boolean[] array1 = { false, false, false, false };
        boolean[] array2 = { false, false, false, false };

        assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should detect difference at beginning of array")
    void testDifferenceAtBeginning() {
        boolean[] array1 = { true, false, true, false };
        boolean[] array2 = { false, false, true, false }; // First element different

        assertFalse(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should detect difference at end of array")
    void testDifferenceAtEnd() {
        boolean[] array1 = { true, false, true, false };
        boolean[] array2 = { true, false, true, true }; // Last element different

        assertFalse(strictEqualBooleanArray.isSameValue(array2, array1));
    }

    @Test
    @DisplayName("Should handle alternating pattern arrays")
    void testAlternatingPattern() {
        boolean[] array1 = { true, false, true, false, true, false };
        boolean[] array2 = { true, false, true, false, true, false };

        assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));

        // Reverse pattern
        boolean[] array3 = { false, true, false, true, false, true };
        assertFalse(strictEqualBooleanArray.isSameValue(array3, array1));
    }

    @Test
    @DisplayName("Should be consistent with multiple calls")
    void testConsistency() {
        boolean[] array1 = { true, false, true };
        boolean[] array2 = { true, false, true };
        boolean[] array3 = { false, true, false };

        // Multiple calls should return same result
        for (int i = 0; i < 10; i++) {
            assertTrue(strictEqualBooleanArray.isSameValue(array2, array1));
            assertFalse(strictEqualBooleanArray.isSameValue(array3, array1));
        }
    }

    @Test
    @DisplayName("Complex scenario with various array configurations")
    void testComplexScenario() {
        // Test various configurations
        boolean[] empty1 = {};
        boolean[] empty2 = {};
        assertTrue(strictEqualBooleanArray.isSameValue(empty2, empty1));

        boolean[] single = { true };
        assertFalse(strictEqualBooleanArray.isSameValue(single, empty1));

        boolean[] pattern1 = { true, true, false, false };
        boolean[] pattern2 = { true, true, false, false };
        assertTrue(strictEqualBooleanArray.isSameValue(pattern2, pattern1));

        boolean[] shifted = { false, true, true, false };
        assertFalse(strictEqualBooleanArray.isSameValue(shifted, pattern1));

        // Arrays with different internal representation but same values
        boolean[] created1 = new boolean[4];
        created1[0] = true;
        created1[1] = true;
        created1[2] = false;
        created1[3] = false;

        assertTrue(strictEqualBooleanArray.isSameValue(created1, pattern1));
    }

    @Test
    @DisplayName("Should handle arrays created different ways")
    void testArrayCreationMethods() {
        // Array literal
        boolean[] literal = { true, false, true };

        // Array created with new and filled
        boolean[] created = new boolean[3];
        created[0] = true;
        created[1] = false;
        created[2] = true;

        // Array cloned
        boolean[] cloned = literal.clone();

        assertTrue(strictEqualBooleanArray.isSameValue(created, literal));
        assertTrue(strictEqualBooleanArray.isSameValue(cloned, literal));
        assertTrue(strictEqualBooleanArray.isSameValue(cloned, created));
    }
}
