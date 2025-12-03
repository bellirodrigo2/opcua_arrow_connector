package com.opcua_arrow.batch_builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opcua_arrow.data.TSValue;

class JsonBufferBuilderTest {

    private JsonBufferBuilder builder;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        builder = new JsonBufferBuilder("test-source");
        mapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (builder != null) {
            builder.close();
        }
    }

    @Test
    void testAppendList_SingleValue() {
        // Given
        TSValue tsValue = new TSValue(1, 1000L, "value1", true, null);
        List<TSValue> values = List.of(tsValue);

        // When
        builder.appendList(values);
        byte[] result = builder.flush();

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testAppendList_MultipleValues() throws Exception {
        // Given
        TSValue ts1 = new TSValue(1, 1000L, "value1", true, null);
        TSValue ts2 = new TSValue(2, 2000L, "value2", true, null);
        TSValue ts3 = new TSValue(3, 3000L, "value3", true, null);
        List<TSValue> values = List.of(ts1, ts2, ts3);

        // When
        builder.appendList(values);
        byte[] result = builder.flush();

        // Then
        assertNotNull(result);
        List<?> deserialized = mapper.readValue(result, List.class);
        assertEquals(4, deserialized.size()); // 1 source + 3 values
    }

    @Test
    void testFlush_IncludesSourceName() throws Exception {
        // Given
        TSValue tsValue = new TSValue(1, 1000L, "test-value", true, null);
        builder.appendList(List.of(tsValue));

        // When
        byte[] result = builder.flush();

        // Then
        String json = new String(result);
        assertTrue(json.contains("test-source"));
    }

    @Test
    void testFlush_ClearsBuffer() {
        // Given
        TSValue tsValue = new TSValue(1, 1000L, "value1", true, null);
        builder.appendList(List.of(tsValue));

        // When
        builder.flush();
        int sizeAfterFlush = builder.size();

        // Then
        assertEquals(0, sizeAfterFlush);
    }

    @Test
    void testFlush_EmptyBuffer() throws Exception {
        // When
        byte[] result = builder.flush();

        // Then
        assertNotNull(result);
        List<?> deserialized = mapper.readValue(result, List.class);
        assertEquals(1, deserialized.size()); // Only source metadata
    }

    @Test
    void testSize_EmptyBuffer() {
        // When
        int size = builder.size();

        // Then
        assertEquals(0, size);
    }

    @Test
    void testSize_WithValues() {
        // Given
        TSValue ts1 = new TSValue(1, 1000L, "ab", true, null);
        TSValue ts2 = new TSValue(2, 2000L, "cde", true, null);
        builder.appendList(List.of(ts1, ts2));

        // When
        int size = builder.size();

        // Then
        assertEquals(5, size); // "ab" (2) + "cde" (3)
    }

    @Test
    void testClose_ClearsBuffer() {
        // Given
        TSValue tsValue = new TSValue(1, 1000L, "value", true, null);
        builder.appendList(List.of(tsValue));

        // When
        builder.close();
        int sizeAfterClose = builder.size();

        // Then
        assertEquals(0, sizeAfterClose);
    }

    @Test
    void testMultipleFlushCycles() throws Exception {
        // First cycle
        TSValue ts1 = new TSValue(1, 1000L, "value1", true, null);
        builder.appendList(List.of(ts1));
        byte[] result1 = builder.flush();

        // Second cycle
        TSValue ts2 = new TSValue(2, 2000L, "value2", true, null);
        builder.appendList(List.of(ts2));
        byte[] result2 = builder.flush();

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertFalse(Arrays.equals(result1, result2));
    }

    @Test
    void testAppendList_EmptyList() {
        // When
        builder.appendList(List.of());
        byte[] result = builder.flush();

        // Then
        assertNotNull(result);
        assertEquals(0, builder.size());
    }
}
