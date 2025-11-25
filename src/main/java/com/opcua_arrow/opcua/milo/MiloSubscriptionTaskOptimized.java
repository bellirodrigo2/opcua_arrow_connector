package com.opcua_arrow.opcua.milo;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.read.IReadTask;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-performance OPC-UA subscription task optimized for minimal GC and high
 * throughput.
 * Designed to handle up to 50k nodes efficiently with batch processing.
 */
public class MiloSubscriptionTaskOptimized implements IReadTask {

    private static final Logger logger = LoggerFactory.getLogger(MiloSubscriptionTaskOptimized.class);

    // Configuration
    private final IOPCUAConnection connection;
    private final IQueue<List<IOPCUADataValue>> queue;
    private final double publishingInterval;
    private final int queueSize;
    private final int batchSize;
    private final long batchTimeoutNanos;

    // Node management - lock-free structures
    private final ConcurrentHashMap<String, UaMonitoredItem> monitoredItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UaMonitoredItem, String> itemToNodeId = new ConcurrentHashMap<>();

    // Batch accumulation
    private final ThreadLocal<BatchAccumulator> batchAccumulator;

    // State management
    private volatile UaSubscription subscription;
    private final AtomicBoolean started = new AtomicBoolean(false);

    // Statistics
    private final LongAdder valuesReceived = new LongAdder();
    private final LongAdder batchesSent = new LongAdder();
    private final AtomicInteger activeItems = new AtomicInteger(0);

    /**
     * Creates an optimized subscription task with batch processing.
     *
     * @param connection         The OPC-UA connection
     * @param queue              The queue to push received values to
     * @param publishingInterval Publishing interval in milliseconds
     * @param queueSize          Queue size for monitored items
     * @param batchSize          Number of values to accumulate before pushing
     *                           (default 100)
     * @param batchTimeoutMillis Maximum time to wait before pushing partial batch
     *                           (default 50ms)
     */
    public MiloSubscriptionTaskOptimized(
            IOPCUAConnection connection,
            IQueue<List<IOPCUADataValue>> queue,
            double publishingInterval,
            int queueSize,
            int batchSize,
            long batchTimeoutMillis) {

        this.connection = connection;
        this.queue = queue;
        this.publishingInterval = publishingInterval;
        this.queueSize = queueSize;
        this.batchSize = (batchSize > 0) ? batchSize : 100;
        this.batchTimeoutNanos = batchTimeoutMillis * 1_000_000L;

        // Initialize thread-local batch accumulators
        this.batchAccumulator = ThreadLocal
                .withInitial(() -> new BatchAccumulator(this.batchSize, this.batchTimeoutNanos));
    }

    /**
     * Constructor with default batch settings.
     */
    public MiloSubscriptionTaskOptimized(
            IOPCUAConnection connection,
            IQueue<List<IOPCUADataValue>> queue,
            double publishingInterval,
            int queueSize) {
        this(connection, queue, publishingInterval, queueSize, 100, 50);
    }

    @Override
    public List<String> getNodeIds() {
        // Return a snapshot of node IDs
        return new ArrayList<>(monitoredItems.keySet());
    }

    @Override
    public void addNodeId(String nodeId) {
        if (nodeId == null) {
            throw new IllegalArgumentException("nodeId cannot be null");
        }

        // Fast-path check for existing
        if (monitoredItems.containsKey(nodeId)) {
            logger.debug("NodeId {} already exists", nodeId);
            return;
        }

        if (started.get() && subscription != null) {
            try {
                UaMonitoredItem item = createMonitoredItem(subscription, nodeId);
                if (monitoredItems.putIfAbsent(nodeId, item) == null) {
                    itemToNodeId.put(item, nodeId);
                    activeItems.incrementAndGet();
                    logger.info("Added node {} to active subscription (total: {})",
                            nodeId, activeItems.get());
                }
            } catch (Exception e) {
                logger.error("Failed to add node {} to subscription", nodeId, e);
            }
        } else {
            // Add as pending
            if (monitoredItems.putIfAbsent(nodeId, null) == null) {
                logger.debug("Added node {} to pending list", nodeId);
            }
        }
    }

    @Override
    public void removeNodeId(String nodeId) {
        if (nodeId == null) {
            return;
        }

        UaMonitoredItem item = monitoredItems.remove(nodeId);
        if (item == null) {
            logger.debug("NodeId {} not found", nodeId);
            return;
        }

        itemToNodeId.remove(item);

        if (item != null && started.get() && subscription != null) {
            try {
                subscription.deleteMonitoredItems(List.of(item)).get();
                activeItems.decrementAndGet();
                logger.info("Removed node {} from subscription (remaining: {})",
                        nodeId, activeItems.get());
            } catch (Exception e) {
                logger.error("Failed to remove node {} from subscription", nodeId, e);
            }
        }
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            logger.warn("Subscription task already started");
            return;
        }

        var lock = connection.getClientLock();
        lock.readLock().lock();
        try {
            if (!connection.isConnected()) {
                started.set(false);
                throw new IllegalStateException("Connection not established");
            }

            OpcUaClient client = connection.getClient();
            if (client == null) {
                started.set(false);
                throw new IllegalStateException("Client is null");
            }

            // Create subscription
            subscription = client.getSubscriptionManager()
                    .createSubscription(publishingInterval)
                    .get();

            logger.info("Created subscription with {} ms interval", publishingInterval);

            // Get pending node IDs
            List<String> pendingNodeIds = new ArrayList<>();
            monitoredItems.forEach((nodeId, item) -> {
                if (item == null) {
                    pendingNodeIds.add(nodeId);
                }
            });

            if (!pendingNodeIds.isEmpty()) {
                createMonitoredItemsBatch(pendingNodeIds);
            }

            logger.info("Subscription started with {} items", activeItems.get());

        } catch (Exception e) {
            started.set(false);
            logger.error("Failed to start subscription", e);
            throw new RuntimeException("Failed to start subscription", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            logger.debug("Subscription not started");
            return;
        }

        // Flush any pending batches
        flushAllBatches();

        var lock = connection.getClientLock();
        lock.writeLock().lock();
        try {
            if (subscription != null) {
                OpcUaClient client = connection.getClient();
                if (client != null) {
                    client.getSubscriptionManager()
                            .deleteSubscription(subscription.getSubscriptionId())
                            .get();
                    logger.info("Deleted subscription");
                }
            }

            // Convert items back to pending state
            List<String> nodeIds = new ArrayList<>(monitoredItems.keySet());
            itemToNodeId.clear();
            monitoredItems.clear();
            activeItems.set(0);

            for (String nodeId : nodeIds) {
                monitoredItems.put(nodeId, null);
            }

            subscription = null;
            logger.info("Subscription stopped. Stats: {} values received, {} batches sent",
                    valuesReceived.sum(), batchesSent.sum());

        } catch (Exception e) {
            logger.error("Failed to stop subscription", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Creates monitored items in batch for better performance.
     */
    private void createMonitoredItemsBatch(List<String> nodeIds) throws Exception {
        // Create requests
        List<MonitoredItemCreateRequest> requests = new ArrayList<>(nodeIds.size());

        for (String nodeId : nodeIds) {
            ReadValueId readValueId = new ReadValueId(
                    NodeId.parse(nodeId),
                    AttributeId.Value.uid(),
                    null,
                    QualifiedName.NULL_VALUE);

            MonitoringParameters parameters = new MonitoringParameters(
                    subscription.nextClientHandle(),
                    publishingInterval,
                    null,
                    uint(queueSize),
                    true);

            requests.add(new MonitoredItemCreateRequest(
                    readValueId,
                    MonitoringMode.Reporting,
                    parameters));
        }

        // Create items with optimized callback
        UaSubscription.ItemCreationCallback callback = (item, id) -> {
            item.setValueConsumer(this::onValueChange);
        };

        List<UaMonitoredItem> items = subscription
                .createMonitoredItems(TimestampsToReturn.Both, requests, callback)
                .get();

        // Map items
        int created = 0;
        for (int i = 0; i < items.size() && i < nodeIds.size(); i++) {
            String nodeId = nodeIds.get(i);
            UaMonitoredItem item = items.get(i);
            monitoredItems.put(nodeId, item);
            itemToNodeId.put(item, nodeId);
            created++;
        }

        activeItems.addAndGet(created);
        logger.info("Created {} monitored items in batch", created);
    }

    /**
     * Creates a single monitored item.
     */
    private UaMonitoredItem createMonitoredItem(UaSubscription sub, String nodeId) throws Exception {
        ReadValueId readValueId = new ReadValueId(
                NodeId.parse(nodeId),
                AttributeId.Value.uid(),
                null,
                QualifiedName.NULL_VALUE);

        MonitoringParameters parameters = new MonitoringParameters(
                sub.nextClientHandle(),
                publishingInterval,
                null,
                uint(queueSize),
                true);

        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                readValueId,
                MonitoringMode.Reporting,
                parameters);

        UaSubscription.ItemCreationCallback callback = (item, id) -> {
            item.setValueConsumer(this::onValueChange);
        };

        List<UaMonitoredItem> items = sub
                .createMonitoredItems(TimestampsToReturn.Both, List.of(request), callback)
                .get();

        if (items.isEmpty()) {
            throw new RuntimeException("Failed to create monitored item for: " + nodeId);
        }

        return items.get(0);
    }

    /**
     * Handles value changes with batch accumulation.
     */
    private void onValueChange(UaMonitoredItem item, DataValue value) {
        try {
            // Fast lookup
            String nodeId = itemToNodeId.get(item);
            if (nodeId == null) {
                logger.warn("Value change for unknown item");
                return;
            }

            // Create lightweight data value
            IOPCUADataValue dataValue = new MiloSubscriptionDataValue(value, nodeId);
            valuesReceived.increment();

            // Get thread-local batch accumulator
            BatchAccumulator batch = batchAccumulator.get();

            // Add to batch and flush if needed
            if (batch.add(dataValue)) {
                flushBatch(batch);
            }

        } catch (Exception e) {
            logger.error("Error processing value change", e);
        }
    }

    /**
     * Flushes a batch to the queue.
     */
    private void flushBatch(BatchAccumulator batch) {
        List<IOPCUADataValue> toFlush = batch.drain();
        if (!toFlush.isEmpty()) {
            try {
                queue.push(toFlush);
                batchesSent.increment();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while pushing batch", e);
            }
        }
    }

    /**
     * Forces flush of all thread-local batches.
     */
    private void flushAllBatches() {
        // This is called on stop, so we flush the current thread's batch
        BatchAccumulator batch = batchAccumulator.get();
        flushBatch(batch);
    }

    /**
     * Thread-local batch accumulator for efficient batching without contention.
     */
    private class BatchAccumulator {
        private final List<IOPCUADataValue> buffer;
        private final int maxSize;
        private final long timeoutNanos;
        private long firstItemNanos;

        BatchAccumulator(int maxSize, long timeoutNanos) {
            this.buffer = new ArrayList<>(maxSize);
            this.maxSize = maxSize;
            this.timeoutNanos = timeoutNanos;
            this.firstItemNanos = 0;
        }

        /**
         * Adds a value to the batch.
         * 
         * @return true if batch should be flushed
         */
        boolean add(IOPCUADataValue value) {
            if (buffer.isEmpty()) {
                firstItemNanos = System.nanoTime();
            }

            buffer.add(value);

            // Check if we should flush
            return buffer.size() >= maxSize ||
                    (System.nanoTime() - firstItemNanos) >= timeoutNanos;
        }

        /**
         * Drains the batch and returns the values.
         */
        List<IOPCUADataValue> drain() {
            if (buffer.isEmpty()) {
                return List.of();
            }

            List<IOPCUADataValue> result = new ArrayList<>(buffer);
            buffer.clear();
            firstItemNanos = 0;
            return result;
        }
    }

    // Statistics methods
    public long getValuesReceived() {
        return valuesReceived.sum();
    }

    public long getBatchesSent() {
        return batchesSent.sum();
    }

    public int getActiveItemCount() {
        return activeItems.get();
    }

    public boolean isStarted() {
        return started.get();
    }
}
