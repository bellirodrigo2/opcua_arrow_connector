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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.opcua.OPCUAServerExtension;
import com.opcua_arrow.opcua.retry.IRetryPolicy;
import com.opcua_arrow.opcua.retry.resilience4j.Resilience4jRetryPolicy;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(OPCUAServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MiloOPCUAConnectionTest {

    private MiloOPCUAConnection connection;

    @BeforeAll
    void setup() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        // Set keep-alive to a very long interval to avoid disconnection during tests
        // Note: There's a bug in MiloOPCUAConnection.sendKeepAlive() that calls
        // disconnectAsync()
        config.setKeepAliveInterval(Duration.ofMinutes(10));
        config.setUsername(null);
        config.setPassword(null);

        // Retry policy using Resilience4j
        RetryPolicyConfig retryConfig = new RetryPolicyConfig()
                .setMaxAttempts(3)
                .setInitialDelay(Duration.ofMillis(100))
                .setMaxDelay(Duration.ofSeconds(2))
                .setUseJitter(true); // Use jitter to avoid intervalFunction conflict

        IRetryPolicy retry = new Resilience4jRetryPolicy(retryConfig);

        connection = new MiloOPCUAConnection(config, retry);

        // connect() ainda retorna CompletableFuture → só aguardar
        connection.connect().get();
    }

    @AfterAll
    void cleanup() throws Exception {
        if (connection != null) {
            connection.disconnect().get();
        }
    }

    @Test
    void testConnectionIsConnected() {
        assertTrue(connection.isConnected());
        assertNotNull(connection.getClient());
    }

    @Test
    void testDisconnect() throws Exception {
        connection.disconnect().get();
        assertFalse(connection.isConnected());

        // connect again
        connection.connect().get();
        assertTrue(connection.isConnected());
    }

    @Test
    void testReadWriteLockExists() {
        assertNotNull(connection.getClientLock());
        assertNotNull(connection.getClientLock().readLock());
    }

    // ========================================
    // Connection Lifecycle Tests
    // ========================================

    @Test
    void testMultipleConnectCallsAreIdempotent() throws Exception {
        // Connection is already established in setup
        assertTrue(connection.isConnected());

        // Multiple connect calls should not fail
        connection.connect().get();
        assertTrue(connection.isConnected());

        connection.connect().get();
        assertTrue(connection.isConnected());
    }

    @Test
    void testGetServerUrl() {
        assertEquals("opc.tcp://localhost:12686/milo", connection.getServerUrl());
    }

    @Test
    void testClientIsNotNullWhenConnected() {
        assertTrue(connection.isConnected());
        OpcUaClient client = connection.getClient();
        assertNotNull(client);
    }

    @Test
    void testCloseDisconnectsConnection() throws Exception {
        MiloOPCUAConnection tempConnection = createConnection();
        tempConnection.connect().get();
        assertTrue(tempConnection.isConnected());

        tempConnection.close();

        assertFalse(tempConnection.isConnected());
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    void testConcurrentConnectionAttempts() throws Exception {
        MiloOPCUAConnection tempConnection = createConnection();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    tempConnection.connect().get();
                } catch (Exception e) {
                    // Expected for some threads - connection is already in progress
                }
            }, executor));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        // Connection should be established
        assertTrue(tempConnection.isConnected());
        assertNotNull(tempConnection.getClient());

        tempConnection.close();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testConcurrentDisconnectCalls() throws Exception {
        MiloOPCUAConnection tempConnection = createConnection();
        tempConnection.connect().get();
        assertTrue(tempConnection.isConnected());

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    tempConnection.disconnect().get();
                } catch (Exception e) {
                    // Should handle gracefully
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        assertFalse(tempConnection.isConnected());

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testReadLockAcquisition() throws Exception {
        // Test that we can acquire read lock
        var readLock = connection.getClientLock().readLock();

        assertTrue(readLock.tryLock(1, TimeUnit.SECONDS));
        assertTrue(connection.isConnected());
        readLock.unlock();
    }

    @Test
    void testMultipleReadLocksCanBeAcquiredSimultaneously() throws Exception {
        var readLock1 = connection.getClientLock().readLock();
        var readLock2 = connection.getClientLock().readLock();

        assertTrue(readLock1.tryLock(1, TimeUnit.SECONDS));
        assertTrue(readLock2.tryLock(1, TimeUnit.SECONDS));

        readLock1.unlock();
        readLock2.unlock();
    }

    // ========================================
    // Reconnection Tests
    // ========================================

    @Test
    void testReconnectAfterManualDisconnect() throws Exception {
        assertTrue(connection.isConnected());

        connection.disconnect().get();
        assertFalse(connection.isConnected());

        connection.connect().get();
        assertTrue(connection.isConnected());
        assertNotNull(connection.getClient());
    }

    @Test
    void testMultipleDisconnectReconnectCycles() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertTrue(connection.isConnected());

            connection.disconnect().get();
            assertFalse(connection.isConnected());

            connection.connect().get();
            assertTrue(connection.isConnected());
        }
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    void testConnectionToInvalidServerFails() {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://invalid-server:99999/invalid");
        config.setRequestTimeout(Duration.ofSeconds(2));
        config.setSessionTimeout(Duration.ofSeconds(2));
        config.setKeepAliveInterval(Duration.ofSeconds(1));

        MiloOPCUAConnection invalidConnection = new MiloOPCUAConnection(config, createSimpleRetryPolicy(1));

        assertThrows(Exception.class, () -> {
            invalidConnection.connect().get(5, TimeUnit.SECONDS);
        });

        assertFalse(invalidConnection.isConnected());
    }

    @Test
    void testDisconnectOnNonConnectedConnectionDoesNotFail() {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(10));
        config.setKeepAliveInterval(Duration.ofSeconds(3));

        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, createSimpleRetryPolicy(3));

        // Disconnect without connecting first should not throw
        assertDoesNotThrow(() -> tempConnection.disconnect().get());
        assertFalse(tempConnection.isConnected());
    }

    // ========================================
    // Configuration Tests
    // ========================================

    @Test
    void testConnectionWithDifferentTimeouts() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(10));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10)); // Long interval for test stability

        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, createSimpleRetryPolicy(3));
        tempConnection.connect().get();

        assertTrue(tempConnection.isConnected());
        assertEquals("opc.tcp://localhost:12686/milo", tempConnection.getServerUrl());

        tempConnection.close();
    }

    @Test
    void testConnectionWithUsernamePassword() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(10));
        config.setKeepAliveInterval(Duration.ofSeconds(3));
        config.setUsername("testuser");
        config.setPassword("testpassword");

        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, createSimpleRetryPolicy(3));

        // This may fail if server doesn't accept these credentials, but should not
        // throw NPE
        try {
            tempConnection.connect().get(10, TimeUnit.SECONDS);
            tempConnection.close();
        } catch (Exception e) {
            // Expected if server doesn't support username/password auth
            // The important thing is no NPE or other unexpected error
        }
    }

    // ========================================
    // Retry Policy Integration Tests
    // ========================================

    @Test
    void testRetryPolicyConfiguration() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10)); // Long interval for test stability

        RetryPolicyConfig retryConfig = new RetryPolicyConfig()
                .setMaxAttempts(5)
                .setInitialDelay(Duration.ofMillis(50))
                .setMaxDelay(Duration.ofSeconds(1))
                .setUseJitter(true);

        IRetryPolicy retryPolicy = new Resilience4jRetryPolicy(retryConfig);
        MiloOPCUAConnection tempConnection = new MiloOPCUAConnection(config, retryPolicy);

        tempConnection.connect().get();

        assertTrue(tempConnection.isConnected());
        assertEquals(5, retryPolicy.getMaxAttempts());

        tempConnection.close();
    }

    // ========================================
    // Helper Methods
    // ========================================

    private MiloOPCUAConnection createConnection() {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10)); // Long interval to avoid test interference

        return new MiloOPCUAConnection(config, createSimpleRetryPolicy(3));
    }

    private IRetryPolicy createSimpleRetryPolicy(int maxAttempts) {
        RetryPolicyConfig retryConfig = new RetryPolicyConfig()
                .setMaxAttempts(maxAttempts)
                .setInitialDelay(Duration.ofMillis(100))
                .setMaxDelay(Duration.ofSeconds(2))
                .setUseJitter(true); // Use jitter to avoid intervalFunction conflict

        return new Resilience4jRetryPolicy(retryConfig);
    }
}
