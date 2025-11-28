package com.opcua_arrow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EDataTypeTest {

    @Test
    void testEnumValues() {
        EDataType[] values = EDataType.values();

        assertEquals(6, values.length);
        assertEquals(EDataType.BOOLEAN, values[0]);
        assertEquals(EDataType.STRING, values[1]);
        assertEquals(EDataType.NUMERIC, values[2]);
        assertEquals(EDataType.NUMERIC_ARRAY, values[3]);
        assertEquals(EDataType.BOOLEAN_ARRAY, values[4]);
        assertEquals(EDataType.EVENTS, values[5]);
    }

    @Test
    void testValueOf() {
        assertEquals(EDataType.BOOLEAN, EDataType.valueOf("BOOLEAN"));
        assertEquals(EDataType.STRING, EDataType.valueOf("STRING"));
        assertEquals(EDataType.NUMERIC, EDataType.valueOf("NUMERIC"));
        assertEquals(EDataType.NUMERIC_ARRAY, EDataType.valueOf("NUMERIC_ARRAY"));
        assertEquals(EDataType.BOOLEAN_ARRAY, EDataType.valueOf("BOOLEAN_ARRAY"));
        assertEquals(EDataType.EVENTS, EDataType.valueOf("EVENTS"));
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            EDataType.valueOf("INVALID");
        });
    }

    @Test
    void testOrdinal() {
        assertEquals(0, EDataType.BOOLEAN.ordinal());
        assertEquals(1, EDataType.STRING.ordinal());
        assertEquals(2, EDataType.NUMERIC.ordinal());
        assertEquals(3, EDataType.NUMERIC_ARRAY.ordinal());
        assertEquals(4, EDataType.BOOLEAN_ARRAY.ordinal());
        assertEquals(5, EDataType.EVENTS.ordinal());
    }

    @Test
    void testName() {
        assertEquals("BOOLEAN", EDataType.BOOLEAN.name());
        assertEquals("STRING", EDataType.STRING.name());
        assertEquals("NUMERIC", EDataType.NUMERIC.name());
        assertEquals("NUMERIC_ARRAY", EDataType.NUMERIC_ARRAY.name());
        assertEquals("BOOLEAN_ARRAY", EDataType.BOOLEAN_ARRAY.name());
        assertEquals("EVENTS", EDataType.EVENTS.name());
    }

    @Test
    void testToString() {
        assertEquals("BOOLEAN", EDataType.BOOLEAN.toString());
        assertEquals("STRING", EDataType.STRING.toString());
        assertEquals("NUMERIC", EDataType.NUMERIC.toString());
        assertEquals("NUMERIC_ARRAY", EDataType.NUMERIC_ARRAY.toString());
        assertEquals("BOOLEAN_ARRAY", EDataType.BOOLEAN_ARRAY.toString());
        assertEquals("EVENTS", EDataType.EVENTS.toString());
    }

    @Test
    void testEnumEquality() {
        EDataType boolean1 = EDataType.BOOLEAN;
        EDataType boolean2 = EDataType.BOOLEAN;
        EDataType string1 = EDataType.STRING;

        assertEquals(boolean1, boolean2);
        assertSame(boolean1, boolean2);
        assertNotEquals(boolean1, string1);
    }

    @Test
    void testSwitchStatement() {
        EDataType type = EDataType.NUMERIC;
        String result = "";

        switch (type) {
            case BOOLEAN:
                result = "bool";
                break;
            case STRING:
                result = "str";
                break;
            case NUMERIC:
                result = "num";
                break;
            case NUMERIC_ARRAY:
                result = "num_arr";
                break;
            case BOOLEAN_ARRAY:
                result = "bool_arr";
                break;
            case EVENTS:
                result = "events";
                break;
        }

        assertEquals("num", result);
    }

    @Test
    void testEnumIteration() {
        int count = 0;
        for (EDataType type : EDataType.values()) {
            assertNotNull(type);
            count++;
        }
        assertEquals(6, count);
    }
}

class EReadModeTest {

    @Test
    void testEnumValues() {
        EReadMode[] values = EReadMode.values();

        assertEquals(3, values.length);
        assertEquals(EReadMode.READ, values[0]);
        assertEquals(EReadMode.SUBSCRIBE, values[1]);
        assertEquals(EReadMode.EVENTS, values[2]);
    }

    @Test
    void testValueOf() {
        assertEquals(EReadMode.READ, EReadMode.valueOf("READ"));
        assertEquals(EReadMode.SUBSCRIBE, EReadMode.valueOf("SUBSCRIBE"));
        assertEquals(EReadMode.EVENTS, EReadMode.valueOf("EVENTS"));
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            EReadMode.valueOf("INVALID");
        });
    }

    @Test
    void testValueOfNull() {
        assertThrows(NullPointerException.class, () -> {
            EReadMode.valueOf(null);
        });
    }

    @Test
    void testOrdinal() {
        assertEquals(0, EReadMode.READ.ordinal());
        assertEquals(1, EReadMode.SUBSCRIBE.ordinal());
        assertEquals(2, EReadMode.EVENTS.ordinal());
    }

    @Test
    void testName() {
        assertEquals("READ", EReadMode.READ.name());
        assertEquals("SUBSCRIBE", EReadMode.SUBSCRIBE.name());
        assertEquals("EVENTS", EReadMode.EVENTS.name());
    }

    @Test
    void testToString() {
        assertEquals("READ", EReadMode.READ.toString());
        assertEquals("SUBSCRIBE", EReadMode.SUBSCRIBE.toString());
        assertEquals("EVENTS", EReadMode.EVENTS.toString());
    }

    @Test
    void testEnumEquality() {
        EReadMode read1 = EReadMode.READ;
        EReadMode read2 = EReadMode.READ;
        EReadMode subscribe = EReadMode.SUBSCRIBE;

        assertEquals(read1, read2);
        assertSame(read1, read2);
        assertNotEquals(read1, subscribe);
    }

    @Test
    void testHashCode() {
        EReadMode read1 = EReadMode.READ;
        EReadMode read2 = EReadMode.READ;
        EReadMode subscribe = EReadMode.SUBSCRIBE;

        assertEquals(read1.hashCode(), read2.hashCode());
        assertNotEquals(read1.hashCode(), subscribe.hashCode());
    }

    @Test
    void testSwitchStatement() {
        EReadMode mode = EReadMode.SUBSCRIBE;
        String result = "";

        switch (mode) {
            case READ:
                result = "read";
                break;
            case SUBSCRIBE:
                result = "subscribe";
                break;
            case EVENTS:
                result = "events";
                break;
        }

        assertEquals("subscribe", result);
    }

    @Test
    void testEnumIteration() {
        int count = 0;
        for (EReadMode mode : EReadMode.values()) {
            assertNotNull(mode);
            assertNotNull(mode.name());
            assertNotNull(mode.toString());
            assertTrue(mode.ordinal() >= 0);
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    void testCompareTo() {
        assertTrue(EReadMode.READ.compareTo(EReadMode.SUBSCRIBE) < 0);
        assertTrue(EReadMode.SUBSCRIBE.compareTo(EReadMode.READ) > 0);
        assertEquals(0, EReadMode.READ.compareTo(EReadMode.READ));
        assertTrue(EReadMode.READ.compareTo(EReadMode.EVENTS) < 0);
    }

    @Test
    void testEnumClass() {
        assertEquals(EReadMode.class, EReadMode.READ.getDeclaringClass());
        assertEquals(EReadMode.class, EReadMode.SUBSCRIBE.getDeclaringClass());
        assertEquals(EReadMode.class, EReadMode.EVENTS.getDeclaringClass());
    }
}
