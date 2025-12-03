package com.opcua_arrow.batch_builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opcua_arrow.data.TSValue;

class AcumJsonBufferBuilderTest {

    private AcumJsonBufferBuilder builder;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (builder != null) {
            builder.close();
        }
    }

    @Test
    void testFlush_ReturnsNull_WhenSizeThresholdNotMet() {
        // Given: minBatchSize=100, minFlushInterval=1 second
        builder = new AcumJsonBufferBuilder("test-source", 100, TimeUnit.SECONDS.toNanos(1));
        TSValue tsValue = new TSValue(1, 1000L, "ab", true, null);
        builder.appendList(List.of(tsValue));

        // When: size is only 2 bytes (less than 100)
        byte[] result = builder.flush();

        // Then
        assertNull(result);
    }

    @Test
    void testFlush_ReturnsData_WhenSizeThresholdMet() throws Exception {
        // Given: minBatchSize=10, minFlushInterval=1 hour (won't trigger)
        builder = new AcumJsonBufferBuilder("test-source", 10, TimeUnit.HOURS.toNanos(1));

        // Build a string with 10+ characters
        TSValue tsValue = new TSValue(1, 1000L, "0123456789", true, null);
        builder.appendList(List.of(tsValue));

        // When: size >= 10
        byte[] result = builder.flush();

        // Then
        assertNotNull(result);
        List<?> deserialized = mapper.readValue(result, List.class);
        assertEquals(2, deserialized.size()); // source + 1 value
    }

    @Test
    void testFlush_ReturnsData_WhenTimeThresholdMet() throws Exception {
        // Given: minBatchSize=1000 (won't trigger), minFlushInterval=100ms
        builder = new AcumJsonBufferBuilder("test-source", 1000, TimeUnit.MILLISECONDS.toNanos(100));

        TSValue tsValue = new TSValue(1, 1000L, "ab", true, null);
        builder.appendList(List.of(tsValue));

        // When: wait for time threshold
        Thread.sleep(150); // Wait 150ms (more than 100ms threshold)
        byte[] result = builder.flush();

        // Then
        assertNotNull(result);
    }

    @Test
    void testFlush_Accumulation_MultipleCallsBeforeThreshold() {
        // Given: minBatchSize=100, minFlushInterval=1 hour
        builder = new AcumJsonBufferBuilder("test-source", 100, TimeUnit.HOURS.toNanos(1));

        // When: add values below threshold
        TSValue ts1 = new TSValue(1, 1000L, "abc", true, null);
        builder.appendList(List.of(ts1));
        byte[] result1 = builder.flush();

        TSValue ts2 = new TSValue(2, 2000L, "def", true, null);
        builder.appendList(List.of(ts2));
        byte[] result2 = builder.flush();

        // Then: both should return null (accumulating)
        assertNull(result1);
        assertNull(result2);

        // Size should accumulate
        assertEquals(6, builder.size()); // "abc" + "def"
    }

    @Test
    void testFlush_ResetsTimer_AfterSuccessfulFlush() throws Exception {
        // Given: minBatchSize=5, minFlushInterval=100ms
        builder = new AcumJsonBufferBuilder("test-source", 5, TimeUnit.MILLISECONDS.toNanos(100));

        // First flush (size threshold)
        TSValue ts1 = new TSValue(1, 1000L, "12345", true, null);
        builder.appendList(List.of(ts1));
        byte[] result1 = builder.flush();
        assertNotNull(result1);

        // Add small value immediately after
        TSValue ts2 = new TSValue(2, 2000L, "ab", true, null);
        builder.appendList(List.of(ts2));
        byte[] result2 = builder.flush();
        assertNull(result2); // Timer was reset, shouldn't flush yet

        // Wait for new time threshold
        Thread.sleep(150);
        byte[] result3 = builder.flush();
        assertNotNull(result3); // Now time threshold is met again
    }

    @Test
    void testFlush_BothThresholds_SizeWins() throws Exception {
        // Given: both thresholds are configured
        builder = new AcumJsonBufferBuilder("test-source", 10, TimeUnit.MILLISECONDS.toNanos(100));

        // When: size threshold is met before time
        TSValue tsValue = new TSValue(1, 1000L, "0123456789", true, null);
        builder.appendList(List.of(tsValue));
        byte[] result = builder.flush();

        // Then: should flush immediately due to size
        assertNotNull(result);
    }

    @Test
    void testFlush_EmptyBuffer_ReturnsNull() {
        // Given: empty buffer
        builder = new AcumJsonBufferBuilder("test-source", 10, TimeUnit.SECONDS.toNanos(1));

        // When
        byte[] result = builder.flush();

        // Then
        assertNull(result);
    }

    @Test
    void testMultipleCycles_WithAccumulation() throws Exception {
        // Given: minBatchSize=10
        builder = new AcumJsonBufferBuilder("test-source", 10, TimeUnit.HOURS.toNanos(1));

        // First cycle: accumulate below threshold
        TSValue ts1 = new TSValue(1, 1000L, "abc", true, null);
        builder.appendList(List.of(ts1));
        assertNull(builder.flush());

        TSValue ts2 = new TSValue(2, 2000L, "def", true, null);
        builder.appendList(List.of(ts2));
        assertNull(builder.flush());

        // Third addition crosses threshold
        TSValue ts3 = new TSValue(3, 3000L, "ghij", true, null);
        builder.appendList(List.of(ts3));
        byte[] result = builder.flush();

        // Then: should have accumulated all 3 values
        assertNotNull(result);
        List<?> deserialized = mapper.readValue(result, List.class);
        assertEquals(4, deserialized.size()); // source + 3 values

        // Buffer should be cleared
        assertEquals(0, builder.size());
    }

    @Test
    void testClose_WithAccumulatedData() {
        // Given: data below threshold
        builder = new AcumJsonBufferBuilder("test-source", 100, TimeUnit.HOURS.toNanos(1));
        TSValue tsValue = new TSValue(1, 1000L, "data", true, null);
        builder.appendList(List.of(tsValue));

        // When
        builder.close();

        // Then: should clear buffer
        assertEquals(0, builder.size());
    }

    @Test
    void testFlush_VerySmallTimeThreshold() throws Exception {
        // Given: very small time threshold (1 nanosecond)
        builder = new AcumJsonBufferBuilder("test-source", 1000, 1L);

        TSValue tsValue = new TSValue(1, 1000L, "x", true, null);
        builder.appendList(List.of(tsValue));

        // When: even tiny delay should trigger
        Thread.sleep(1);
        byte[] result = builder.flush();

        // Then
        assertNotNull(result);
    }

    @Test
    void testSize_InheritedFromParent() {
        // Given
        builder = new AcumJsonBufferBuilder("test-source", 100, TimeUnit.SECONDS.toNanos(1));

        TSValue ts1 = new TSValue(1, 1000L, "abc", true, null);
        TSValue ts2 = new TSValue(2, 2000L, "defgh", true, null);
        builder.appendList(List.of(ts1, ts2));

        // When
        int size = builder.size();

        // Then: should correctly calculate size from parent
        assertEquals(8, size); // "abc"(3) + "defgh"(5)
    }
}
