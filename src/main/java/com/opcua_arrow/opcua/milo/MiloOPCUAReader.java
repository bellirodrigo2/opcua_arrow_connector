package com.opcua_arrow.opcua.milo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.retry.IRetryPolicy;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OPC-UA reader implementation using Eclipse Milo.
 * Handles reading values from configured nodes using an encapsulated
 * connection.
 *
 * @param <T> The type of values to read
 */
public class MiloOPCUAReader<T> implements IOPCUAReader<T> {
    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUAReader.class);

    private final IRetryPolicy retryPolicy;
    private final IOPCUAConnection connection;
    private final BlockingQueue<List<IOPCUADataValue<T>>> queue;

    public MiloOPCUAReader(IOPCUAConnection connection, IRetryPolicy retryPolicy,
            BlockingQueue<List<IOPCUADataValue<T>>> queue) {
        this.connection = connection;
        this.retryPolicy = retryPolicy;
        this.queue = queue;
    }

    @Override
    public CompletableFuture<Void> read(List<String> nodeIds) {
        if (connection == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Connection is null"));
        }

        return retryPolicy.executeWithRetry(() -> readInternal(nodeIds)
                .thenAccept(values -> {
                    try {
                        queue.put(values);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted while adding value to queue", e);
                    }
                }));
    }

    @Override
    public CompletableFuture<Void> start() {
        if (connection == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Connection is null"));
        }
        return retryPolicy.executeWithRetry(() -> connection.connect());
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

    private CompletableFuture<List<IOPCUADataValue<T>>> readInternal(List<String> nodeIds) {
        ReadWriteLock clientLock = connection.getClientLock();
        clientLock.readLock().lock();

        try {
            OpcUaClient client = connection.getClient();
            if (client == null || !connection.isConnected()) {
                throw new IllegalStateException("Client not connected");
            }

            List<ReadValueId> readValueIds = nodeIds.stream()
                    .map(id -> new ReadValueId(
                            NodeId.parse(id),
                            AttributeId.Value.uid(),
                            null,
                            QualifiedName.NULL_VALUE))
                    .collect(Collectors.toList());

            return client.read(0, TimestampsToReturn.Both, readValueIds)
                    .thenApply(response -> {
                        DataValue[] results = response.getResults();
                        List<IOPCUADataValue<T>> values = new ArrayList<>();
                        for (int i = 0; i < results.length; i++) {
                            values.add(new MiloDataValueAdapter<>(results[i], nodeIds.get(i)));
                        }
                        return values;
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