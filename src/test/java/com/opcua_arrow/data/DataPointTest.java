package com.opcua_arrow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opcua_arrow.data.equals.NoFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataPointTest {

    private IDataPointEqual testEquals;
    private DataWriteGroup testWriteGroup;
    private DataReadGroup testReadGroup;

    @BeforeEach
    void setUp() {
        testEquals = new NoFilter();
        IntRange range = new IntRange(1, 10);
        testWriteGroup = new DataWriteGroup(EDataType.NUMERIC, range);
        testReadGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
    }

    @Test
    void testConstructorAndGetters() {
        DataPoint point = new DataPoint(
                "TestPoint",
                "Test Description",
                "ns=2;i=1234",
                100,
                EDataType.NUMERIC,
                testEquals,
                testWriteGroup,
                testReadGroup);

        assertEquals("TestPoint", point.getName());
        assertEquals("Test Description", point.getDescription());
        assertEquals("ns=2;i=1234", point.getNodeId());
        assertEquals(100, point.getPointId());
        assertEquals(EDataType.NUMERIC, point.getDataType());
        assertSame(testEquals, point.getEquals());
        assertSame(testWriteGroup, point.getWriteGroup());
        assertSame(testReadGroup, point.getReadGroup());
    }

    @Test
    void testWithNullValues() {
        DataPoint point = new DataPoint(
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null);

        assertNull(point.getName());
        assertNull(point.getDescription());
        assertNull(point.getNodeId());
        assertEquals(0, point.getPointId());
        assertNull(point.getDataType());
        assertNull(point.getEquals());
        assertNull(point.getWriteGroup());
        assertNull(point.getReadGroup());
    }

    @Test
    void testWithEmptyStrings() {
        DataPoint point = new DataPoint(
                "",
                "",
                "",
                -1,
                EDataType.STRING,
                testEquals,
                testWriteGroup,
                testReadGroup);

        assertEquals("", point.getName());
        assertEquals("", point.getDescription());
        assertEquals("", point.getNodeId());
        assertEquals(-1, point.getPointId());
        assertEquals(EDataType.STRING, point.getDataType());
    }

    @Test
    void testWithAllDataTypes() {
        for (EDataType dataType : EDataType.values()) {
            DataPoint point = new DataPoint(
                    "Point" + dataType,
                    "Desc" + dataType,
                    "node" + dataType,
                    dataType.ordinal(),
                    dataType,
                    testEquals,
                    testWriteGroup,
                    testReadGroup);

            assertEquals(dataType, point.getDataType());
            assertEquals("Point" + dataType, point.getName());
        }
    }

    @Test
    void testWithMinMaxPointId() {
        DataPoint pointMin = new DataPoint(
                "MinPoint",
                "Min Description",
                "ns=2;i=min",
                Integer.MIN_VALUE,
                EDataType.BOOLEAN,
                testEquals,
                testWriteGroup,
                testReadGroup);

        DataPoint pointMax = new DataPoint(
                "MaxPoint",
                "Max Description",
                "ns=2;i=max",
                Integer.MAX_VALUE,
                EDataType.BOOLEAN_ARRAY,
                testEquals,
                testWriteGroup,
                testReadGroup);

        assertEquals(Integer.MIN_VALUE, pointMin.getPointId());
        assertEquals(Integer.MAX_VALUE, pointMax.getPointId());
    }

    @Test
    void testWithLongStrings() {
        String longName = "A".repeat(1000);
        String longDesc = "B".repeat(5000);
        String longNodeId = "C".repeat(10000);

        DataPoint point = new DataPoint(
                longName,
                longDesc,
                longNodeId,
                999,
                EDataType.EVENTS,
                testEquals,
                testWriteGroup,
                testReadGroup);

        assertEquals(longName, point.getName());
        assertEquals(longDesc, point.getDescription());
        assertEquals(longNodeId, point.getNodeId());
    }

    @Test
    void testWithSpecialCharacters() {
        DataPoint point = new DataPoint(
                "Name with 特殊 characters!@#$%",
                "Description\nwith\ttabs\rand\nbreaks",
                "ns=2;s=node/with/slashes\\and\\backslashes",
                42,
                EDataType.NUMERIC_ARRAY,
                testEquals,
                testWriteGroup,
                testReadGroup);

        assertEquals("Name with 特殊 characters!@#$%", point.getName());
        assertTrue(point.getDescription().contains("\n"));
        assertTrue(point.getDescription().contains("\t"));
        assertTrue(point.getNodeId().contains("/"));
        assertTrue(point.getNodeId().contains("\\"));
    }

    @Test
    void testFieldImmutability() {
        DataPoint point = new DataPoint(
                "Original",
                "Original Desc",
                "ns=2;i=1",
                1,
                EDataType.NUMERIC,
                testEquals,
                testWriteGroup,
                testReadGroup);

        // Tenta modificar os valores retornados (apenas verifica que os getters
        // funcionam corretamente)
        String name = point.getName();
        assertEquals("Original", name);

        String desc = point.getDescription();
        assertEquals("Original Desc", desc);

        String nodeId = point.getNodeId();
        assertEquals("ns=2;i=1", nodeId);
    }
}
