package com.opcua_arrow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataWriteGroupTest {

    private IntRange testRange;

    @BeforeEach
    void setUp() {
        testRange = new IntRange(1, 100);
    }

    @Test
    void testConstructorAndGetters() {
        DataWriteGroup group = new DataWriteGroup(EDataType.NUMERIC, testRange);

        assertEquals(EDataType.NUMERIC, group.getDataType());
        assertSame(testRange, group.getPointIdRange());
    }

    @Test
    void testWithBooleanDataType() {
        DataWriteGroup group = new DataWriteGroup(EDataType.BOOLEAN, testRange);

        assertEquals(EDataType.BOOLEAN, group.getDataType());
        assertSame(testRange, group.getPointIdRange());
    }

    @Test
    void testWithNullDataType() {
        DataWriteGroup group = new DataWriteGroup(null, testRange);

        assertNull(group.getDataType());
        assertSame(testRange, group.getPointIdRange());
    }

    @Test
    void testWithNullRange() {
        DataWriteGroup group = new DataWriteGroup(EDataType.STRING, null);

        assertEquals(EDataType.STRING, group.getDataType());
        assertNull(group.getPointIdRange());
    }

    @Test
    void testWithAllDataTypes() {
        for (EDataType dataType : EDataType.values()) {
            DataWriteGroup group = new DataWriteGroup(dataType, testRange);
            assertEquals(dataType, group.getDataType());
        }
    }
}
