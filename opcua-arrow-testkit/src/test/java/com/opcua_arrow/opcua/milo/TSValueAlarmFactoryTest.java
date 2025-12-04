package com.opcua_arrow.opcua.milo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.EDataType;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.IDataPointEqual;
import com.opcua_arrow.data.IntRange;
import com.opcua_arrow.data.TSValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for TSValueAlarmFactory.
 *
 * Tests the creation of TSValue instances from alarm JSON strings.
 */
public class TSValueAlarmFactoryTest {

    private DataWriteGroup writeGroup;
    private DataReadGroup readGroup;
    private IDataPointEqual alwaysAccept;

    @BeforeEach
    void setup() {
        writeGroup = new DataWriteGroup(EDataType.NUMERIC, new IntRange(0, 100));
        readGroup = new DataReadGroup(EReadMode.EVENTS, 1000L);
        alwaysAccept = (newValue, newIsGood) -> true;
    }

    // ========================================
    // Basic TSValue Creation Tests
    // ========================================

    @Test
    void testCreateTSValueFromAlarmJson() {
        DataPoint dp = createDataPoint("ns=2;s=TestAlarm", 42);
        String alarmJson = "{\"message\": \"Temperature high\", \"severity\": 500}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, alarmJson);

        assertNotNull(tsValue);
        assertEquals(42, tsValue.id);
        assertEquals(alarmJson, tsValue.value);
        assertTrue(tsValue.isGood);
    }

    @Test
    void testCreateTSValuePreservesPointId() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 123);
        String alarmJson = "{}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, alarmJson);

        assertEquals(123, tsValue.id);
    }

    @Test
    void testCreateTSValueAlwaysHasGoodQuality() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);
        String alarmJson = "{\"error\": true}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, alarmJson);

        assertTrue(tsValue.isGood, "Alarm TSValue should always have good quality");
    }

    @Test
    void testCreateTSValuePreservesWriteGroup() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);
        String alarmJson = "{}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, alarmJson);

        assertEquals(writeGroup, tsValue.writeGroup);
    }

    // ========================================
    // Timestamp Tests
    // ========================================

    @Test
    void testCreateTSValueHasPositiveTimestamp() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);
        String alarmJson = "{}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, alarmJson);

        assertTrue(tsValue.timestamp > 0, "Timestamp should be positive (nanoTime)");
    }

    @Test
    void testCreateTSValueTimestampsAreIncreasing() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);

        TSValue tsValue1 = TSValueAlarmFactory.createTSValue(dp, "{}");
        TSValue tsValue2 = TSValueAlarmFactory.createTSValue(dp, "{}");

        assertTrue(tsValue2.timestamp >= tsValue1.timestamp,
                "Timestamps should be monotonically increasing");
    }

    // ========================================
    // JSON Content Tests
    // ========================================

    @Test
    void testCreateTSValueWithEmptyJson() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);
        String emptyJson = "{}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, emptyJson);

        assertEquals("{}", tsValue.value);
    }

    @Test
    void testCreateTSValueWithComplexJson() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);
        String complexJson = "{\"eventType\": \"Alarm\", \"fields\": {\"message\": \"Test\", \"severity\": 100, \"active\": true}}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, complexJson);

        assertEquals(complexJson, tsValue.value);
    }

    @Test
    void testCreateTSValueWithNullJson() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, null);

        assertNotNull(tsValue);
        assertEquals(null, tsValue.value);
        assertTrue(tsValue.isGood);
    }

    @Test
    void testCreateTSValueWithEmptyString() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, "");

        assertNotNull(tsValue);
        assertEquals("", tsValue.value);
    }

    // ========================================
    // Multiple DataPoint Tests
    // ========================================

    @Test
    void testCreateMultipleTSValuesFromDifferentDataPoints() {
        DataPoint dp1 = createDataPoint("ns=2;s=Alarm1", 1);
        DataPoint dp2 = createDataPoint("ns=2;s=Alarm2", 2);
        DataPoint dp3 = createDataPoint("ns=2;s=Alarm3", 3);

        String json1 = "{\"id\": 1}";
        String json2 = "{\"id\": 2}";
        String json3 = "{\"id\": 3}";

        TSValue tsValue1 = TSValueAlarmFactory.createTSValue(dp1, json1);
        TSValue tsValue2 = TSValueAlarmFactory.createTSValue(dp2, json2);
        TSValue tsValue3 = TSValueAlarmFactory.createTSValue(dp3, json3);

        assertEquals(1, tsValue1.id);
        assertEquals(2, tsValue2.id);
        assertEquals(3, tsValue3.id);

        assertEquals(json1, tsValue1.value);
        assertEquals(json2, tsValue2.value);
        assertEquals(json3, tsValue3.value);
    }

    @Test
    void testCreateTSValueFromSameDataPointMultipleTimes() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);

        String json1 = "{\"event\": 1}";
        String json2 = "{\"event\": 2}";
        String json3 = "{\"event\": 3}";

        TSValue tsValue1 = TSValueAlarmFactory.createTSValue(dp, json1);
        TSValue tsValue2 = TSValueAlarmFactory.createTSValue(dp, json2);
        TSValue tsValue3 = TSValueAlarmFactory.createTSValue(dp, json3);

        // All should have same point ID
        assertEquals(1, tsValue1.id);
        assertEquals(1, tsValue2.id);
        assertEquals(1, tsValue3.id);

        // But different values
        assertEquals(json1, tsValue1.value);
        assertEquals(json2, tsValue2.value);
        assertEquals(json3, tsValue3.value);
    }

    // ========================================
    // Consistency Tests
    // ========================================

    @Test
    void testCreateTSValueIsConsistent() {
        DataPoint dp = createDataPoint("ns=2;s=Alarm1", 1);
        String alarmJson = "{\"alarm\": true}";

        TSValue tsValue = TSValueAlarmFactory.createTSValue(dp, alarmJson);

        assertTrue(tsValue.isConsistent(),
                "Alarm TSValue with good quality and positive timestamp should be consistent");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private DataPoint createDataPoint(String nodeId, int pointId) {
        return new DataPoint(
                "AlarmPoint_" + pointId,
                "Test alarm point " + pointId,
                nodeId,
                pointId,
                EDataType.STRING, // Alarms are typically stored as strings (JSON)
                alwaysAccept,
                writeGroup,
                readGroup);
    }
}
