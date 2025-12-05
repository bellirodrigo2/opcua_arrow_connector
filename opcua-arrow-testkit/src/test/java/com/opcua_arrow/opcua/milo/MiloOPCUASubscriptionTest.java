package com.opcua_arrow.opcua.milo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.opcua_arrow.ICallBack;
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

/**
 * Tests for MiloOPCUASubscription.
 *
 * Note: These tests focus on subscription management mechanics (creation,
 * lifecycle,
 * data point tracking) rather than data change notifications, because data
 * change
 * notifications depend on server-side sampling and can be timing-sensitive in
 * tests.
 *
 * Data change notification functionality should be verified through integration
 * tests
 * with a server that actively pushes changes.
 */
@ExtendWith(OPCUAServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MiloOPCUASubscriptionTest {

    private MiloOPCUAConnection connection;
    private IRetryPolicy retryPolicy;

    // Test data
    private DataWriteGroup writeGroup;
    private IDataPointEqual alwaysAccept;

    @BeforeAll
    void setup() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(10));
        config.setSessionTimeout(Duration.ofSeconds(120));
        config.setKeepAliveInterval(Duration.ofMinutes(10)); // Long interval to avoid bug

        RetryPolicyConfig retryConfig = new RetryPolicyConfig()
                .setMaxAttempts(3)
                .setInitialDelay(Duration.ofMillis(100))
                .setMaxDelay(Duration.ofSeconds(2))
                .setUseJitter(true);

        retryPolicy = new Resilience4jRetryPolicy(retryConfig);
        connection = new MiloOPCUAConnection(config, retryPolicy);

        // Connect to server
        connection.connect().get();

        // Setup test data
        writeGroup = new DataWriteGroup(EDataType.NUMERIC, new IntRange(0, 100));
        alwaysAccept = (newValue, newIsGood) -> true;
    }

    @AfterAll
    void cleanup() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    // ========================================
    // Subscription Creation Tests
    // ========================================

    @Test
    void testSubscriptionCanBeCreated() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        assertNotNull(subscription, "Subscription should be created");
    }

    @Test
    void testAddNodesToSubscriptionRegistersDataPoints() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, readGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        List<DataPoint> dataPoints = subscription.getDataPoints();

        // Cleanup
        subscription.closeSubscription(readGroup);

        assertNotNull(dataPoints);
        assertEquals(1, dataPoints.size(), "Should contain 1 data point");
        assertEquals(dp.getNodeId(), dataPoints.get(0).getNodeId(), "Node ID should match");
    }

    @Test
    void testGetDataPointsReturnsAllAddedPoints() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        DataPoint dp1 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, readGroup);
        DataPoint dp2 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 2, readGroup);
        DataPoint dp3 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Boolean", 3, readGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp1, dp2, dp3), handler);

        List<DataPoint> dataPoints = subscription.getDataPoints();

        // Cleanup
        subscription.closeSubscription(readGroup);

        assertNotNull(dataPoints);
        assertEquals(3, dataPoints.size(), "Should contain 3 data points");
    }

    // ========================================
    // Multiple Subscriptions Tests
    // ========================================

    @Test
    void testMultipleReadGroupsCreateSeparateSubscriptions() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup1 = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        DataReadGroup readGroup2 = new DataReadGroup(EReadMode.SUBSCRIBE, 2000L);

        DataPoint dp1 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, readGroup1);
        DataPoint dp2 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 2, readGroup2);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(readGroup1, List.of(dp1), handler);
        subscription.addNodesToSubscription(readGroup2, List.of(dp2), handler);

        List<DataPoint> dataPoints = subscription.getDataPoints();

        // Cleanup
        subscription.closeSubscription(readGroup1);
        subscription.closeSubscription(readGroup2);

        assertNotNull(dataPoints);
        assertEquals(2, dataPoints.size(), "Should contain 2 data points from different groups");
    }

    @Test
    void testSameReadGroupReusesSubscription() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);

        DataPoint dp1 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, readGroup);
        DataPoint dp2 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 2, readGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        // Add points in separate calls but with same read group
        subscription.addNodesToSubscription(readGroup, List.of(dp1), handler);
        subscription.addNodesToSubscription(readGroup, List.of(dp2), handler);

        List<DataPoint> dataPoints = subscription.getDataPoints();

        // Cleanup
        subscription.closeSubscription(readGroup);

        assertNotNull(dataPoints);
        assertEquals(2, dataPoints.size(), "Should contain 2 data points");
    }

    // ========================================
    // Subscription Lifecycle Tests
    // ========================================

    @Test
    void testCloseSubscriptionRemovesSubscription() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, readGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Close the subscription
        subscription.closeSubscription(readGroup);

        // Try to close again - should not throw
        subscription.closeSubscription(readGroup);
    }

    @Test
    void testCloseNonExistentSubscriptionDoesNotThrow() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 9999L);

        // Should not throw
        subscription.closeSubscription(readGroup);
    }

    @Test
    void testRemoveNodeFromSubscription() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        DataPoint dp1 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, readGroup);
        DataPoint dp2 = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Double", 2, readGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp1, dp2), handler);

        // Remove one node
        subscription.removeNodeFromSubscription(readGroup, List.of("ns=2;s=HelloWorld/ScalarTypes/Int32"));

        List<DataPoint> remaining = subscription.getDataPoints();

        // Cleanup
        subscription.closeSubscription(readGroup);

        assertEquals(1, remaining.size(), "Should have 1 remaining data point");
        assertEquals("ns=2;s=HelloWorld/ScalarTypes/Double", remaining.get(0).getNodeId(),
                "Remaining node should be Double");
    }

    @Test
    void testRemoveNodeFromNonExistentSubscriptionDoesNotThrow() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 8888L);

        // Should not throw, just log a warning
        subscription.removeNodeFromSubscription(readGroup, List.of("ns=2;s=NonExistent/Node"));
    }

    // ========================================
    // Data Point Tracking Tests
    // ========================================

    @Test
    void testDataPointsAreTrackedByNodeId() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        String nodeId = "ns=2;s=HelloWorld/ScalarTypes/Int32";
        DataPoint dp = createDataPoint(nodeId, 1, readGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        List<DataPoint> dataPoints = subscription.getDataPoints();

        // Cleanup
        subscription.closeSubscription(readGroup);

        assertTrue(dataPoints.stream().anyMatch(p -> p.getNodeId().equals(nodeId)),
                "Should find data point by node ID");
    }

    @Test
    void testAddingDuplicateNodeIdDoesNotDuplicate() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        String nodeId = "ns=2;s=HelloWorld/ScalarTypes/Int32";
        DataPoint dp1 = createDataPoint(nodeId, 1, readGroup);
        DataPoint dp2 = createDataPoint(nodeId, 2, readGroup); // Same node ID, different point ID

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp1), handler);
        subscription.addNodesToSubscription(readGroup, List.of(dp2), handler);

        List<DataPoint> dataPoints = subscription.getDataPoints();

        // Cleanup
        subscription.closeSubscription(readGroup);

        // Should only have one entry per node ID (putIfAbsent behavior)
        long count = dataPoints.stream().filter(p -> p.getNodeId().equals(nodeId)).count();
        assertEquals(1, count, "Should have only one entry per node ID");
    }

    // ========================================
    // Read Mode Tests
    // ========================================

    @Test
    void testSubscriptionModeReadGroup() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup subscribeGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 1000L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, subscribeGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(subscribeGroup, List.of(dp), handler);

        // Should complete without error
        subscription.closeSubscription(subscribeGroup);
    }

    @Test
    void testEventsReadModeReadGroup() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup eventsGroup = new DataReadGroup(EReadMode.EVENTS, 1000L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/ScalarTypes/Int32", 1, eventsGroup);

        Consumer<List<TSValue>> handler = values -> {
        };

        subscription.addNodesToSubscription(eventsGroup, List.of(dp), handler);

        // Should complete without error
        subscription.closeSubscription(eventsGroup);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private ICallBack createCountingCallback(AtomicInteger counter) {
        return new ICallBack() {
            @Override
            public ICallBackObject startCallback(String label, Collection<?> data) {
                counter.incrementAndGet();
                return new ICallBackObject() {
                    @Override
                    public void markFailure(Throwable t) {
                        // No-op
                    }

                    public void addKeyValue(String key, Object value) {
                        // No-op
                    }

                    @Override
                    public void close() {
                        // No-op
                    }
                };
            }
        };
    }

    private DataPoint createDataPoint(String nodeId, int pointId, DataReadGroup readGroup) {
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
