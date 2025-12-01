package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StrictEqualBooleanTest {

    private StrictEqualBoolean strictEqualBoolean;

    @BeforeEach
    void setUp() {
        strictEqualBoolean = new StrictEqualBoolean();
    }

    @Test
    @DisplayName("Should return true when both values are true")
    void testBothTrue() {
        Boolean value1 = Boolean.TRUE;
        Boolean value2 = Boolean.TRUE;

        assertTrue(strictEqualBoolean.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return true when both values are false")
    void testBothFalse() {
        Boolean value1 = Boolean.FALSE;
        Boolean value2 = Boolean.FALSE;

        assertTrue(strictEqualBoolean.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return false when values are different (true vs false)")
    void testTrueVsFalse() {
        Boolean value1 = Boolean.TRUE;
        Boolean value2 = Boolean.FALSE;

        assertFalse(strictEqualBoolean.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should return false when values are different (false vs true)")
    void testFalseVsTrue() {
        Boolean value1 = Boolean.FALSE;
        Boolean value2 = Boolean.TRUE;

        assertFalse(strictEqualBoolean.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should use reference equality for primitive boolean wrappers")
    void testReferenceEquality() {
        // Using Boolean.TRUE and Boolean.FALSE constants
        assertTrue(strictEqualBoolean.isSameValue(Boolean.TRUE, Boolean.TRUE));
        assertTrue(strictEqualBoolean.isSameValue(Boolean.FALSE, Boolean.FALSE));

        // These are the same references due to Boolean caching
        Boolean b1 = true;
        Boolean b2 = true;
        assertTrue(strictEqualBoolean.isSameValue(b1, b2));
    }

    @Test
    @DisplayName("Should handle Boolean created with new (though not recommended)")
    void testBooleanCreatedWithNew() {
        // Note: new Boolean() is deprecated, but testing for completeness
        @SuppressWarnings("deprecation")
        Boolean value1 = new Boolean(true);
        @SuppressWarnings("deprecation")
        Boolean value2 = new Boolean(true);

        // Since the implementation uses ==, these will be false (different references)
        assertFalse(strictEqualBoolean.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void testNullValues() {
        // Both null - same reference
        assertTrue(strictEqualBoolean.isSameValue(null, null));

        // One null, one not null
        assertFalse(strictEqualBoolean.isSameValue(null, Boolean.TRUE));
        assertFalse(strictEqualBoolean.isSameValue(Boolean.TRUE, null));
        assertFalse(strictEqualBoolean.isSameValue(null, Boolean.FALSE));
        assertFalse(strictEqualBoolean.isSameValue(Boolean.FALSE, null));
    }

    @Test
    @DisplayName("Should handle autoboxed primitive booleans")
    void testAutoboxing() {
        boolean primitiveTrue = true;
        boolean primitiveFalse = false;

        // Autoboxing will use Boolean.TRUE and Boolean.FALSE constants
        assertTrue(strictEqualBoolean.isSameValue(primitiveTrue, Boolean.TRUE));
        assertTrue(strictEqualBoolean.isSameValue(primitiveFalse, Boolean.FALSE));
        assertFalse(strictEqualBoolean.isSameValue(primitiveTrue, Boolean.FALSE));
        assertFalse(strictEqualBoolean.isSameValue(primitiveFalse, Boolean.TRUE));
    }

    @ParameterizedTest
    @CsvSource({
            "true, true, true",
            "false, false, true",
            "true, false, false",
            "false, true, false"
    })
    @DisplayName("Parameterized test for various boolean combinations")
    void testBooleanCombinations(boolean first, boolean second, boolean expectedResult) {
        Boolean value1 = first;
        Boolean value2 = second;

        assertEquals(expectedResult, strictEqualBoolean.isSameValue(value2, value1));
    }

    @Test
    @DisplayName("Should be consistent with multiple calls")
    void testConsistency() {
        Boolean value1 = Boolean.TRUE;
        Boolean value2 = Boolean.TRUE;
        Boolean value3 = Boolean.FALSE;

        // Multiple calls should return same result
        for (int i = 0; i < 10; i++) {
            assertTrue(strictEqualBoolean.isSameValue(value2, value1));
            assertFalse(strictEqualBoolean.isSameValue(value3, value1));
        }
    }

    @Test
    @DisplayName("Should work correctly when used with Boolean.valueOf")
    void testBooleanValueOf() {
        Boolean value1 = Boolean.valueOf(true);
        Boolean value2 = Boolean.valueOf(true);
        Boolean value3 = Boolean.valueOf(false);

        assertTrue(strictEqualBoolean.isSameValue(value2, value1));
        assertFalse(strictEqualBoolean.isSameValue(value3, value1));

        // valueOf with String
        Boolean value4 = Boolean.valueOf("true");
        Boolean value5 = Boolean.valueOf("false");

        assertTrue(strictEqualBoolean.isSameValue(value4, value1));
        assertTrue(strictEqualBoolean.isSameValue(value5, value3));
    }

    @Test
    @DisplayName("Edge case: comparing result of Boolean operations")
    void testBooleanOperations() {
        Boolean result1 = (5 > 3); // true
        Boolean result2 = (2 > 4); // false
        Boolean result3 = (10 > 1); // true

        assertTrue(strictEqualBoolean.isSameValue(result1, result3));
        assertFalse(strictEqualBoolean.isSameValue(result1, result2));
    }
}
