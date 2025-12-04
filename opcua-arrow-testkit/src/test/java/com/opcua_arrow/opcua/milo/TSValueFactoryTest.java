package com.opcua_arrow.opcua.milo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.EDataType;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.IDataPointEqual;
import com.opcua_arrow.data.IntRange;
import com.opcua_arrow.data.TSValue;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TSValueFactoryTest {

    private DataPoint testDataPoint;
    private DataWriteGroup writeGroup;

    @BeforeEach
    void setUp() {
        IntRange range = new IntRange(1, 100);
        writeGroup = new DataWriteGroup(EDataType.NUMERIC, range);
        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);

        // Simple IDataPointEqual that always returns true
        IDataPointEqual alwaysEqual = (newValue, newIsGood) -> true;

        testDataPoint = new DataPoint(
                "TestPoint",
                "Test description",
                "ns=2;i=1001",
                42,
                EDataType.NUMERIC,
                alwaysEqual,
                writeGroup,
                readGroup);
    }

    // ========================================
    // TSValue Creation Tests
    // ========================================

    @Test
    void testCreateTSValueWithGoodQuality() {
        Variant variant = new Variant(100.5);
        DataValue dataValue = new DataValue(variant, StatusCode.GOOD, DateTime.now(), DateTime.now());

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertNotNull(result);
        assertEquals(42, result.id);
        assertEquals(100.5, result.value);
        assertTrue(result.isGood);
        assertTrue(result.isConsistent());
        assertEquals(writeGroup, result.writeGroup);
    }

    @Test
    void testCreateTSValueWithBadQuality() {
        Variant variant = new Variant(50);
        DataValue dataValue = new DataValue(
                variant,
                new StatusCode(StatusCodes.Bad_ConnectionClosed),
                DateTime.now(),
                DateTime.now());

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertNotNull(result);
        assertEquals(42, result.id);
        assertEquals(50, result.value);
        assertFalse(result.isGood);
        assertTrue(result.isConsistent());
    }

    @Test
    void testCreateTSValueWithNullVariant() {
        DataValue dataValue = new DataValue(null, StatusCode.GOOD, DateTime.now(), DateTime.now());

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertNotNull(result);
        assertEquals(42, result.id);
        assertNull(result.value);
        assertTrue(result.isGood);
        assertTrue(result.isConsistent());
    }

    @Test
    void testCreateTSValueWithNullStatusCode() {
        Variant variant = new Variant("test");
        DataValue dataValue = new DataValue(variant, null, DateTime.now(), DateTime.now());

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertNotNull(result);
        assertEquals(42, result.id);
        assertEquals("test", result.value);
        assertFalse(result.isGood); // null status code should be treated as bad
        assertTrue(result.isConsistent());
    }

    @Test
    void testCreateTSValueWithVariousTypes() {
        // Test with Integer
        Variant intVariant = new Variant(42);
        DataValue intDv = new DataValue(intVariant, StatusCode.GOOD, DateTime.now(), null);
        TSValue intResult = TSValueFactory.createTSValue(testDataPoint, intDv);
        assertEquals(42, intResult.value);

        // Test with String
        Variant stringVariant = new Variant("Hello OPC-UA");
        DataValue stringDv = new DataValue(stringVariant, StatusCode.GOOD, DateTime.now(), null);
        TSValue stringResult = TSValueFactory.createTSValue(testDataPoint, stringDv);
        assertEquals("Hello OPC-UA", stringResult.value);

        // Test with Boolean
        Variant boolVariant = new Variant(true);
        DataValue boolDv = new DataValue(boolVariant, StatusCode.GOOD, DateTime.now(), null);
        TSValue boolResult = TSValueFactory.createTSValue(testDataPoint, boolDv);
        assertEquals(true, boolResult.value);
    }

    // ========================================
    // Timestamp Priority Tests
    // ========================================

    @Test
    void testTimestampPrioritizesSourceTime() {
        DateTime sourceTime = DateTime.now();
        DateTime serverTime = new DateTime(sourceTime.getUtcTime() + 5000); // 5 seconds later

        DataValue dataValue = new DataValue(new Variant(100), StatusCode.GOOD, sourceTime, serverTime);

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        // Calculate expected nanoseconds from sourceTime
        Instant sourceInstant = sourceTime.getJavaInstant();
        long expectedNanos = sourceInstant.getEpochSecond() * 1_000_000_000L + sourceInstant.getNano();

        assertEquals(expectedNanos, result.timestamp);
    }

    @Test
    void testTimestampFallsBackToServerTime() {
        DateTime serverTime = DateTime.now();

        DataValue dataValue = new DataValue(new Variant(200), StatusCode.GOOD, null, serverTime);

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        // Calculate expected nanoseconds from serverTime
        Instant serverInstant = serverTime.getJavaInstant();
        long expectedNanos = serverInstant.getEpochSecond() * 1_000_000_000L + serverInstant.getNano();

        assertEquals(expectedNanos, result.timestamp);
    }

    @Test
    void testTimestampReturnsNegativeOneWhenBothNull() {
        DataValue dataValue = new DataValue(new Variant(300), StatusCode.GOOD, null, null);

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertEquals(-1L, result.timestamp);
        assertFalse(result.isConsistent()); // negative timestamp means inconsistent
    }

    @Test
    void testTimestampReturnsNegativeOneWhenNullDateTime() {
        DataValue dataValue = new DataValue(new Variant(300), StatusCode.GOOD, DateTime.NULL_VALUE, DateTime.NULL_VALUE);

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertEquals(-1L, result.timestamp);
        assertFalse(result.isConsistent());
    }

    // ========================================
    // Edge Case Tests
    // ========================================

    @Test
    void testTimestampCalculationWithEpochZero() {
        DateTime dateTime = new DateTime(0L); // OPC-UA epoch (Jan 1, 1601)

        DataValue dataValue = new DataValue(new Variant(42), StatusCode.GOOD, dateTime, null);

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        // DateTime 0 in OPC-UA is not Unix epoch 0, so timestamp might be negative or different
        // The important thing is it's consistent
        assertTrue(result.isConsistent() || result.timestamp == -1L);
    }

    @Test
    void testDataPointFieldsPreserved() {
        DataValue dataValue = new DataValue(new Variant(999), StatusCode.GOOD, DateTime.now(), null);

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertEquals(testDataPoint.getPointId(), result.id);
        assertEquals(testDataPoint.getWriteGroup(), result.writeGroup);
    }

    @Test
    void testMultipleDataPointsProduceDifferentTSValues() {
        DataPoint dp1 = new DataPoint("Point1", "Desc1", "ns=2;i=1001", 1, EDataType.NUMERIC,
                (v, g) -> true, writeGroup, null);
        DataPoint dp2 = new DataPoint("Point2", "Desc2", "ns=2;i=1002", 2, EDataType.NUMERIC,
                (v, g) -> true, writeGroup, null);

        DataValue dv = new DataValue(new Variant(100), StatusCode.GOOD, DateTime.now(), null);

        TSValue tsv1 = TSValueFactory.createTSValue(dp1, dv);
        TSValue tsv2 = TSValueFactory.createTSValue(dp2, dv);

        assertEquals(1, tsv1.id);
        assertEquals(2, tsv2.id);
        assertEquals(tsv1.value, tsv2.value);
        assertEquals(tsv1.timestamp, tsv2.timestamp);
    }

    @Test
    void testTimestampWithRecentTime() {
        DateTime now = DateTime.now();
        DataValue dataValue = new DataValue(new Variant(123), StatusCode.GOOD, now, null);

        TSValue result = TSValueFactory.createTSValue(testDataPoint, dataValue);

        assertTrue(result.timestamp > 0);
        assertTrue(result.isConsistent());
    }

    @Test
    void testGoodStatusCodeVariants() {
        // Test with explicit GOOD status
        DataValue dv1 = new DataValue(new Variant(1), StatusCode.GOOD, DateTime.now(), null);
        TSValue tsv1 = TSValueFactory.createTSValue(testDataPoint, dv1);
        assertTrue(tsv1.isGood);

        // Test with BAD status
        DataValue dv2 = new DataValue(new Variant(2), new StatusCode(StatusCodes.Bad), DateTime.now(), null);
        TSValue tsv2 = TSValueFactory.createTSValue(testDataPoint, dv2);
        assertFalse(tsv2.isGood);

        // Test with UNCERTAIN status
        DataValue dv3 = new DataValue(new Variant(3), new StatusCode(StatusCodes.Uncertain), DateTime.now(), null);
        TSValue tsv3 = TSValueFactory.createTSValue(testDataPoint, dv3);
        assertFalse(tsv3.isGood);
    }
}
