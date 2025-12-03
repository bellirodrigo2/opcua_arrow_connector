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
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFieldList;
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

        for (int i = 0; i < dataPoints.size(); i++) {
            nodeIdToPointIdMap.putIfAbsent(dataPoints.get(i).getNodeId(), dataPoints.get(i));
        }

        OpcUaSubscription subscription = getOrCreateSubscription(dataReadGroup, batchHandler);

        List<OpcUaMonitoredItem> monitoredItems = dataPoints.stream()
                .map(dp -> OpcUaMonitoredItem.newDataItem(NodeId.parse(dp.getNodeId())))
                .collect(Collectors.toList());

        subscription.addMonitoredItems(monitoredItems);
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

                newSubscription.setSubscriptionListener(dataReadGroup.getReadMode() == EReadMode.EVENTS
                        ? createEventNotificationListener(batchHandler, dataReadGroup.getInterval())
                        : createDataNotificationListener(batchHandler, dataReadGroup.getInterval()));
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

                ICallBackObject ac = callBack.startCallback(getLabel(), monitoredItems);
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
                        TSValue tsValue = TSValueFactory.createTSValue(dp, dataValues.get(i));
                        if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                            values.add(tsValue);
                        }
                    }
                    if (!values.isEmpty()) {
                        batchHandler.accept(values);
                    }
                } catch (Exception e) {
                    logger.error("Error in batch handler: " + e.getMessage());
                } finally {
                    ac.close();
                }
            }
        };

    }

    private SubscriptionListener createEventNotificationListener(Consumer<List<TSValue>> batchHandler, Long interval) {
        return new SubscriptionListener() {
            private String getLabel() {
                return "subscription_events_" + interval + "_ms";
            }

            @Override
            public void onEventReceived(
                    OpcUaSubscription subscription,
                    List<OpcUaMonitoredItem> monitoredItems,
                    List<Variant[]> eventFieldLists) {
                EncodingContext context = subscription.getClient().getStaticEncodingContext();
                ICallBackObject ac = callBack.startCallback(getLabel(), monitoredItems);

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

                                String json = VariantJsonConverter.variantsToJson(context, fields);

                                // Create TSValue from alarm data
                                TSValue tsValue = TSValueAlarmFactory.createTSValue(
                                        dp,
                                        json);

                                if (tsValue.isConsistent()
                                        && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                                    alarmValues.add(tsValue);
                                }
                            } else {
                                logger.debug("Skipping non-EventFieldList variant in event fields.");
                            }
                        }

                        if (!alarmValues.isEmpty()) {
                            batchHandler.accept(alarmValues);
                        }
                    }
                    logger.debug("Received {} alarm events at {}", eventFieldLists.size());
                } catch (Exception e) {
                    logger.error("Error processing alarm events: " + e.getMessage());
                } finally {
                    ac.close();
                }
            }
        };
    }

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
