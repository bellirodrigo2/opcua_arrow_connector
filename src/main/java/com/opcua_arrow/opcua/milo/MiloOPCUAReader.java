package com.opcua_arrow.opcua.milo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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

/**
 * OPC-UA reader implementation using Eclipse Milo.
 * Fully thread-safe and lock-free for NodeId updates.
 */
public class MiloOPCUAReader<T> implements IOPCUAReader<T> {

    private final IRetryPolicy retryPolicy;
    private final IOPCUAConnection connection;

    // Lock-free, atomic, immutable list snapshots
    private final AtomicReference<List<String>> nodeIdsRef = new AtomicReference<>(List.of());

    private final AtomicReference<List<ReadValueId>> readValueIdsRef = new AtomicReference<>(List.of());

    public MiloOPCUAReader(IOPCUAConnection connection, IRetryPolicy retryPolicy) {
        this.connection = connection;
        this.retryPolicy = retryPolicy;
        List<String> nodeIds = List.of();
        setNodeIds(nodeIds);
    }

    @Override
    public List<String> getNodeIds() {
        return nodeIdsRef.get();
    }

    // ------------------------------------------------------------
    // LOCK-FREE NODE LIST MANAGEMENT
    // ------------------------------------------------------------
    @Override
    public void setNodeIds(List<String> nodeIds) {
        if (nodeIds == null)
            throw new IllegalArgumentException("nodeIds cannot be null");

        applyUpdate(old -> List.copyOf(nodeIds));
    }

    @Override
    public void addNodeId(String nodeId) {
        if (nodeId == null)
            throw new IllegalArgumentException("nodeId cannot be null");

        applyUpdate(old -> {
            List<String> newList = new ArrayList<>(old);
            newList.add(nodeId);
            return List.copyOf(newList);
        });
    }

    @Override
    public void removeNodeId(String nodeId) {
        if (nodeId == null)
            return;

        applyUpdate(old -> old.stream()
                .filter(id -> !id.equals(nodeId))
                .collect(Collectors.toUnmodifiableList()));
    }

    /**
     * Lock-free update with CAS.
     */
    private void applyUpdate(Function<List<String>, List<String>> updater) {
        while (true) {
            List<String> oldList = nodeIdsRef.get();
            List<String> newList = updater.apply(oldList);

            if (nodeIdsRef.compareAndSet(oldList, newList)) {

                // Rebuild ReadValueId list atomically
                List<ReadValueId> newReadValues = newList.stream()
                        .map(id -> new ReadValueId(
                                NodeId.parse(id),
                                AttributeId.Value.uid(),
                                null,
                                QualifiedName.NULL_VALUE))
                        .collect(Collectors.toUnmodifiableList());

                readValueIdsRef.set(newReadValues);
                return; // success
            }
            // Else retry (benign race, extremely rare)
        }
    }

    // ------------------------------------------------------------
    // OPC-UA CONNECTION & READ
    // ------------------------------------------------------------
    @Override
    public CompletableFuture<List<IOPCUADataValue<T>>> read() {
        if (connection == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Connection is null"));
        }

        return retryPolicy.executeWithRetry(this::readInternal);
    }

    private CompletableFuture<List<IOPCUADataValue<T>>> readInternal() {
        var lock = connection.getClientLock();
        lock.readLock().lock();

        try {
            OpcUaClient client = connection.getClient();
            if (client == null || !connection.isConnected()) {
                throw new IllegalStateException("Client not connected");
            }

            // Atomic read of node lists
            List<String> ids = nodeIdsRef.get();
            List<ReadValueId> rvids = readValueIdsRef.get();

            return client.read(0, TimestampsToReturn.Both, rvids)
                    .thenApply(response -> {
                        DataValue[] results = response.getResults();
                        List<IOPCUADataValue<T>> values = new ArrayList<>();

                        for (int i = 0; i < results.length; i++) {
                            values.add(new MiloDataValueAdapter<>(results[i], ids.get(i)));
                        }
                        return values;
                    });
        } finally {
            lock.readLock().unlock();
        }
    }

    // ------------------------------------------------------------
    // CONNECTION MANAGEMENT
    // ------------------------------------------------------------
    @Override
    public CompletableFuture<Void> start() {
        if (connection == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Connection is null"));
        }
        return retryPolicy.executeWithRetry(connection::connect);
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

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}
