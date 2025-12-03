package com.opcua_arrow.batch_builder.arrow;

import static org.junit.jupiter.api.Assertions.*;

import com.opcua_arrow.batch_builder.arrow.columns.DoubleValueColumn;
import com.opcua_arrow.batch_builder.arrow.columns.IntegerValueColumn;
import com.opcua_arrow.data.TSValue;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

class AcumBatchArrowBuilderTest {

    @Test
    void testConstructor_WithThresholds() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        // When
        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 5, TimeUnit.SECONDS.toNanos(1))) {

            // Then
            assertNotNull(builder);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testFlush_BelowSizeThreshold_ReturnsNull() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 5, TimeUnit.HOURS.toNanos(1))) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null)
            );
            builder.appendList(values);
            assertEquals(2, builder.size());

            // When
            byte[] result = builder.flush();

            // Then
            assertNull(result, "Should return null when size < minBatchSize");
            assertEquals(2, builder.size(), "Size should not change when flush returns null");
        }
    }

    @Test
    void testFlush_MeetsSizeThreshold_ReturnsData() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 3, TimeUnit.HOURS.toNanos(1))) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null),
                    new TSValue(3, 3000000L, 30.5, true, null)
            );
            builder.appendList(values);
            assertEquals(3, builder.size());

            // When
            byte[] result = builder.flush();

            // Then
            assertNotNull(result, "Should return data when size >= minBatchSize");
            assertEquals(0, builder.size(), "Size should be 0 after successful flush");
        }
    }

    @Test
    void testFlush_ExceedsSizeThreshold_ReturnsData() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 2, TimeUnit.HOURS.toNanos(1))) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null),
                    new TSValue(3, 3000000L, 30.5, true, null),
                    new TSValue(4, 4000000L, 40.5, true, null)
            );
            builder.appendList(values);
            assertEquals(4, builder.size());

            // When
            byte[] result = builder.flush();

            // Then
            assertNotNull(result, "Should return data when size > minBatchSize");
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testFlush_MeetsTimeThreshold_ReturnsData() throws InterruptedException {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        // Use a very short time threshold for testing
        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 100, TimeUnit.MILLISECONDS.toNanos(50))) {

            List<TSValue> values = List.of(
                    new TSValue(1, 1000000L, 10.5, true, null)
            );
            builder.appendList(values);
            assertEquals(1, builder.size());

            // Wait for time threshold to pass
            Thread.sleep(60);

            // When
            byte[] result = builder.flush();

            // Then
            assertNotNull(result, "Should return data when time threshold is met");
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testFlush_EmptyBuffer_ReturnsNull() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 0, 0)) {

            // When
            byte[] result = builder.flush();

            // Then
            assertNull(result, "Should return null when buffer is empty");
        }
    }

    @Test
    void testFlush_AccumulationAcrossMultipleCalls() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 5, TimeUnit.HOURS.toNanos(1))) {

            // Add 2 values
            builder.appendList(List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null)
            ));

            // First flush - should return null
            byte[] result1 = builder.flush();
            assertNull(result1);
            assertEquals(2, builder.size());

            // Add 3 more values - total 5
            builder.appendList(List.of(
                    new TSValue(3, 3000000L, 30.5, true, null),
                    new TSValue(4, 4000000L, 40.5, true, null),
                    new TSValue(5, 5000000L, 50.5, true, null)
            ));
            assertEquals(5, builder.size());

            // Second flush - should return data
            byte[] result2 = builder.flush();
            assertNotNull(result2);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testFlush_TimerResetAfterSuccessfulFlush() throws InterruptedException {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 10, TimeUnit.MILLISECONDS.toNanos(100))) {

            // Add enough data to trigger size threshold
            for (int i = 0; i < 10; i++) {
                builder.appendList(List.of(new TSValue(i, i * 1000000L, i * 10.5, true, null)));
            }

            // Flush - should succeed due to size threshold
            byte[] result1 = builder.flush();
            assertNotNull(result1);

            // Add 1 value immediately after flush
            builder.appendList(List.of(new TSValue(100, 100000000L, 100.5, true, null)));

            // Try to flush immediately - should return null (timer was reset)
            byte[] result2 = builder.flush();
            assertNull(result2);

            // Wait for time threshold
            Thread.sleep(110);

            // Now flush should succeed
            byte[] result3 = builder.flush();
            assertNotNull(result3);
        }
    }

    @Test
    void testFlush_BothThresholdsConfigured_SizeWins() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 3, TimeUnit.HOURS.toNanos(1))) {

            // Add exactly minBatchSize items
            builder.appendList(List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null),
                    new TSValue(3, 3000000L, 30.5, true, null)
            ));

            // When - flush immediately without waiting for time threshold
            byte[] result = builder.flush();

            // Then - should flush based on size threshold
            assertNotNull(result);
        }
    }

    @Test
    void testFlush_MultipleFlushCycles() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 2, TimeUnit.HOURS.toNanos(1))) {

            // First cycle
            builder.appendList(List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null)
            ));
            byte[] batch1 = builder.flush();
            assertNotNull(batch1);
            assertEquals(0, builder.size());

            // Second cycle
            builder.appendList(List.of(
                    new TSValue(3, 3000000L, 30.5, true, null),
                    new TSValue(4, 4000000L, 40.5, true, null)
            ));
            byte[] batch2 = builder.flush();
            assertNotNull(batch2);
            assertEquals(0, builder.size());

            // Third cycle - below threshold
            builder.appendList(List.of(new TSValue(5, 5000000L, 50.5, true, null)));
            byte[] batch3 = builder.flush();
            assertNull(batch3);
            assertEquals(1, builder.size());
        }
    }

    @Test
    void testFlush_WithCompression() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, true, valueColumn, 2, TimeUnit.HOURS.toNanos(1))) {

            builder.appendList(List.of(
                    new TSValue(1, 1000000L, 10.5, true, null),
                    new TSValue(2, 2000000L, 20.5, true, null)
            ));

            // When
            byte[] compressedResult = builder.flush();

            // Then
            assertNotNull(compressedResult);
            assertTrue(compressedResult.length > 0);
        }
    }

    @Test
    void testWithIntegerValues() {
        // Given
        IntegerValueColumn valueColumn = new IntegerValueColumn();
        Schema schema = SchemaUtils.createSchema(Integer.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 2, TimeUnit.HOURS.toNanos(1))) {

            builder.appendList(List.of(
                    new TSValue(1, 1000000L, 100, true, null),
                    new TSValue(2, 2000000L, 200, true, null)
            ));

            // When
            byte[] result = builder.flush();

            // Then
            assertNotNull(result);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testReset_ClearsSizeButNotTimer() throws InterruptedException {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 100, TimeUnit.MILLISECONDS.toNanos(50))) {

            builder.appendList(List.of(new TSValue(1, 1000000L, 10.5, true, null)));
            assertEquals(1, builder.size());

            // Wait a bit
            Thread.sleep(60);

            // Reset clears size
            builder.reset();
            assertEquals(0, builder.size());

            // Add one more item
            builder.appendList(List.of(new TSValue(2, 2000000L, 20.5, true, null)));

            // Flush should succeed because timer threshold is still met
            byte[] result = builder.flush();
            assertNotNull(result);
        }
    }

    @Test
    void testZeroSizeThreshold_FlushesImmediately() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 0, TimeUnit.HOURS.toNanos(1))) {

            builder.appendList(List.of(new TSValue(1, 1000000L, 10.5, true, null)));

            // When - even with 1 item, should flush because minBatchSize is 0
            byte[] result = builder.flush();

            // Then
            assertNotNull(result);
            assertEquals(0, builder.size());
        }
    }

    @Test
    void testZeroTimeThreshold_FlushesImmediately() {
        // Given
        DoubleValueColumn valueColumn = new DoubleValueColumn();
        Schema schema = SchemaUtils.createSchema(Double.class);
        BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        try (AcumBatchArrowBuilder builder = new AcumBatchArrowBuilder(
                schema, 10, allocator, false, valueColumn, 100, 0)) {

            builder.appendList(List.of(new TSValue(1, 1000000L, 10.5, true, null)));

            // When - even with size below threshold, should flush because time threshold is 0
            byte[] result = builder.flush();

            // Then
            assertNotNull(result);
            assertEquals(0, builder.size());
        }
    }
}
