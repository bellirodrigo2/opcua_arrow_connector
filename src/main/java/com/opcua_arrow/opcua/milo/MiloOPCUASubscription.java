package com.opcua_arrow.opcua.milo;

// Import static factory methods for unsigned types
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.IOPCUASubscriber;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFieldList;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiloOPCUASubscription implements IOPCUASubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUASubscription.class);
    private Map<Double, UaSubscription> subscriptionMap = new ConcurrentHashMap<>();
    private Map<String, DataPoint> nodeIdToPointIdMap = new ConcurrentHashMap<>();
    private IOPCUAConnection connection;
    private final TSValueFactory tsValueFactory;
    private final TSValueAlarmFactory alarmTsValueFactory;
    private final int queueSize;

    public MiloOPCUASubscription(IOPCUAConnection connection, TSValueFactory tsValueFactory,
            TSValueAlarmFactory alarmTsValueFactory, int queueSize) {
        this.connection = connection;
        this.tsValueFactory = tsValueFactory;
        this.alarmTsValueFactory = alarmTsValueFactory;
        this.queueSize = queueSize;
    }

    @Override
    public List<DataPoint> getDataPoints() {
        return new ArrayList<>(nodeIdToPointIdMap.values());
    }

    @Override
    public void addNodesToSubscription(
            double interval,
            List<DataPoint> dataPoints,
            Consumer<List<TSValue>> batchHandler) {

        for (int i = 0; i < dataPoints.size(); i++) {
            nodeIdToPointIdMap.putIfAbsent(dataPoints.get(i).getNodeId(), dataPoints.get(i));
        }

        UaSubscription subscription = getOrCreateSubscription(interval, batchHandler);

        List<MonitoredItemCreateRequest> requests = new ArrayList<>();

        for (DataPoint dp : dataPoints) {
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                    new ReadValueId(
                            NodeId.parse(dp.getNodeId()),
                            AttributeId.Value.uid(),
                            null,
                            QualifiedName.NULL_VALUE),
                    MonitoringMode.Reporting,
                    new MonitoringParameters(
                            subscription.nextClientHandle(),
                            interval,
                            null,
                            uint(queueSize),
                            true));
            requests.add(request);
        }

        // UaSubscription.ItemCreationCallback onItemCreated = (item, id) -> item
        // .setValueConsumer(this::onSubscriptionValue);
        try {
            List<UaMonitoredItem> items = subscription.createMonitoredItems(
                    TimestampsToReturn.Both,
                    requests,
                    // onItemCreated
                    null).get();

            // Check creation status
            for (UaMonitoredItem item : items) {
                if (item.getStatusCode().isGood()) {
                    logger.debug("Successfully created monitored item for: " +
                            item.getReadValueId().getNodeId());
                } else {
                    logger.error("Failed to create monitored item for: " +
                            item.getReadValueId().getNodeId() + " Status: " + item.getStatusCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error creating monitored items: " + e.getMessage());
        }
    }

    @Override
    public void removeNodeFromSubscription(double interval, String nodeId) {
        UaSubscription subscription = subscriptionMap.get(interval);
        if (subscription == null) {
            logger.warn("No subscription found for interval: " + interval);
            return;
        }

        List<UaMonitoredItem> itemsToDelete = subscription.getMonitoredItems().stream()
                .filter(item -> item.getReadValueId().getNodeId().equals(NodeId.parse(nodeId)))
                .collect(Collectors.toList());

        if (!itemsToDelete.isEmpty()) {
            try {
                subscription.deleteMonitoredItems(itemsToDelete).get();
                nodeIdToPointIdMap.remove(nodeId);

            } catch (Exception e) {
                logger.error("Error deleting monitored items: " + e.getMessage());
            }
        }
    }

    /**
     * Create or get a subscription with a specific batch handler
     */
    private UaSubscription getOrCreateSubscription(
            double interval, Consumer<List<TSValue>> batchHandler) {

        UaSubscription subscription = subscriptionMap.computeIfAbsent(interval, i -> {
            try {
                UaSubscription newSubscription = connection.getClient().getSubscriptionManager()
                        .createSubscription(i)
                        .get();

                newSubscription.addNotificationListener(new UaSubscription.NotificationListener() {
                    @Override
                    public void onDataChangeNotification(
                            UaSubscription sub,
                            List<UaMonitoredItem> monitoredItems,
                            List<DataValue> dataValues,
                            DateTime publishTime) {

                        try {
                            List<TSValue> values = new ArrayList<>();
                            for (int i = 0; i < monitoredItems.size(); i++) {
                                DataPoint dp = nodeIdToPointIdMap
                                        .get(monitoredItems.get(i).getReadValueId().getNodeId().toString());
                                if (dp == null) {
                                    logger.debug("Skipping data change for unknown nodeId: " +
                                            monitoredItems.get(i).getReadValueId().getNodeId().toString());
                                    continue;
                                }
                                TSValue tsValue = tsValueFactory.createTSValue(dp.getPointId(), dataValues.get(i),
                                        dp.getWriteGroup());
                                if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                                    values.add(tsValue);
                                }
                            }
                            if (!values.isEmpty()) {
                                batchHandler.accept(values);
                            }
                        } catch (Exception e) {
                            logger.error("Error in batch handler: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onEventNotification(
                            UaSubscription sub,
                            List<UaMonitoredItem> monitoredItems,
                            List<Variant[]> eventFieldLists,
                            DateTime publishTime) {

                        try {
                            for (int i = 0; i < monitoredItems.size(); i++) {

                                DataPoint dp = nodeIdToPointIdMap
                                        .get(monitoredItems.get(i).getReadValueId().getNodeId().toString());
                                if (dp == null) {
                                    logger.debug("Skipping event for unknown nodeId: " +
                                            monitoredItems.get(i).getReadValueId().getNodeId().toString());
                                    continue;
                                }
                                Variant[] eventFields = eventFieldLists.get(i);
                                List<TSValue> alarmValues = new ArrayList<>();

                                for (Variant variant : eventFields) {
                                    if (variant.getValue() instanceof EventFieldList) {
                                        EventFieldList eventFieldList = (EventFieldList) variant.getValue();
                                        Variant[] fields = eventFieldList.getEventFields();

                                        // Create TSValue from alarm data
                                        TSValue tsValue = alarmTsValueFactory.createTSValue(
                                                dp.getPointId(),
                                                fields,
                                                dp.getWriteGroup());

                                        if (tsValue.isConsistent()
                                                && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                                            alarmValues.add(tsValue);
                                        }
                                    }
                                }

                                if (!alarmValues.isEmpty()) {
                                    batchHandler.accept(alarmValues);
                                }
                            }
                            logger.debug("Received {} alarm events at {}", eventFieldLists.size(), publishTime);
                        } catch (Exception e) {
                            logger.error("Error processing alarm events: " + e.getMessage());
                        }
                    }

                    // @Override
                    // public void onStatusChangedNotification(UaSubscription subscription,
                    // StatusCode status) {
                    // logger.info("Subscription " + subscription.getSubscriptionId() +
                    // " status changed: " + status);
                    // }
                });
                return newSubscription;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return subscription;

    }

    // // Individual item value change callback
    // private void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
    // logger.debug("Value changed for item: " + item.getReadValueId().getNodeId() +
    // " New Value: " + value.getValue());
    // // This is called for individual item processing if needed
    // // Usually overridden by custom value consumers
    // }

    @Override
    public void closeSubscription(double interval) {
        UaSubscription subscription = subscriptionMap.remove(interval);
        if (subscription == null) {
            return;
        }
        try {
            UInteger subscriptionId = subscription.getSubscriptionId();
            connection.getClient().getSubscriptionManager().deleteSubscription(subscriptionId).get();
        } catch (Exception e) {
            logger.error("Error closing subscription: " + e.getMessage());
        }
    }
}
