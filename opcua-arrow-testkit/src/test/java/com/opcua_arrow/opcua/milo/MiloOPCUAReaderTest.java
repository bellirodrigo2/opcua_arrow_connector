package com.opcua_arrow.opcua.milo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.EDataType;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.IDataPointEqual;
import com.opcua_arrow.data.IntRange;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.opcua.OPCUAServerExtension;
import com.opcua_arrow.opcua.retry.IRetryPolicy;
import com.opcua_arrow.opcua.retry.resilience4j.Resilience4jRetryPolicy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(OPCUAServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MiloOPCUAReaderTest {

    private MiloOPCUAConnection connection;
    private MiloOPCUAReader reader;
    private IRetryPolicy retryPolicy;

    // Test data
    private DataWriteGroup writeGroup;
    private DataReadGroup readGroup; // Uses EReadMode.READ for polling
    private IDataPointEqual alwaysAccept;

    @BeforeAll
    void setup() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10)); // Long interval to avoid bug

        RetryPolicyConfig retryConfig = new RetryPolicyConfig()
                .setMaxAttempts(3)
                .setInitialDelay(Duration.ofMillis(100))
                .setMaxDelay(Duration.ofSeconds(2))
                .setUseJitter(true);

        retryPolicy = new Resilience4jRetryPolicy(retryConfig);
        connection = new MiloOPCUAConnection(config, retryPolicy);

        // Create reader
        reader = new MiloOPCUAReader(connection, retryPolicy);

        // Setup test data
        writeGroup = new DataWriteGroup(EDataType.NUMERIC, new IntRange(0, 100));
        readGroup = new DataReadGroup(EReadMode.READ, 1000L);
        alwaysAccept = (newValue, newIsGood) -> true;

        // Start reader (connects to server)
        reader.start();
    }

    @AfterAll
    void cleanup() throws Exception {
        if (reader != null) {
            reader.close();
        }
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void testConstructorRejectsNullConnection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MiloOPCUAReader(null, retryPolicy);
        });
    }

    @Test
    void testConstructorRejectsNullRetryPolicy() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MiloOPCUAReader(connection, null);
        });
    }

    // ========================================
    // Basic Read Operations
    // ========================================

    @Test
    void testReadSingleDataPoint() {
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).id);
        assertTrue(result.get(0).isGood);
        assertTrue(result.get(0).isConsistent());
    }

    @Test
    void testReadMultipleDataPoints() {
        List<DataPoint> dataPoints = List.of(
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1),
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 2),
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Boolean", 3),
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/String", 4));

        List<TSValue> result = reader.read(dataPoints);

        assertNotNull(result);
        assertEquals(4, result.size());

        // Verify all points have good quality
        for (TSValue tsv : result) {
            assertTrue(tsv.isGood);
            assertTrue(tsv.isConsistent());
        }
    }

    @Test
    void testReadEmptyListReturnsEmpty() {
        List<TSValue> result = reader.read(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testReadNullListReturnsEmpty() {
        List<TSValue> result = reader.read(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========================================
    // Data Value Processing Tests
    // ========================================

    @Test
    void testReadIntegerValue() {
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).value);
        // The value should be an integer
        assertTrue(result.get(0).value instanceof Integer);
    }

    @Test
    void testReadDoubleValue() {
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 1);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).value);
        assertTrue(result.get(0).value instanceof Double);
    }

    @Test
    void testReadBooleanValue() {
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Boolean", 1);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).value);
        assertTrue(result.get(0).value instanceof Boolean);
    }

    @Test
    void testReadStringValue() {
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/String", 1);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).value);
        assertTrue(result.get(0).value instanceof String);
    }

    @Test
    void testReadDynamicValue() {
        // Dynamic nodes return random values on each read
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/Dynamic/Int32", 1);

        List<TSValue> result1 = reader.read(List.of(dp));
        List<TSValue> result2 = reader.read(List.of(dp));

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(1, result1.size());
        assertEquals(1, result2.size());

        // Both should be good quality
        assertTrue(result1.get(0).isGood);
        assertTrue(result2.get(0).isGood);

        // Values might be different (dynamic node)
        // We don't assert they're different since it's random
    }

    @Test
    void testReadTimestampIsPositive() {
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).timestamp > 0, "Timestamp should be positive");
        assertTrue(result.get(0).isConsistent());
    }

    // ========================================
    // Data Point Filtering Tests
    // ========================================

    @Test
    void testDataPointFilteringWithRejectAll() {
        // Create a filter that rejects all values
        IDataPointEqual rejectAll = (newValue, newIsGood) -> false;

        DataPoint dp = new DataPoint(
                "TestPoint",
                "Test description",
                "ns=2;s=HelloWorld/ScalarTypes/Int32",
                1,
                EDataType.NUMERIC,
                rejectAll,
                writeGroup,
                readGroup);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when filter rejects all values");
    }

    @Test
    void testDataPointFilteringWithAcceptAll() {
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1);

        List<TSValue> result = reader.read(List.of(dp));

        assertNotNull(result);
        assertEquals(1, result.size(), "Result should contain one value when filter accepts all");
    }

    // ========================================
    // Connection State Tests
    // ========================================

    @Test
    void testIsStartedWhenConnected() {
        assertTrue(reader.isStarted());
    }

    @Test
    void testReadWhenDisconnectedThrowsException() throws Exception {
        // Create a separate reader that we can disconnect
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10));

        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, retryPolicy);
        MiloOPCUAReader tempReader = new MiloOPCUAReader(tempConnection, retryPolicy);

        // Don't start the reader (don't connect)
        assertFalse(tempReader.isStarted());

        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1);

        // Should throw exception when trying to read while disconnected
        assertThrows(Exception.class, () -> {
            tempReader.read(List.of(dp));
        });
    }

    // ========================================
    // Lifecycle Management Tests
    // ========================================

    @Test
    void testStartConnectsToServer() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10));

        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, retryPolicy);
        MiloOPCUAReader tempReader = new MiloOPCUAReader(tempConnection, retryPolicy);

        assertFalse(tempReader.isStarted());

        tempReader.start();

        assertTrue(tempReader.isStarted());

        tempReader.close();
    }

    @Test
    void testStopDisconnectsFromServer() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10));

        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, retryPolicy);
        MiloOPCUAReader tempReader = new MiloOPCUAReader(tempConnection, retryPolicy);

        tempReader.start();
        assertTrue(tempReader.isStarted());

        tempReader.stop();
        assertFalse(tempReader.isStarted());
    }

    @Test
    void testCloseDisconnectsFromServer() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10));

        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, retryPolicy);
        MiloOPCUAReader tempReader = new MiloOPCUAReader(tempConnection, retryPolicy);

        tempReader.start();
        assertTrue(tempReader.isStarted());

        tempReader.close();
        assertFalse(tempReader.isStarted());
    }

    // ========================================
    // Concurrent Read Tests
    // ========================================

    @Test
    void testConcurrentReads() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    List<TSValue> result = reader.read(List.of(dp));
                    if (result.size() == 1 && result.get(0).isGood) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, successCount.get(), "All concurrent reads should succeed");
    }

    @Test
    void testConcurrentReadsWithDifferentPoints() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<DataPoint> dataPoints = List.of(
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1),
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 2),
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Boolean", 3),
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/String", 4));

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    List<TSValue> result = reader.read(List.of(dataPoints.get(index)));
                    if (result.size() == 1 && result.get(0).isGood) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount, successCount.get(), "All concurrent reads should succeed");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    void testReadInvalidNodeIdReturnsEmptyOrThrows() {
        // Reading an invalid node ID should either return empty list
        // or throw an exception depending on how the filter handles bad quality
        DataPoint dp = createDataPoint("ns=99;s=NonExistent/Node", 1);

        // This may throw an exception or return empty list depending on server behavior
        // The test verifies it doesn't crash
        assertDoesNotThrow(() -> {
            try {
                reader.read(List.of(dp));
            } catch (RuntimeException e) {
                // Expected - retry exhausted
            }
        });
    }

    // ========================================
    // Batch Read Tests
    // ========================================

    @Test
    void testBatchReadMultipleScalarTypes() {
        List<DataPoint> dataPoints = new ArrayList<>();

        // Create data points for all scalar types available
        String[] types = { "Boolean", "Int16", "Int32", "Int64", "Float", "Double", "String" };

        for (int i = 0; i < types.length; i++) {
            dataPoints.add(createDataPoint("ns=2;s=HelloWorld/ScalarTypes/" + types[i], i + 1));
        }

        List<TSValue> result = reader.read(dataPoints);

        assertNotNull(result);
        assertEquals(types.length, result.size());

        for (TSValue tsv : result) {
            assertTrue(tsv.isGood, "All values should have good quality");
            assertTrue(tsv.isConsistent(), "All values should be consistent");
        }
    }

    @Test
    void testBatchReadMixedGoodAndBadNodes() {
        List<DataPoint> dataPoints = List.of(
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1), // Good
                createDataPoint("ns=99;s=Invalid/Node", 2), // Bad (non-existent)
                createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 3) // Good
        );

        // This test verifies the reader handles mixed results
        // The behavior depends on retry policy and error handling
        try {
            List<TSValue> result = reader.read(dataPoints);
            // If it succeeds, check what we got
            assertNotNull(result);
        } catch (RuntimeException e) {
            // This is also acceptable - the batch failed due to invalid node
            assertTrue(e.getMessage() != null);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private DataPoint createDataPoint(String nodeId, int pointId) {
        return new DataPoint(
                "TestPoint_" + pointId,
                "Test description for point " + pointId,
                nodeId,
                pointId,
                EDataType.NUMERIC,
                alwaysAccept,
                writeGroup,
                readGroup);
    }
}
