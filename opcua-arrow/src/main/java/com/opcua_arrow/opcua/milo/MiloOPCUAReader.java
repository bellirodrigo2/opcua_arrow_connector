package com.opcua_arrow.opcua.milo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.opcua.retry.IRetryPolicy;
import com.opcua_arrow.read.IReader;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

/**
 * OPC-UA reader síncrono usando Eclipse Milo.
 * Pensado para ser chamado dentro de virtual threads.
 */
public class MiloOPCUAReader implements IReader {

    private final IRetryPolicy retryPolicy;
    private final MiloOPCUAConnection connection;

    public MiloOPCUAReader(MiloOPCUAConnection connection, IRetryPolicy retryPolicy) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        if (retryPolicy == null) {
            throw new IllegalArgumentException("retryPolicy cannot be null");
        }
        this.connection = connection;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public List<TSValue> read(List<DataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return List.of();
        }

        // Retry síncrono. Este método pode ser chamado em virtual thread.
        return retryPolicy.executeWithRetry(() -> readInternal(dataPoints));
    }

    /**
     * Implementação interna de leitura.
     * Bloqueia na chamada readAsync().get(), mas é OK em virtual threads.
     */
    private List<TSValue> readInternal(List<DataPoint> dataPoints) throws Exception {
        var lock = connection.getClientLock();
        lock.readLock().lock();
        try {
            OpcUaClient client = connection.getClient();
            if (client == null || !connection.isConnected()) {
                throw new IllegalStateException("Client not connected");
            }

            List<ReadValueId> readValueIds = dataPoints.stream()
                    .map(dataPoint -> new ReadValueId(
                            NodeId.parse(dataPoint.getNodeId()),
                            AttributeId.Value.uid(),
                            null,
                            QualifiedName.NULL_VALUE))
                    .collect(Collectors.toList());

            // Bloqueia até receber a resposta (virtual thread-friendly).
            DataValue[] results = client
                    .readAsync(0.0, TimestampsToReturn.Both, readValueIds)
                    .get().getResults();

            return processReadResponse(dataPoints, results);

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Idêntico ao que você já tinha, mas síncrono.
     */
    private List<TSValue> processReadResponse(List<DataPoint> dataPoints, DataValue[] results) {
        if (results.length != dataPoints.size()) {
            throw new IllegalStateException(String.format(
                    "Response size mismatch: expected %d, got %d",
                    dataPoints.size(), results.length));
        }

        int n = results.length;
        List<TSValue> values = new ArrayList<>(n);

        try {
            for (int i = 0; i < n; i++) {
                DataPoint dp = dataPoints.get(i);
                DataValue dataValue = results[i];

                TSValue tsValue = TSValueFactory.createTSValue(dp, dataValue);

                if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                    values.add(tsValue);
                }
            }

            return values;

        } catch (Exception e) {
            throw new RuntimeException("Error processing read results", e);
        }
    }

    @Override
    public void start() {
        // Supondo que connect() retorna CompletableFuture<Void>
        retryPolicy.executeWithRetry(() -> {
            connection.connect().get();
            return null;
        });
    }

    @Override
    public void stop() {
        try {
            // Se você quiser retry aqui também, pode usar retryPolicy.executeWithRetry
            connection.disconnect().get();
        } catch (Exception e) {
            throw new RuntimeException("Error disconnecting OPC-UA client", e);
        }
    }

    @Override
    public boolean isStarted() {
        return connection.isConnected();
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
