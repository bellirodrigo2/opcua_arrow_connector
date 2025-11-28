package com.opcua_arrow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DataReadGroupTest {

    @Test
    void testConstructorAndGetters() {
        DataReadGroup group = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);

        assertEquals(EReadMode.SUBSCRIBE, group.getReadMode());
        assertEquals(1000L, group.getInterval());
    }

    @Test
    void testWithReadMode() {
        DataReadGroup group = new DataReadGroup(EReadMode.READ, 500L);

        assertEquals(EReadMode.READ, group.getReadMode());
        assertEquals(500L, group.getInterval());
    }

    @Test
    void testWithEventsMode() {
        DataReadGroup group = new DataReadGroup(EReadMode.EVENTS, 2000L);

        assertEquals(EReadMode.EVENTS, group.getReadMode());
        assertEquals(2000L, group.getInterval());
    }

    @Test
    void testWithZeroInterval() {
        DataReadGroup group = new DataReadGroup(EReadMode.SUBSCRIBE, 0L);

        assertEquals(EReadMode.SUBSCRIBE, group.getReadMode());
        assertEquals(0L, group.getInterval());
    }

    @Test
    void testWithNegativeInterval() {
        DataReadGroup group = new DataReadGroup(EReadMode.READ, -100L);

        assertEquals(EReadMode.READ, group.getReadMode());
        assertEquals(-100L, group.getInterval());
    }

    @Test
    void testWithMaxLongInterval() {
        DataReadGroup group = new DataReadGroup(EReadMode.EVENTS, Long.MAX_VALUE);

        assertEquals(EReadMode.EVENTS, group.getReadMode());
        assertEquals(Long.MAX_VALUE, group.getInterval());
    }

    @Test
    void testWithNullReadMode() {
        DataReadGroup group = new DataReadGroup(null, 1000L);

        assertNull(group.getReadMode());
        assertEquals(1000L, group.getInterval());
    }

    @Test
    void testAllReadModes() {
        for (EReadMode mode : EReadMode.values()) {
            DataReadGroup group = new DataReadGroup(mode, 1000L);
            assertEquals(mode, group.getReadMode());
        }
    }
}
