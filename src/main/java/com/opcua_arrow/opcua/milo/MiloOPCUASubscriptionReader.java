package com.opcua_arrow.opcua.milo;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.retry.IRetryPolicy;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OPC-UA subscription-based reader that pushes values to a queue.
 * Thread-safe using CopyOnWriteArrayList for data point management.
 */
public class MiloOPCUASubscriptionReader implements IReader {
    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUASubscriptionReader.class);

    private final IOPCUAConnection connection;
    private final IRetryPolicy retryPolicy;
    private final TSValueFactory tsValueFactory;
    private final IQueue<List<TSValue>> queue;
    private final double publishingInterval;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile UaSubscription subscription;
    private final List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();
    private final Map<String, DataPoint> nodeIdToDataPoint = new ConcurrentHashMap<>();

    public MiloOPCUASubscriptionReader(IOPCUAConnection connection, IRetryPolicy retryPolicy,
            TSValueFactory tsValueFactory, IQueue<List<TSValue>> queue, long publishingInterval) {
        this.connection = connection;
        this.retryPolicy = retryPolicy;
        this.tsValueFactory = tsValueFactory;
        this.queue = queue;
        this.publishingInterval = publishingInterval;
    }

    @Override
    public List<DataPoint> getDataPoint() {
        return new ArrayList<>(dataPoints);
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        if (dataPoint == null)
            throw new IllegalArgumentException("dataPoint cannot be null");
        dataPoints.add(dataPoint);
        nodeIdToDataPoint.put(dataPoint.getNodeId(), dataPoint);
        if (started.get()) {
            updateSubscription();
        }
    }

    @Override
    public void removeDataPoint(DataPoint dataPoint) {
        if (dataPoint == null)
            return;
        dataPoints.remove(dataPoint);
        nodeIdToDataPoint.remove(dataPoint.getNodeId());
        if (started.get()) {
            updateSubscription();
        }
    }

    @Override
    public void start() {
        if (started.getAndSet(true)) {
            return;
        }

        retryPolicy.executeWithRetry(() -> {
            return connection.connect()
                    .thenCompose(v -> createSubscription())
                    .exceptionally(ex -> {
                        started.set(false);
                        logger.error("Failed to start subscription reader", ex);
                        throw new RuntimeException("Failed to start subscription reader", ex);
                    });
        });
    }

    @Override
    public void stop() {
        if (!started.getAndSet(false)) {
            return;
        }

        var lock = connection.getClientLock();
        lock.readLock().lock();
        try {
            if (subscription != null) {
                OpcUaClient client = connection.getClient();
                if (client != null) {
                    client.getSubscriptionManager()
                            .deleteSubscription(subscription.getSubscriptionId())
                            .thenRun(() -> {
                                subscription = null;
                                logger.info("Subscription deleted");
                            })
                            .exceptionally(ex -> {
                                logger.warn("Error deleting subscription", ex);
                                return null;
                            });
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        connection.disconnect();
    }

    private CompletableFuture<Void> createSubscription() {
        var lock = connection.getClientLock();
        lock.readLock().lock();
        try {
            OpcUaClient client = connection.getClient();
            if (client == null || !connection.isConnected()) {
                throw new IllegalStateException("Client not connected");
            }
            return client.getSubscriptionManager()
                    .createSubscription(publishingInterval)
                    .thenCompose(sub -> {
                        this.subscription = sub;
                        logger.info("Subscription created: id={}", sub.getSubscriptionId());
                        return createMonitoredItems(sub);
                    });

        } finally {
            lock.readLock().unlock();
        }
    }

    private CompletableFuture<Void> createMonitoredItems(UaSubscription subscription) {
        if (dataPoints.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        subscription.addNotificationListener(new UaSubscription.NotificationListener() {
            @Override
            public void onDataChangeNotification(UaSubscription sub, List<UaMonitoredItem> monitoredItems,
                    List<DataValue> dataValues,
                    DateTime publishTime) {

                List<TSValue> batchValues = new ArrayList<>();

                for (int i = 0; i < monitoredItems.size(); i++) {
                    UaMonitoredItem item = monitoredItems.get(i);
                    DataValue value = dataValues.get(i);

                    String nodeIdStr = item.getReadValueId().getNodeId().toParseableString();
                    DataPoint dp = nodeIdToDataPoint.get(nodeIdStr);

                    if (dp != null) {
                        TSValue tsValue = tsValueFactory.createTSValue(dp.getPointId(), value, dp.getWriteGroup());
                        if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                            batchValues.add(tsValue);
                        }
                    }
                }

                if (!batchValues.isEmpty()) {
                    try {
                        queue.push(batchValues);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted while pushing batch to queue", e);
                    }
                }
            }
        });

        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        for (DataPoint dp : dataPoints) {
            requests.add(createMonitoredItemRequest(dp, subscription));
        }

        return subscription.createMonitoredItems(TimestampsToReturn.Both, requests)
                .thenAccept(items -> {
                    logger.info("Created {} monitored items", items.size());
                    for (UaMonitoredItem item : items) {
                        if (!item.getStatusCode().isGood()) {
                            logger.warn("Failed to create item for nodeId={} (status={})",
                                    item.getReadValueId().getNodeId(), item.getStatusCode());
                        }
                    }
                });
    }

    private MonitoredItemCreateRequest createMonitoredItemRequest(DataPoint dataPoint, UaSubscription sub) {
        ReadValueId readValueId = new ReadValueId(
                NodeId.parse(dataPoint.getNodeId()),
                AttributeId.Value.uid(),
                null,
                QualifiedName.NULL_VALUE);

        double samplingInterval = dataPoint.getReadGroup().getInterval() * 1000.0;
        UInteger clientHandle = sub.nextClientHandle();

        MonitoringParameters parameters = new MonitoringParameters(
                clientHandle,
                samplingInterval,
                null,
                uint(10),
                true);

        return new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters);
    }

    private void updateSubscription() {
        if (!started.get() || subscription == null) {
            return;
        }

        var lock = connection.getClientLock();
        lock.readLock().lock();
        try {
            List<UaMonitoredItem> oldItems = subscription.getMonitoredItems();

            CompletableFuture<?> deleteFuture;
            if (!oldItems.isEmpty()) {
                deleteFuture = subscription.deleteMonitoredItems(new ArrayList<>(oldItems));
            } else {
                deleteFuture = CompletableFuture.completedFuture(null);
            }

            deleteFuture.thenCompose(v -> createMonitoredItems(subscription))
                    .exceptionally(ex -> {
                        logger.error("Failed to update subscription", ex);
                        return null;
                    });
        } finally {
            lock.readLock().unlock();
        }
    }
}
