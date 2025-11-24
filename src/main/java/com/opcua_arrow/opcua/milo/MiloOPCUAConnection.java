package com.opcua_arrow.opcua.milo;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.retry.IRetryPolicy;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe OPC-UA connection implementation using Eclipse Milo with
 * auto-reconnect and keep-alive.
 * Handles only connection management, separate from reading operations.
 */
public class MiloOPCUAConnection implements IOPCUAConnection {
    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUAConnection.class);

    private final OPCUAClientConfig config;
    private final IRetryPolicy retryPolicy;

    // Thread-safe state management
    private volatile OpcUaClient client;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    // Synchronization for client operations
    private final ReadWriteLock clientLock = new ReentrantReadWriteLock();
    private final Object reconnectLock = new Object();

    // Keep-alive management with proper synchronization
    private volatile ScheduledExecutorService keepAliveExecutor;
    private final Object keepAliveLock = new Object();

    public MiloOPCUAConnection(OPCUAClientConfig config, IRetryPolicy retryPolicy) {
        this.config = config;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public CompletableFuture<Void> connect() {
        // Prevent multiple simultaneous connection attempts
        if (!connecting.compareAndSet(false, true)) {
            // Already connecting, wait for it to complete
            return CompletableFuture.runAsync(() -> {
                while (connecting.get() && !connected.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for connection", e);
                    }
                }
                if (!connected.get()) {
                    throw new RuntimeException("Connection failed");
                }
            });
        }

        return CompletableFuture.runAsync(() -> {
            clientLock.writeLock().lock();
            try {
                if (connected.get()) {
                    return;
                }

                // Discover endpoints
                List<EndpointDescription> endpoints = DiscoveryClient
                        .getEndpoints(config.getServerUrl())
                        .get();

                EndpointDescription endpoint = endpoints.stream()
                        .findFirst()
                        .orElseThrow(() -> new Exception("No endpoints available"));

                // Configure identity provider
                IdentityProvider identityProvider = config.getUsername() != null
                        ? new UsernameProvider(config.getUsername(), config.getPassword())
                        : new AnonymousProvider();

                // Build client configuration
                OpcUaClientConfig clientConfig = OpcUaClientConfig.builder()
                        .setEndpoint(endpoint)
                        .setIdentityProvider(identityProvider)
                        .setRequestTimeout(uint(config.getRequestTimeout().toMillis()))
                        .setSessionTimeout(uint(config.getSessionTimeout().toMillis()))
                        .build();

                // Create and connect client
                OpcUaClient newClient = OpcUaClient.create(clientConfig);
                newClient.connect().get();

                // Set client atomically
                this.client = newClient;
                connected.set(true);

                // Setup keep-alive mechanism
                setupKeepAlive();

                // Setup reconnection handler
                setupReconnectionHandler(newClient);

                logger.info("Connected to OPC-UA server: {}", config.getServerUrl());

            } catch (Exception e) {
                connected.set(false);
                throw new RuntimeException("Failed to connect to OPC-UA server", e);
            } finally {
                connecting.set(false);
                clientLock.writeLock().unlock();
            }
        });
    }

    private void setupKeepAlive() {
        synchronized (keepAliveLock) {
            if (keepAliveExecutor != null) {
                keepAliveExecutor.shutdown();
                try {
                    if (!keepAliveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        keepAliveExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    keepAliveExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("OPC-UA-KeepAlive-" + hashCode());
                thread.setDaemon(true);
                return thread;
            });

            keepAliveExecutor.scheduleWithFixedDelay(
                    this::sendKeepAlive,
                    config.getKeepAliveInterval().toMillis(),
                    config.getKeepAliveInterval().toMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

    private void sendKeepAlive() {
        clientLock.readLock().lock();
        try {
            if (!connected.get() || client == null) {
                return;
            }

            // Read server status to keep connection alive
            client.readValue(0, TimestampsToReturn.Neither,
                    Identifiers.Server_ServerStatus_State).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Keep-alive ping failed: {}", e.getMessage());
            handleConnectionLoss();
        } finally {
            clientLock.readLock().unlock();
        }
    }

    private void setupReconnectionHandler(OpcUaClient opcClient) {
        opcClient.addSessionActivityListener(new MiloSessionActivityListener());
    }

    private void handleConnectionLoss() {
        if (!connected.compareAndSet(true, false)) {
            return; // Already handling disconnection
        }

        logger.warn("Connection lost, attempting to reconnect...");

        if (shouldReconnect.get()) {
            CompletableFuture.runAsync(() -> {
                synchronized (reconnectLock) {
                    try {
                        Thread.sleep(1000); // Wait before reconnecting
                        reconnect();
                    } catch (Exception e) {
                        logger.error("Reconnection failed", e);
                    }
                }
            });
        }
    }

    private void reconnect() {
        if (!shouldReconnect.get()) {
            return;
        }

        retryPolicy.executeWithRetry(() -> {
            return CompletableFuture.runAsync(() -> {
                clientLock.writeLock().lock();
                try {
                    // Clean up old client
                    if (client != null) {
                        try {
                            client.disconnect().get(5, TimeUnit.SECONDS);
                        } catch (Exception ignored) {
                            // Best effort cleanup
                        }
                        client = null;
                    }

                    // Reset state
                    connected.set(false);
                    connecting.set(false);

                    // Reconnect
                    connect().get();
                    logger.info("Successfully reconnected to OPC-UA server");
                } catch (Exception e) {
                    throw new RuntimeException("Reconnection attempt failed", e);
                } finally {
                    clientLock.writeLock().unlock();
                }
            });
        }).exceptionally(throwable -> {
            logger.error("All reconnection attempts failed", throwable);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        shouldReconnect.set(false);

        return CompletableFuture.runAsync(() -> {
            synchronized (keepAliveLock) {
                if (keepAliveExecutor != null) {
                    keepAliveExecutor.shutdown();
                    try {
                        if (!keepAliveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                            keepAliveExecutor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        keepAliveExecutor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                    keepAliveExecutor = null;
                }
            }

            clientLock.writeLock().lock();
            try {
                if (client != null && connected.get()) {
                    try {
                        client.disconnect().get(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.warn("Error during disconnect: {}", e.getMessage());
                    }
                    client = null;
                }
                connected.set(false);
                connecting.set(false);
                logger.info("Disconnected from OPC-UA server");
            } finally {
                clientLock.writeLock().unlock();
            }
        });
    }

    @Override
    public boolean isConnected() {
        return connected.get() && client != null;
    }

    @Override
    public String getServerUrl() {
        return config.getServerUrl();
    }

    @Override
    public void close() throws Exception {
        disconnect().get();
    }

    @Override
    public OpcUaClient getClient() {
        return client;
    }

    @Override
    public ReadWriteLock getClientLock() {
        return clientLock;
    }

    private class MiloSessionActivityListener implements SessionActivityListener {

        public void onSessionInactive(OpcUaClient client) {
            logger.warn("OPC-UA session became inactive");
            handleConnectionLoss();
        }

        public void onSessionActive(OpcUaClient client) {
            logger.info("OPC-UA session is active");
            connected.set(true);
        }
    }
}
