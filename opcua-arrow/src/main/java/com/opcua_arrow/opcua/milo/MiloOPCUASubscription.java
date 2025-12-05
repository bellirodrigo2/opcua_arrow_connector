package com.opcua_arrow.opcua.milo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.opcua_arrow.ICallBack;
import com.opcua_arrow.ICallBack.ICallBackObject;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.read.ISubscriber;

import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription.SubscriptionListener;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiloOPCUASubscription implements ISubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUASubscription.class);
    private final Map<String, DataPoint> nodeIdToPointIdMap = new ConcurrentHashMap<>();
    private final Map<DataReadGroup, OpcUaSubscription> subscriptionMap = new ConcurrentHashMap<>();
    private final MiloOPCUAConnection connection;
    private final ICallBack callBack;

    public MiloOPCUASubscription(MiloOPCUAConnection connection, ICallBack callBack) {
        this.connection = connection;
        this.callBack = callBack;
    }

    @Override
    public List<DataPoint> getDataPoints() {
        return new ArrayList<>(nodeIdToPointIdMap.values());
    }

    @Override
    public void addNodesToSubscription(
            // double interval,
            DataReadGroup dataReadGroup,
            List<DataPoint> dataPoints,
            Consumer<List<TSValue>> batchHandler) {

        if (dataReadGroup.getReadMode() != EReadMode.SUBSCRIBE
        // && dataReadGroup.getReadMode() != EReadMode.EVENTS
        ) {
            logger.error("addNodesToSubscription called with unsupported read mode: " + dataReadGroup.getReadMode());
            return;
        }

        logger.info("addNodesToSubscription called with {} data points", dataPoints.size());
        for (int i = 0; i < dataPoints.size(); i++) {
            String nodeId = dataPoints.get(i).getNodeId();
            logger.info("Adding to map: {} -> point id {}", nodeId, dataPoints.get(i).getPointId());
            nodeIdToPointIdMap.putIfAbsent(nodeId, dataPoints.get(i));
        }

        OpcUaSubscription subscription = getOrCreateSubscription(dataReadGroup, batchHandler);
        logger.info("Got subscription with publishing interval: {}ms", subscription.getPublishingInterval());

        List<OpcUaMonitoredItem> monitoredItems = dataPoints.stream()
                .map(dp -> {
                    NodeId parsedNodeId = NodeId.parse(dp.getNodeId());
                    logger.info("Creating monitored item for nodeId: {} -> parsed as: {}", dp.getNodeId(),
                            parsedNodeId);
                    OpcUaMonitoredItem item = OpcUaMonitoredItem.newDataItem(parsedNodeId);
                    // Set the sampling interval to match the subscription's publishing interval
                    item.setSamplingInterval((double) dataReadGroup.getInterval());
                    return item;
                })
                .collect(Collectors.toList());

        logger.info("Adding {} monitored items to subscription (locally)", monitoredItems.size());
        subscription.addMonitoredItems(monitoredItems);

        logger.info("Creating monitored items on server via createMonitoredItems()");
        try {
            // createMonitoredItems() sends CreateMonitoredItemsRequest to the server
            // for all items with SyncState.INITIAL
            subscription.createMonitoredItems();
            logger.info("Monitored items created successfully. Total items in subscription: {}",
                    subscription.getMonitoredItems().size());
        } catch (Exception e) {
            logger.error("Failed to create monitored items on server", e);
            throw new RuntimeException("Failed to create monitored items on server", e);
        }
    }

    @Override
    public void removeNodeFromSubscription(DataReadGroup dataReadGroup, List<String> nodeIds) {
        OpcUaSubscription subscription = subscriptionMap.get(dataReadGroup);
        if (subscription == null) {
            logger.warn("No subscription found for: " + dataReadGroup);
            return;
        }
        List<OpcUaMonitoredItem> itemsToDelete = nodeIds.stream()
                .flatMap(nodeId -> subscription.getMonitoredItems().stream()
                        .filter(item -> item.getReadValueId().getNodeId().equals(NodeId.parse(nodeId))))
                .collect(Collectors.toList());
        if (!itemsToDelete.isEmpty()) {
            try {
                subscription.removeMonitoredItem(itemsToDelete.get(0));
                for (String nodeId : nodeIds)
                    nodeIdToPointIdMap.remove(nodeId);

            } catch (Exception e) {
                logger.error("Error deleting monitored items: " + e.getMessage());
            }
        }
    }

    /**
     * Create or get a subscription with a specific batch handler
     */
    private OpcUaSubscription getOrCreateSubscription(
            DataReadGroup dataReadGroup, Consumer<List<TSValue>> batchHandler) {

        OpcUaSubscription subscription = subscriptionMap.computeIfAbsent(dataReadGroup, i -> {
            try {
                OpcUaSubscription newSubscription = new OpcUaSubscription(connection.getClient());
                Double interval = (double) i.getInterval();
                newSubscription.setPublishingInterval(interval);

                newSubscription.setSubscriptionListener(
                        createDataNotificationListener(batchHandler, dataReadGroup.getInterval()));
                // dataReadGroup.getReadMode() == EReadMode.EVENTS
                // ? createEventNotificationListener(batchHandler, dataReadGroup.getInterval())
                // : createDataNotificationListener(batchHandler, dataReadGroup.getInterval()));

                newSubscription.create();
                return newSubscription;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return subscription;

    }

    private SubscriptionListener createDataNotificationListener(Consumer<List<TSValue>> batchHandler, Long interval) {
        return new SubscriptionListener() {

            private String getLabel() {
                return "subscription_data_" + interval + "_ms";
            }

            @Override
            public void onDataReceived(
                    OpcUaSubscription subscription,
                    List<OpcUaMonitoredItem> monitoredItems,
                    List<DataValue> dataValues) {

                logger.info("onDataReceived called with {} items and {} values", monitoredItems.size(),
                        dataValues.size());
                ICallBackObject ac = callBack.startCallback(getLabel(), monitoredItems);
                try {
                    List<TSValue> values = new ArrayList<>();
                    for (int i = 0; i < monitoredItems.size(); i++) {
                        String nodeIdStr = monitoredItems.get(i).getReadValueId().getNodeId().toParseableString();
                        logger.info("Looking up nodeId: {} in map with {} entries", nodeIdStr,
                                nodeIdToPointIdMap.size());
                        logger.info("Map keys: {}", nodeIdToPointIdMap.keySet());

                        DataPoint dp = nodeIdToPointIdMap.get(nodeIdStr);
                        if (dp == null) {
                            logger.warn("Skipping data change for unknown nodeId: {}", nodeIdStr);
                            continue;
                        }
                        logger.info("Found DataPoint for {}, creating TSValue", nodeIdStr);
                        TSValue tsValue = TSValueFactory.createTSValue(dp, dataValues.get(i));
                        logger.info("TSValue: id={}, value={}, isGood={}, isConsistent={}",
                                tsValue.id, tsValue.value, tsValue.isGood, tsValue.isConsistent());
                        if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                            logger.info("Adding TSValue to results");
                            values.add(tsValue);
                        } else {
                            logger.info("TSValue filtered out by equals check");
                        }
                    }
                    logger.info("Calling batchHandler with {} values", values.size());
                    if (!values.isEmpty()) {
                        batchHandler.accept(values);
                    }
                } catch (Exception e) {
                    logger.error("Error in batch handler: " + e.getMessage(), e);
                    ac.markFailure(e);
                } finally {
                    ac.close();
                }
            }
        };

    }

    // private SubscriptionListener
    // createEventNotificationListener(Consumer<List<TSValue>> batchHandler, Long
    // interval) {
    // return new SubscriptionListener() {
    // private String getLabel() {
    // return "subscription_events_" + interval + "_ms";
    // }

    // @Override
    // public void onEventReceived(
    // OpcUaSubscription subscription,
    // List<OpcUaMonitoredItem> monitoredItems,
    // List<Variant[]> eventFieldLists) {
    // EncodingContext context =
    // subscription.getClient().getStaticEncodingContext();
    // ICallBackObject ac = callBack.startCallback(getLabel(), monitoredItems);

    // try {
    // for (int i = 0; i < monitoredItems.size(); i++) {

    // DataPoint dp = nodeIdToPointIdMap
    // .get(monitoredItems.get(i).getReadValueId().getNodeId().toParseableString());
    // if (dp == null) {
    // logger.debug("Skipping event for unknown nodeId: " +
    // monitoredItems.get(i).getReadValueId().getNodeId().toParseableString());
    // continue;
    // }
    // Variant[] eventFields = eventFieldLists.get(i);
    // List<TSValue> alarmValues = new ArrayList<>();

    // for (Variant variant : eventFields) {
    // if (variant.getValue() instanceof EventFieldList) {
    // EventFieldList eventFieldList = (EventFieldList) variant.getValue();
    // Variant[] fields = eventFieldList.getEventFields();

    // String json = VariantJsonConverter.variantsToJson(context, fields);

    // // Create TSValue from alarm data
    // TSValue tsValue = TSValueAlarmFactory.createTSValue(
    // dp,
    // json);

    // if (tsValue.isConsistent()
    // && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
    // alarmValues.add(tsValue);
    // }
    // } else {
    // logger.debug("Skipping non-EventFieldList variant in event fields.");
    // }
    // }

    // if (!alarmValues.isEmpty()) {
    // batchHandler.accept(alarmValues);
    // }
    // }
    // logger.debug("Received {} alarm events at {}", eventFieldLists.size());
    // } catch (Exception e) {
    // logger.error("Error processing alarm events: " + e.getMessage());
    // } finally {
    // ac.close();
    // }
    // }
    // };
    // }

    @Override
    public void closeSubscription(DataReadGroup dataReadGroup) {
        OpcUaSubscription subscription = subscriptionMap.remove(dataReadGroup);
        if (subscription == null) {
            return;
        }
        try {
            subscription.delete();
        } catch (Exception e) {
            logger.error("Error closing subscription for " + dataReadGroup + ": " + e.getMessage());
        }
    }
}
