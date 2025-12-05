package com.opcua_arrow.opcua.milo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
 * Tests for MiloOPCUASubscription data change notifications.
 *
 * These tests verify that subscriptions receive notifications when node values
 * change.
 * The ExampleServer has a background thread that periodically updates the
 * subscription
 * test nodes (HelloWorld/SubscriptionTest/*), which triggers the
 * SubscriptionModel
 * to detect changes and send notifications to subscribers.
 */
@ExtendWith(OPCUAServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MiloOPCUASubscriptionDataNotificationTest {

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
        config.setKeepAliveInterval(Duration.ofMinutes(10));

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
    // Basic Data Notification Tests
    // ========================================

    @Test
    void testSubscriptionReceivesDataChangeNotification() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        // Use Dynamic/Int32 node which is known to generate changing values
        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L); // Fast sampling
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/Dynamic/Int32", 1, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<List<TSValue>> handler = values -> {
            System.out.println("[TEST] Received " + values.size() + " values");
            for (TSValue v : values) {
                System.out.println("[TEST] Value: id=" + v.id + ", value=" + v.value + ", isGood=" + v.isGood);
            }
            receivedValues.addAll(values);
            latch.countDown();
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Wait for notification - server generates random values on each sample
        boolean received = latch.await(10, TimeUnit.SECONDS);

        // Cleanup
        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive data change notification");
        assertFalse(receivedValues.isEmpty(), "Should have received values");

        TSValue tsValue = receivedValues.get(0);
        assertEquals(1, tsValue.id);
        assertTrue(tsValue.isGood);
        assertNotNull(tsValue.value);
        assertTrue(tsValue.value instanceof Integer, "Value should be Integer");
    }

    @Test
    void testSubscriptionReceivesMultipleNotifications() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Int32", 1, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(3); // Wait for 3 notifications

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            for (int i = 0; i < values.size(); i++) {
                latch.countDown();
            }
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server updates every 50ms, so we should receive multiple notifications
        boolean received = latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive multiple notifications");
        assertTrue(receivedValues.size() >= 3, "Should have at least 3 values");

        // Verify all values are good quality and have correct point ID
        for (TSValue tsv : receivedValues) {
            assertTrue(tsv.isGood);
            assertEquals(1, tsv.id);
        }
    }

    @Test
    void testSubscriptionToMultipleNodesReceivesNotifications() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp1 = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Int32", 1, readGroup);
        DataPoint dp2 = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Double", 2, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2); // One for each node

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            for (int i = 0; i < values.size(); i++) {
                latch.countDown();
            }
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp1, dp2), handler);

        // Server updates both nodes every 50ms
        boolean received = latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive notifications for both nodes");
        assertTrue(receivedValues.size() >= 2, "Should have at least 2 values");

        // Check both point IDs are present
        boolean hasPoint1 = receivedValues.stream().anyMatch(v -> v.id == 1);
        boolean hasPoint2 = receivedValues.stream().anyMatch(v -> v.id == 2);
        assertTrue(hasPoint1, "Should have notification for point 1");
        assertTrue(hasPoint2, "Should have notification for point 2");
    }

    // ========================================
    // Data Filtering Tests
    // ========================================

    @Test
    void testDataPointFilterRejectsValues() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);

        // Filter that only accepts values greater than 50
        // The server increments an Int32 counter starting from 0, so eventually it will
        // exceed 50
        IDataPointEqual filterGreaterThan50 = (newValue, newIsGood) -> {
            if (newValue instanceof Integer) {
                return (Integer) newValue > 50;
            }
            return true;
        };

        DataPoint dp = new DataPoint(
                "FilteredPoint",
                "Test point with filter",
                "ns=2;s=HelloWorld/SubscriptionTest/Int32",
                1,
                EDataType.NUMERIC,
                filterGreaterThan50,
                writeGroup,
                readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            if (!values.isEmpty()) {
                latch.countDown();
            }
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server increments counter every 50ms; wait for a value > 50
        boolean received = latch.await(10, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive notification for filtered value");

        // Should only have values > 50
        for (TSValue tsv : receivedValues) {
            assertTrue((Integer) tsv.value > 50, "Value should be > 50");
        }
    }

    @Test
    void testSubscriptionWithAlwaysAcceptFilter() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Int32", 1, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            for (int i = 0; i < values.size(); i++) {
                latch.countDown();
            }
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server updates every 50ms
        boolean received = latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive all notifications with always-accept filter");
        assertTrue(receivedValues.size() >= 2, "Should have at least 2 values");
    }

    // ========================================
    // Different Data Types Tests
    // ========================================

    @Test
    void testSubscriptionReceivesDoubleValueNotifications() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Double", 1, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            latch.countDown();
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server updates Double with sine wave values
        boolean received = latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive Double notification");
        assertFalse(receivedValues.isEmpty());
        assertTrue(receivedValues.get(0).value instanceof Double, "Value should be Double");
    }

    @Test
    void testSubscriptionReceivesBooleanValueNotifications() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Boolean", 1, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2); // Toggle twice

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            for (int i = 0; i < values.size(); i++) {
                latch.countDown();
            }
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server toggles Boolean every 50ms
        boolean received = latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive Boolean notifications");
        assertTrue(receivedValues.size() >= 2);

        // Verify we have boolean values
        for (TSValue tsv : receivedValues) {
            assertTrue(tsv.value instanceof Boolean, "Value should be Boolean");
        }
    }

    @Test
    void testSubscriptionReceivesStringValueNotifications() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/String", 1, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            latch.countDown();
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server updates String with "Update-N" pattern
        boolean received = latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received, "Should receive String notification");
        assertFalse(receivedValues.isEmpty());
        assertTrue(receivedValues.get(0).value instanceof String, "Value should be String");
        assertTrue(((String) receivedValues.get(0).value).startsWith("Update-"), "String should match pattern");
    }

    // ========================================
    // Callback Tests
    // ========================================

    @Test
    void testCallbackInvokedOnNotifications() throws Exception {
        AtomicInteger startCount = new AtomicInteger(0);
        AtomicInteger closeCount = new AtomicInteger(0);

        ICallBack countingCallback = new ICallBack() {
            @Override
            public ICallBackObject startCallback(String label, Collection<?> data) {
                startCount.incrementAndGet();
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
                        closeCount.incrementAndGet();
                    }
                };
            }
        };

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, countingCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Int32", 1, readGroup);

        CountDownLatch latch = new CountDownLatch(1);

        Consumer<List<TSValue>> handler = values -> {
            latch.countDown();
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server updates every 50ms
        latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(startCount.get() > 0, "Callback start should have been called");
        assertTrue(closeCount.get() > 0, "Callback close should have been called");
    }

    // ========================================
    // Timestamp Tests
    // ========================================

    @Test
    void testNotificationValuesHaveValidTimestamps() throws Exception {
        AtomicInteger callbackCount = new AtomicInteger(0);
        ICallBack testCallback = createCountingCallback(callbackCount);

        MiloOPCUASubscription subscription = new MiloOPCUASubscription(connection, testCallback);

        DataReadGroup readGroup = new DataReadGroup(EReadMode.SUBSCRIBE, 100L);
        DataPoint dp = createDataPoint("ns=2;s=HelloWorld/SubscriptionTest/Int32", 1, readGroup);

        List<TSValue> receivedValues = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<List<TSValue>> handler = values -> {
            receivedValues.addAll(values);
            latch.countDown();
        };

        subscription.addNodesToSubscription(readGroup, List.of(dp), handler);

        // Server updates every 50ms with timestamps
        boolean received = latch.await(5, TimeUnit.SECONDS);

        subscription.closeSubscription(readGroup);

        assertTrue(received);
        assertFalse(receivedValues.isEmpty());

        TSValue tsValue = receivedValues.get(0);
        assertTrue(tsValue.timestamp > 0, "Timestamp should be positive");
        assertTrue(tsValue.isConsistent(), "TSValue should be consistent");
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
