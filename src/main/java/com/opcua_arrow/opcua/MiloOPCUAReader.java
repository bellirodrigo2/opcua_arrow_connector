package com.opcua_arrow.opcua;

import com.opcua_arrow.interfaces.IOPCUAConnection;
import com.opcua_arrow.interfaces.IOPCUAReader;
import com.opcua_arrow.interfaces.IRetryPolicy;
import com.opcua_arrow.interfaces.IOPCUADataValue;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

/**
 * OPC-UA reader implementation using Eclipse Milo.
 * Handles reading values from configured nodes using an encapsulated connection.
 *
 * @param <T> The type of values to read
 */
public class MiloOPCUAReader<T> implements IOPCUAReader<T> {
    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUAReader.class);
    
    private final List<String> nodeIds;
    private final Class<T> valueType;
    private final IRetryPolicy retryPolicy;
    private final IOPCUAConnection connection;
    
    public MiloOPCUAReader(List<String> nodeIds, Class<T> valueType, IOPCUAConnection connection, IRetryPolicy retryPolicy) {
        this.nodeIds = new ArrayList<>(nodeIds); // Defensive copy
        this.valueType = valueType;
        this.connection = connection;
        this.retryPolicy = retryPolicy;
    }
    
    @Override
    public CompletableFuture<List<IOPCUADataValue<T>>> read() {
        if (connection == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Connection is null"));
        }
        
        if (!connection.isConnected()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Reader is not started - call start() first"));
        }
        
        return retryPolicy.executeWithRetry(() -> readInternal());
    }
    
    @Override
    public CompletableFuture<Void> start() {
        if (connection == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Connection is null"));
        }
        
        return retryPolicy.executeWithRetry(() -> {
            return connection.connect()
                .thenCompose(v -> validateNodes());
        });
    }
    
    @Override
    public CompletableFuture<Void> stop() {
        if (connection == null) {
            return CompletableFuture.completedFuture(null);
        }
        return connection.disconnect();
    }
    
    @Override
    public boolean isStarted() {
        return connection != null && connection.isConnected();
    }
    
    private CompletableFuture<List<IOPCUADataValue<T>>> readInternal() {
        ReadWriteLock clientLock = connection.getClientLock();
        clientLock.readLock().lock();
        
        try {
            OpcUaClient client = connection.getClient();
            if (client == null || !connection.isConnected()) {
                throw new IllegalStateException("Client not connected");
            }
            
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
                    List<IOPCUADataValue<T>> dataValues = new ArrayList<>();
                    
                    for (DataValue dataValue : results) {
                        IOPCUADataValue<T> adaptedValue = new MiloDataValueAdapter<>(dataValue, valueType);
                        dataValues.add(adaptedValue);
                    }
                    
                    return dataValues;
                });
        } finally {
            clientLock.readLock().unlock();
        }
    }
    
    
    @Override
    public List<String> getNodeIds() {
        return new ArrayList<>(nodeIds); // Return defensive copy
    }
    
    @Override
    public Class<T> getValueType() {
        return valueType;
    }
    
    private CompletableFuture<Void> validateNodes() {
        ReadWriteLock clientLock = connection.getClientLock();
        clientLock.readLock().lock();
        
        try {
            OpcUaClient client = connection.getClient();
            if (client == null || !connection.isConnected()) {
                throw new IllegalStateException("Client not connected");
            }
            
            logger.info("Validating {} configured nodes...", nodeIds.size());
            
            List<NodeId> nodeIdList = nodeIds.stream()
                .map(NodeId::parse)
                .collect(Collectors.toList());
            
            List<ReadValueId> readValueIds = nodeIdList.stream()
                .map(nodeId -> new ReadValueId(
                    nodeId,
                    AttributeId.DataType.uid(),
                    null,
                    QualifiedName.NULL_VALUE
                ))
                .collect(Collectors.toList());
            
            return client.read(0, TimestampsToReturn.Neither, readValueIds)
                .thenApply(response -> {
                    DataValue[] results = response.getResults();
                    List<String> invalidNodes = new ArrayList<>();
                    
                    for (int i = 0; i < results.length; i++) {
                        DataValue dataValue = results[i];
                        String nodeId = nodeIds.get(i);
                        
                        if (dataValue.getStatusCode() == null || !dataValue.getStatusCode().isGood()) {
                            invalidNodes.add(nodeId);
                            logger.warn("Node {} validation failed with status: {}", 
                                nodeId, dataValue.getStatusCode());
                        } else {
                            logger.debug("Node {} validated successfully", nodeId);
                        }
                    }
                    
                    if (!invalidNodes.isEmpty()) {
                        throw new IllegalStateException(
                            "Node validation failed for " + invalidNodes.size() + 
                            " nodes: " + invalidNodes);
                    }
                    
                    logger.info("All {} nodes validated successfully", nodeIds.size());
                    return null;
                });
        } finally {
            clientLock.readLock().unlock();
        }
    }
    
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}