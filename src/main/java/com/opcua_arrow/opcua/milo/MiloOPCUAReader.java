package com.opcua_arrow.opcua.milo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.opcua_arrow.data.DataPointParams;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.data.TSValueFactory;
import com.opcua_arrow.opcua.IOPCUAConnection;
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
 * Lock-free & thread-safe snapshot design.
 */
public class MiloOPCUAReader implements IOPCUAReader {

    private final IRetryPolicy retryPolicy;
    private final IOPCUAConnection connection;
    private final TSValueFactory tsValueFactory;

    // ------------------------------------------------------------
    // Snapshot atomico e imutável
    // ------------------------------------------------------------
    private static final class NodeSnapshot {
        final List<DataPointParams> dataPoints;
        final List<ReadValueId> readValueIds;

        NodeSnapshot(List<DataPointParams> dataPoints, List<ReadValueId> readValueIds) {
            this.dataPoints = dataPoints;
            this.readValueIds = readValueIds;
        }
    }

    private final AtomicReference<NodeSnapshot> snapshotRef = new AtomicReference<>(
            new NodeSnapshot(List.of(), List.of()));

    // ------------------------------------------------------------
    public MiloOPCUAReader(IOPCUAConnection connection, IRetryPolicy retryPolicy, TSValueFactory tsValueFactory) {
        this.connection = connection;
        this.retryPolicy = retryPolicy;
        this.tsValueFactory = tsValueFactory;
    }

    @Override
    public List<DataPointParams> getDataPoints() {
        return snapshotRef.get().dataPoints;
    }

    // ------------------------------------------------------------
    // LOCK-FREE NODE LIST MANAGEMENT
    // ------------------------------------------------------------
    @Override
    public void setDataPoints(List<DataPointParams> dataPoints) {
        if (dataPoints == null)
            throw new IllegalArgumentException("dataPoints cannot be null");
        applyUpdate(old -> List.copyOf(dataPoints));
    }

    @Override
    public void addDataPoint(DataPointParams dataPoint) {
        if (dataPoint == null)
            throw new IllegalArgumentException("dataPoint cannot be null");

        applyUpdate(old -> {
            List<DataPointParams> newList = new ArrayList<>(old);
            newList.add(dataPoint);
            return List.copyOf(newList);
        });
    }

    @Override
    public void removeDataPoint(DataPointParams dataPoint) {
        if (dataPoint == null)
            return;

        applyUpdate(old -> old.stream()
                .filter(id -> !id.equals(dataPoint))
                .collect(Collectors.toUnmodifiableList()));
    }

    // ------------------------------------------------------------
    // ATUALIZAÇÃO ATÔMICA COM SNAPSHOT ÚNICO
    // ------------------------------------------------------------
    private void applyUpdate(Function<List<DataPointParams>, List<DataPointParams>> updater) {
        while (true) {
            NodeSnapshot oldSnap = snapshotRef.get();
            List<DataPointParams> oldList = oldSnap.dataPoints;

            List<DataPointParams> newList = updater.apply(oldList);

            List<ReadValueId> newReadValues = newList.stream()
                    .map(dataPoint -> new ReadValueId(
                            NodeId.parse(dataPoint.getNodeId()),
                            AttributeId.Value.uid(),
                            null,
                            QualifiedName.NULL_VALUE))
                    .collect(Collectors.toUnmodifiableList());

            NodeSnapshot newSnap = new NodeSnapshot(newList, newReadValues);

            if (snapshotRef.compareAndSet(oldSnap, newSnap)) {
                return; // sucesso, snapshot trocado atomically
            }
        }
    }

    // ------------------------------------------------------------
    // OPC-UA CONNECTION & READ
    // ------------------------------------------------------------

    @Override
    public CompletableFuture<List<TSValue>> read() {
        if (connection == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Connection is null"));
        }
        return retryPolicy.executeWithRetry(this::readInternal);
    }

    private CompletableFuture<List<TSValue>> readInternal() {
        var lock = connection.getClientLock();
        lock.readLock().lock();
        try {
            OpcUaClient client = connection.getClient();
            if (client == null || !connection.isConnected()) {
                throw new IllegalStateException("Client not connected");
            }

            // snapshot consistente
            NodeSnapshot snap = snapshotRef.get();
            List<DataPointParams> ids = snap.dataPoints;
            List<ReadValueId> rvids = snap.readValueIds;

            return client.read(0, TimestampsToReturn.Both, rvids)
                    .thenApply(response -> {
                        DataValue[] results = response.getResults();
                        int n = results.length;
                        List<TSValue> values = new ArrayList<>(n);

                        for (int i = 0; i < n; i++) {
                            DataPointParams dp = ids.get(i);
                            TSValue tsValue = tsValueFactory.createTSValue(dp.getPointId(), results[i],
                                    dp.getWriteGroup());
                            if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                                values.add(tsValue);
                            }
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
