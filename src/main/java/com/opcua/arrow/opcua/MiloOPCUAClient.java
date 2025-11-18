package com.opcua.arrow.opcua;

import com.opcua.arrow.config.OPCUAClientConfig;
import com.opcua.arrow.interfaces.IOPCUAClient;
import com.opcua.arrow.interfaces.IRetryPolicy;
import com.opcua.arrow.interfaces.OPCUAValue;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * OPC-UA client implementation using Eclipse Milo with auto-reconnect and keep-alive.
 *
 * @param <T> The type of values to read
 */
public class MiloOPCUAClient<T> implements IOPCUAClient<T> {
    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUAClient.class);
    
    private final OPCUAClientConfig config;
    private final List<String> nodeIds;
    private final Class<T> valueType;
    private final IRetryPolicy retryPolicy;
    
    private OpcUaClient client;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private ScheduledExecutorService keepAliveExecutor;
    private UaSubscription keepAliveSubscription;
    
    public MiloOPCUAClient(OPCUAClientConfig config, List<String> nodeIds, 
                          Class<T> valueType, IRetryPolicy retryPolicy) {
        this.config = config;
        this.nodeIds = nodeIds;
        this.valueType = valueType;
        this.retryPolicy = retryPolicy;
    }
    
    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
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
                client = OpcUaClient.create(clientConfig);
                client.connect().get();
                
                connected.set(true);
                
                // Setup keep-alive mechanism
                setupKeepAlive();
                
                // Setup reconnection handler
                setupReconnectionHandler();
                
                logger.info("Connected to OPC-UA server: {}", config.getServerUrl());
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to OPC-UA server", e);
            }
        });
    }
    
    private void setupKeepAlive() {
        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdown();
        }
        
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("OPC-UA-KeepAlive");
            thread.setDaemon(true);
            return thread;
        });
        
        // Schedule keep-alive pings
        keepAliveExecutor.scheduleWithFixedDelay(
            this::sendKeepAlive,
            config.getKeepAliveInterval().toMillis(),
            config.getKeepAliveInterval().toMillis(),
            TimeUnit.MILLISECONDS
        );
    }
    
    private void sendKeepAlive() {
        if (!connected.get() || client == null) {
            return;
        }
        
        try {
            // Read server status to keep connection alive
            client.readValue(0, TimestampsToReturn.Neither, 
                Identifiers.Server_ServerStatus_State).get();
        } catch (Exception e) {
            logger.warn("Keep-alive ping failed: {}", e.getMessage());
            handleConnectionLoss();
        }
    }
    
    private void setupReconnectionHandler() {
        client.addSessionActivityListener(new MiloSessionActivityListener());
    }
    
    private void handleConnectionLoss() {
        if (!connected.compareAndSet(true, false)) {
            return; // Already handling disconnection
        }
        
        logger.warn("Connection lost, attempting to reconnect...");
        
        if (shouldReconnect.get()) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000); // Wait before reconnecting
                    reconnect();
                } catch (Exception e) {
                    logger.error("Reconnection failed", e);
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
                try {
                    // Clean up old client
                    if (client != null) {
                        try {
                            client.disconnect().get(5, TimeUnit.SECONDS);
                        } catch (Exception ignored) {
                            // Best effort cleanup
                        }
                    }
                    
                    // Reconnect
                    connect().get();
                    logger.info("Successfully reconnected to OPC-UA server");
                } catch (Exception e) {
                    throw new RuntimeException("Reconnection attempt failed", e);
                }
            });
        }).exceptionally(throwable -> {
            logger.error("All reconnection attempts failed", throwable);
            return null;
        });
    }
    
    @Override
    public CompletableFuture<List<OPCUAValue<T>>> read() {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Not connected to OPC-UA server"));
        }
        
        return retryPolicy.executeWithRetry(() -> readInternal());
    }
    
    private CompletableFuture<List<OPCUAValue<T>>> readInternal() {
        List<NodeId> nodeIdList = nodeIds.stream()
            .map(NodeId::parse)
            .collect(Collectors.toList());
        
        List<ReadValueId> readValueIds = nodeIdList.stream()
            .map(nodeId -> new ReadValueId(
                nodeId,
                AttributeId.Value.uid(),
                null,
                QualifiedName.NULL_VALUE
            ))
            .collect(Collectors.toList());
        
        return client.read(0, TimestampsToReturn.Both, readValueIds)
            .thenApply(response -> {
                DataValue[] results = response.getResults();
                List<OPCUAValue<T>> opcValues = new ArrayList<>();
                
                for (DataValue dataValue : results) {
                    opcValues.add(convertDataValue(dataValue));
                }
                
                return opcValues;
            });
    }
    
    @SuppressWarnings("unchecked")
    private OPCUAValue<T> convertDataValue(DataValue dataValue) {
        DateTime sourceTime = dataValue.getSourceTime();
        DateTime serverTime = dataValue.getServerTime();
        
        Instant sourceTimestamp = sourceTime != null 
            ? Instant.ofEpochSecond(sourceTime.getJavaTime() / 1000, 
                (int) ((sourceTime.getJavaTime() % 1000) * 1_000_000))
            : null;
        
        Instant serverTimestamp = serverTime != null
            ? Instant.ofEpochSecond(serverTime.getJavaTime() / 1000,
                (int) ((serverTime.getJavaTime() % 1000) * 1_000_000))
            : null;
        
        T value = null;
        if (dataValue.getValue() != null && dataValue.getValue().getValue() != null) {
            try {
                Object rawValue = dataValue.getValue().getValue();
                if (valueType.isInstance(rawValue)) {
                    value = (T) rawValue;
                } else {
                    // Try to convert
                    value = convertValue(rawValue);
                }
            } catch (Exception e) {
                logger.warn("Failed to convert value: {}", e.getMessage());
            }
        }
        
        int statusCode = dataValue.getStatusCode() != null 
            ? (int) dataValue.getStatusCode().getValue()
            : StatusCodes.Bad;
        
        return new OPCUAValue<>(sourceTimestamp, serverTimestamp, value, statusCode);
    }
    
    @SuppressWarnings("unchecked")
    private T convertValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        
        if (valueType == Double.class) {
            if (rawValue instanceof Number) {
                return (T) Double.valueOf(((Number) rawValue).doubleValue());
            }
        } else if (valueType == Float.class) {
            if (rawValue instanceof Number) {
                return (T) Float.valueOf(((Number) rawValue).floatValue());
            }
        } else if (valueType == Boolean.class) {
            return (T) Boolean.valueOf(rawValue.toString());
        } else if (valueType == String.class) {
            return (T) rawValue.toString();
        } else if (valueType == Integer.class) {
            if (rawValue instanceof Number) {
                return (T) Integer.valueOf(((Number) rawValue).intValue());
            }
        } else if (valueType == Long.class) {
            if (rawValue instanceof Number) {
                return (T) Long.valueOf(((Number) rawValue).longValue());
            }
        }
        
        throw new IllegalArgumentException("Cannot convert " + rawValue.getClass() + " to " + valueType);
    }
    
    @Override
    public CompletableFuture<Void> disconnect() {
        shouldReconnect.set(false);
        
        return CompletableFuture.runAsync(() -> {
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
            
            if (client != null && connected.get()) {
                try {
                    client.disconnect().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("Error during disconnect: {}", e.getMessage());
                }
            }
            
            connected.set(false);
            logger.info("Disconnected from OPC-UA server");
        });
    }
    
    @Override
    public boolean isConnected() {
        return connected.get() && client != null;
    }
    
    @Override
    public void close() throws Exception {
        disconnect().get();
    }
    
    private class MiloSessionActivityListener implements org.eclipse.milo.opcua.sdk.client.SessionActivityListener {
        @Override
        public void onSessionInactive(OpcUaClient client) {
            logger.warn("OPC-UA session became inactive");
            handleConnectionLoss();
        }
        
        @Override
        public void onSessionActive(OpcUaClient client) {
            logger.info("OPC-UA session is active");
            connected.set(true);
        }
    }
}
