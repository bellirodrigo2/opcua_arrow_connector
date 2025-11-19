package com.opcua_arrow.arrow;

import com.opcua_arrow.interfaces.IOPCUADataValue;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test implementation of IOPCUADataValue for testing purposes.
 */
class MockDataValue<T> implements IOPCUADataValue<T> {
    private final Instant sourceTimestamp;
    private final Instant serverTimestamp;
    private final T value;
    private final int statusCode;

    public MockDataValue(Instant sourceTimestamp, Instant serverTimestamp, T value, int statusCode) {
        this.sourceTimestamp = sourceTimestamp;
        this.serverTimestamp = serverTimestamp;
        this.value = value;
        this.statusCode = statusCode;
    }


    
    @Override
    public String getNodeId() {
        return null;
    }

    @Override
    public Integer getPointId() {
        return null;
    }

    @Override
    public Instant getSourceTimestamp() {
        return sourceTimestamp;
    }

    @Override
    public Instant getServerTimestamp() {
        return serverTimestamp;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public boolean isGood() {
        return (statusCode & 0x80000000) == 0;
    }
}

class ArrowAdapterTest {

    private List<String> nodeIds;
    private Map<String, Integer> idLookup;
    
    @BeforeEach
    void setUp() {
        nodeIds = Arrays.asList("node1", "node2", "node3");
        idLookup = new HashMap<>();
        idLookup.put("node1", 1);
        idLookup.put("node2", 2);
        idLookup.put("node3", 3);
    }

    @Test
    void testFloatArrowAdapterBuilder() throws Exception {
        try (ArrowAdapter<Double> adapter = new ArrowAdapter<>(nodeIds, Double.class, idLookup)) {
            
            List<IOPCUADataValue<Double>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, 3.14, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, 2.71, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, null, 0x80000000) // bad status
            );
            
            byte[] result = adapter.toArrowIPC(data);
            
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, adapter.size()); // Should be empty after flush
            
            // Test schema
            Schema schema = adapter.getSchema();
            assertNotNull(schema);
            assertEquals(4, schema.getFields().size()); // pointid, timestamp, value, statuscode
        }
    }

    @Test
    void testIntArrowAdapterBuilder() throws Exception {
        try (ArrowAdapter<Integer> adapter = new ArrowAdapter<>(nodeIds, Integer.class, idLookup)) {
            
            List<IOPCUADataValue<Integer>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, 42, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, -123, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, null, 0x80000000) // bad status
            );
            
            byte[] result = adapter.toArrowIPC(data);
            
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, adapter.size()); // Should be empty after flush
        }
    }

    @Test
    void testBoolArrowAdapterBuilder() throws Exception {
        try (ArrowAdapter<Boolean> adapter = new ArrowAdapter<>(nodeIds, Boolean.class, idLookup)) {
            
            List<IOPCUADataValue<Boolean>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, true, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, false, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, null, 0x80000000) // bad status
            );
            
            byte[] result = adapter.toArrowIPC(data);
            
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, adapter.size()); // Should be empty after flush
        }
    }

    @Test
    void testStringArrowAdapterBuilder() throws Exception {
        try (ArrowAdapter<String> adapter = new ArrowAdapter<>(nodeIds, String.class, idLookup)) {
            
            List<IOPCUADataValue<String>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, "hello", 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, "world", 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, null, 0x80000000) // bad status
            );
            
            byte[] result = adapter.toArrowIPC(data);
            
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, adapter.size()); // Should be empty after flush
        }
    }

    @Test
    void testWithoutIdLookup() throws Exception {
        // Test without ID lookup (string pointids)
        try (ArrowAdapter<Double> adapter = new ArrowAdapter<>(nodeIds, Double.class, null)) {
            
            List<IOPCUADataValue<Double>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, 3.14, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, 2.71, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, 1.41, 0)
            );
            
            byte[] result = adapter.toArrowIPC(data);
            
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, adapter.size()); // Should be empty after flush
        }
    }

    @Test
    void testCustomCapacityAndCompression() throws Exception {
        try (ArrowAdapter<Integer> adapter = new ArrowAdapter<>(nodeIds, Integer.class, idLookup, 50, true)) {
            
            assertEquals(50, adapter.capacity());
            assertEquals(0, adapter.size());
            
            List<IOPCUADataValue<Integer>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, 1, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, 2, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, 3, 0)
            );
            
            byte[] result = adapter.toArrowIPC(data);
            
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, adapter.size()); // Should be empty after flush
        }
    }

    @Test
    void testMultipleFlushes() throws Exception {
        try (ArrowAdapter<Integer> adapter = new ArrowAdapter<>(nodeIds, Integer.class, idLookup)) {
            
            // First batch
            List<IOPCUADataValue<Integer>> batch1 = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, 1, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, 2, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, 3, 0)
            );
            
            byte[] result1 = adapter.toArrowIPC(batch1);
            assertNotNull(result1);
            assertEquals(0, adapter.size()); // Should be empty after flush
            
            // Second batch (should reuse the same builder)
            List<IOPCUADataValue<Integer>> batch2 = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(4000), null, 4, 0),
                new MockDataValue<>(Instant.ofEpochSecond(5000), null, 5, 0),
                new MockDataValue<>(Instant.ofEpochSecond(6000), null, 6, 0)
            );
            
            byte[] result2 = adapter.toArrowIPC(batch2);
            assertNotNull(result2);
            assertEquals(0, adapter.size()); // Should be empty after flush
            
            // Results should be different (different data)
            assertFalse(Arrays.equals(result1, result2));
        }
    }

    @Test
    void testClearMethod() throws Exception {
        try (ArrowAdapter<Integer> adapter = new ArrowAdapter<>(nodeIds, Integer.class, idLookup)) {
            
            List<IOPCUADataValue<Integer>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, 1, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, 2, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, 3, 0)
            );
            
            adapter.toArrowIPC(data);
            assertEquals(0, adapter.size()); // Should be empty after flush
            
            // Clear should still work even when already empty
            adapter.clear();
            assertEquals(0, adapter.size());
        }
    }

    @Test
    void testInvalidDataSize() throws Exception {
        try (ArrowAdapter<Integer> adapter = new ArrowAdapter<>(nodeIds, Integer.class, idLookup)) {
            
            // Wrong number of data points
            List<IOPCUADataValue<Integer>> data = Arrays.asList(
                new MockDataValue<>(Instant.ofEpochSecond(1000), null, 1, 0),
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, 2, 0)
                // Missing node3
            );
            
            assertThrows(IllegalArgumentException.class, () -> {
                adapter.toArrowIPC(data);
            });
        }
    }

    @Test
    void testUnsupportedType() {
        // Test with an unsupported type
        assertThrows(IllegalArgumentException.class, () -> {
            try (ArrowAdapter<Object> adapter = new ArrowAdapter<>(nodeIds, Object.class, idLookup)) {
                // Should fail during construction
            }
        });
    }

    @Test
    void testNullTimestamp() throws Exception {
        try (ArrowAdapter<Integer> adapter = new ArrowAdapter<>(nodeIds, Integer.class, idLookup)) {
            
            List<IOPCUADataValue<Integer>> data = Arrays.asList(
                new MockDataValue<>(null, null, 1, 0), // null timestamp
                new MockDataValue<>(Instant.ofEpochSecond(2000), null, 2, 0),
                new MockDataValue<>(Instant.ofEpochSecond(3000), null, 3, 0)
            );
            
            byte[] result = adapter.toArrowIPC(data);
            
            assertNotNull(result);
            assertTrue(result.length > 0);
            assertEquals(0, adapter.size()); // Should be empty after flush
        }
    }

}