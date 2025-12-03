package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BaseEqualValueTest {

    private IsSameValue mockIsSameValue;
    private BaseEqualValue baseEqualValue;

    @BeforeEach
    void setUp() {
        mockIsSameValue = mock(IsSameValue.class);
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for negative interval")
    void testConstructorNegativeInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> new BaseEqualValue(-1, mockIsSameValue),
                "Interval seconds must be non-negative");
    }

    @Test
    @DisplayName("Constructor should accept zero interval")
    void testConstructorZeroInterval() {
        assertDoesNotThrow(() -> new BaseEqualValue(0, mockIsSameValue));
    }

    @Test
    @DisplayName("Constructor should accept positive interval")
    void testConstructorPositiveInterval() {
        assertDoesNotThrow(() -> new BaseEqualValue(10, mockIsSameValue));
    }

    @Test
    @DisplayName("First call should always return false and update state")
    void testFirstCallReturnsFalse() {
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual("value1", true));
        // Verify internal state was updated (implicitly tested by subsequent calls)
    }

    @Test
    @DisplayName("First call with null value should return false")
    void testFirstCallWithNull() {
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual(null, true));
    }

    @Test
    @DisplayName("Should return false when interval has elapsed")
    void testIntervalElapsed() throws InterruptedException {
        baseEqualValue = new BaseEqualValue(0, mockIsSameValue); // 0 second interval

        assertFalse(baseEqualValue.isEqual("value1", true));
        Thread.sleep(1); // Ensure some time passes
        assertFalse(baseEqualValue.isEqual("value1", true));
    }

    @Test
    @DisplayName("Should return false when status changes from good to bad")
    void testStatusChangeGoodToBad() {
        when(mockIsSameValue.isSameValue(any(), any())).thenReturn(true);
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual("value1", true));
        assertFalse(baseEqualValue.isEqual("value1", false)); // Status changed
    }

    @Test
    @DisplayName("Should return false when status changes from bad to good")
    void testStatusChangeBadToGood() {
        when(mockIsSameValue.isSameValue(any(), any())).thenReturn(true);
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual("value1", false));
        assertFalse(baseEqualValue.isEqual("value1", true)); // Status changed
    }

    @Test
    @DisplayName("Should return true when both values are null and status unchanged")
    void testBothValuesNull() {
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual(null, true)); // First call
        assertTrue(baseEqualValue.isEqual(null, true)); // Both null, same status
    }

    @Test
    @DisplayName("Should return false when last value is null but new value is not null")
    void testLastNullNewNotNull() {
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual(null, true));
        assertFalse(baseEqualValue.isEqual("value", true));
    }

    @Test
    @DisplayName("Should return false when last value is not null but new value is null")
    void testLastNotNullNewNull() {
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual("value", true));
        assertFalse(baseEqualValue.isEqual(null, true));
    }

    @Test
    @DisplayName("Should delegate to IsSameValue when both values are non-null")
    void testDelegateToIsSameValue() {
        when(mockIsSameValue.isSameValue("value2", "value1")).thenReturn(true);
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual("value1", true)); // First call
        assertTrue(baseEqualValue.isEqual("value2", true)); // Same value according to mock

        verify(mockIsSameValue).isSameValue("value2", "value1");
    }

    @Test
    @DisplayName("Should return false when IsSameValue returns false")
    void testIsSameValueReturnsFalse() {
        when(mockIsSameValue.isSameValue("value2", "value1")).thenReturn(false);
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual("value1", true)); // First call
        assertFalse(baseEqualValue.isEqual("value2", true)); // Different value

        verify(mockIsSameValue).isSameValue("value2", "value1");
    }

    @ParameterizedTest
    @CsvSource({
            "true, true, true", // Both good
            "false, false, true" // Both bad
    })
    @DisplayName("Should return true when status unchanged and values are same")
    void testSameStatusSameValue(boolean firstStatus, boolean secondStatus, boolean expectedSame) {
        when(mockIsSameValue.isSameValue(any(), any())).thenReturn(true);
        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        assertFalse(baseEqualValue.isEqual("value", firstStatus));
        assertEquals(expectedSame, baseEqualValue.isEqual("value", secondStatus));
    }

    @Test
    @DisplayName("Complex scenario with multiple state changes")
    void testComplexScenario() {
        when(mockIsSameValue.isSameValue(anyString(), anyString())).thenAnswer(invocation -> {
            String new1 = invocation.getArgument(0);
            String old = invocation.getArgument(1);
            return new1.equals(old);
        });

        baseEqualValue = new BaseEqualValue(10, mockIsSameValue);

        // First call - always false
        assertFalse(baseEqualValue.isEqual("A", true));

        // Same value, same status - true
        assertTrue(baseEqualValue.isEqual("A", true));

        // Different value - false and updates
        assertFalse(baseEqualValue.isEqual("B", true));

        // Same new value - true
        assertTrue(baseEqualValue.isEqual("B", true));

        // Status change - false
        assertFalse(baseEqualValue.isEqual("B", false));

        // Same value with bad status - true
        assertTrue(baseEqualValue.isEqual("B", false));

        // Null value - false (updates state from "B" to null)
        assertFalse(baseEqualValue.isEqual(null, false));

        // Both null - true (both are now null)
        assertTrue(baseEqualValue.isEqual(null, false));
    }

    @Test
    @DisplayName("Should handle interval boundary correctly")
    void testIntervalBoundary() throws InterruptedException {
        // Use 1 millisecond interval (0.001 seconds)
        baseEqualValue = new BaseEqualValue(0, mockIsSameValue);
        when(mockIsSameValue.isSameValue(any(), any())).thenReturn(true);

        assertFalse(baseEqualValue.isEqual("value", true));

        // Sleep to ensure interval passes
        Thread.sleep(2);

        // After interval, should return false even with same value
        assertFalse(baseEqualValue.isEqual("value", true));
    }

    @Test
    @DisplayName("Should handle very large interval")
    void testLargeInterval() {
        // Test with maximum practical interval
        baseEqualValue = new BaseEqualValue(Long.MAX_VALUE / 1_000_000_000L, mockIsSameValue);
        when(mockIsSameValue.isSameValue(any(), any())).thenReturn(true);

        assertFalse(baseEqualValue.isEqual("value", true));
        assertTrue(baseEqualValue.isEqual("value", true)); // Should be within interval
    }
}
