package com.opcua_arrow.opcua.milo;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.IOPCUASubscriber;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFieldList;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiloOPCUAAlarmSubscription implements IOPCUASubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MiloOPCUAAlarmSubscription.class);
    private Map<Double, UaSubscription> subscriptionMap = new ConcurrentHashMap<>();
    private Map<String, DataPoint> nodeIdToPointIdMap = new ConcurrentHashMap<>();
    private IOPCUAConnection connection;
    private final AlarmTSValueFactory alarmTsValueFactory;
    private final int queueSize;
    // private final AlarmSeverityFilter severityFilter;

    // // Standard alarm event fields to monitor
    // private static final QualifiedName[] ALARM_SELECT_CLAUSES = new
    // QualifiedName[] {
    // new QualifiedName(0, "EventId"),
    // new QualifiedName(0, "EventType"),
    // new QualifiedName(0, "SourceNode"),
    // new QualifiedName(0, "SourceName"),
    // new QualifiedName(0, "Time"),
    // new QualifiedName(0, "ReceiveTime"),
    // new QualifiedName(0, "Message"),
    // new QualifiedName(0, "Severity"),
    // new QualifiedName(0, "ConditionName"),
    // new QualifiedName(0, "BranchId"),
    // new QualifiedName(0, "Retain"),
    // new QualifiedName(0, "EnabledState"),
    // new QualifiedName(0, "AckedState"),
    // new QualifiedName(0, "ConfirmedState"),
    // new QualifiedName(0, "ActiveState"),
    // new QualifiedName(0, "SuppressedState"),
    // new QualifiedName(0, "ShelvingState")
    // };

    public MiloOPCUAAlarmSubscription(
            IOPCUAConnection connection,
            AlarmTSValueFactory alarmTsValueFactory,
            int queueSize
    // AlarmSeverityFilter severityFilter
    ) {
        this.connection = connection;
        this.alarmTsValueFactory = alarmTsValueFactory;
        this.queueSize = queueSize;
        // this.severityFilter = severityFilter != null ? severityFilter : new
        // AlarmSeverityFilter(0);
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
            // EventFilter eventFilter = createAlarmEventFilter(dp);

            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                    new ReadValueId(
                            NodeId.parse(dp.getNodeId()),
                            AttributeId.EventNotifier.uid(),
                            null,
                            QualifiedName.NULL_VALUE),
                    MonitoringMode.Reporting,
                    new MonitoringParameters(
                            subscription.nextClientHandle(),
                            interval,
                            null,
                            // ExtensionObject.encode(connection.getClient().getStaticSerializationContext(),
                            // eventFilter),
                            uint(queueSize),
                            true));
            requests.add(request);
        }

        // Create all monitored items at once
        try {
            List<UaMonitoredItem> items = subscription.createMonitoredItems(
                    null, // TimestampsToReturn not used for events
                    requests).get();

            // Set event consumer for each item
            for (int i = 0; i < items.size(); i++) {
                UaMonitoredItem item = items.get(i);
                // DataPoint dp = dataPoints.get(i);

                if (item.getStatusCode().isGood()) {
                    // item.setEventConsumer((monitoredItem, eventFieldLists) -> {
                    // processAlarmEvents(dp, eventFieldLists, batchHandler);
                    // });
                    logger.debug("Successfully created alarm monitor for: " +
                            item.getReadValueId().getNodeId());
                } else {
                    logger.error("Failed to create alarm monitor for: " +
                            item.getReadValueId().getNodeId() + " Status: " + item.getStatusCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error creating alarm monitors: " + e.getMessage());
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

        nodeIdToPointIdMap.remove(nodeId);

        if (!itemsToDelete.isEmpty()) {
            try {
                subscription.deleteMonitoredItems(itemsToDelete).get();
            } catch (Exception e) {
                logger.error("Error deleting alarm monitors: " + e.getMessage());
            }
        }
    }

    /**
     * Create an event filter for alarm subscriptions
     */
    // private EventFilter createAlarmEventFilter(DataPoint dp) {
    // // Select clauses - which alarm fields to return
    // List<SimpleAttributeOperand> selectClauses = new ArrayList<>();
    // for (QualifiedName field : ALARM_SELECT_CLAUSES) {
    // selectClauses.add(new SimpleAttributeOperand(
    // Identifiers.BaseEventType,
    // new QualifiedName[] { field },
    // AttributeId.Value.uid(),
    // null));
    // }

    // // Where clause - filter by severity and optionally by source
    // ContentFilter whereClause = createWhereClause(dp);

    // return new EventFilter(
    // selectClauses.toArray(new SimpleAttributeOperand[0]),
    // whereClause);
    // }

    // /**
    // * Create where clause for filtering alarms
    // */
    // private ContentFilter createWhereClause(DataPoint dp) {
    // List<ContentFilterElement> elements = new ArrayList<>();

    // // Filter by minimum severity
    // if (severityFilter.getMinimumSeverity() > 0) {
    // SimpleAttributeOperand severityOperand = new SimpleAttributeOperand(
    // Identifiers.BaseEventType,
    // new QualifiedName[] { new QualifiedName(0, "Severity") },
    // AttributeId.Value.uid(),
    // null);

    // LiteralOperand minSeverityOperand = new LiteralOperand(
    // new Variant(uint(severityFilter.getMinimumSeverity())));

    // elements.add(new ContentFilterElement(
    // FilterOperator.GreaterThanOrEqual,
    // new FilterOperand[] { severityOperand, minSeverityOperand }));
    // }

    // // Filter by source node if specified in DataPoint
    // if (dp.hasSourceFilter()) {
    // SimpleAttributeOperand sourceOperand = new SimpleAttributeOperand(
    // Identifiers.BaseEventType,
    // new QualifiedName[] { new QualifiedName(0, "SourceNode") },
    // AttributeId.Value.uid(),
    // null);

    // LiteralOperand sourceNodeOperand = new LiteralOperand(
    // new Variant(NodeId.parse(dp.getSourceNodeId())));

    // ContentFilterElement sourceFilter = new ContentFilterElement(
    // FilterOperator.Equals,
    // new FilterOperand[] { sourceOperand, sourceNodeOperand });

    // if (!elements.isEmpty()) {
    // // Combine with AND if we have severity filter
    // ContentFilterElement andElement = new ContentFilterElement(
    // FilterOperator.And,
    // new FilterOperand[] {
    // new ElementOperand(uint(0)),
    // new ElementOperand(uint(1))
    // });
    // elements.add(sourceFilter);
    // elements.add(andElement);
    // } else {
    // elements.add(sourceFilter);
    // }
    // }

    // // Return empty filter if no conditions
    // if (elements.isEmpty()) {
    // elements.add(new ContentFilterElement(
    // FilterOperator.IsNull,
    // new FilterOperand[0]));
    // }

    // return new ContentFilter(elements.toArray(new ContentFilterElement[0]));
    // }

    /**
     * Process alarm events and convert to TSValues
     */
    private void processAlarmEvents(
            DataPoint dp,
            Variant[] eventFieldLists,
            Consumer<List<TSValue>> batchHandler) {

        List<TSValue> alarmValues = new ArrayList<>();

        for (Variant variant : eventFieldLists) {
            if (variant.getValue() instanceof EventFieldList) {
                EventFieldList eventFieldList = (EventFieldList) variant.getValue();
                Variant[] fields = eventFieldList.getEventFields();

                // Create TSValue from alarm data
                TSValue tsValue = alarmTsValueFactory.createAlarmTSValue(
                        dp.getPointId(),
                        fields,
                        dp.getWriteGroup());

                if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value, tsValue.isGood)) {
                    alarmValues.add(tsValue);
                }
            }
        }

        if (!alarmValues.isEmpty()) {
            batchHandler.accept(alarmValues);
        }
    }

    // Helper methods for extracting typed values from event fields
    private ByteString extractByteString(Variant[] fields, int index) {
        if (index < fields.length && fields[index].getValue() instanceof ByteString) {
            return (ByteString) fields[index].getValue();
        }
        return ByteString.NULL_VALUE;
    }

    private NodeId extractNodeId(Variant[] fields, int index) {
        if (index < fields.length && fields[index].getValue() instanceof NodeId) {
            return (NodeId) fields[index].getValue();
        }
        return NodeId.NULL_VALUE;
    }

    private String extractString(Variant[] fields, int index) {
        if (index < fields.length && fields[index].getValue() != null) {
            return fields[index].getValue().toString();
        }
        return "";
    }

    private DateTime extractDateTime(Variant[] fields, int index) {
        if (index < fields.length && fields[index].getValue() instanceof DateTime) {
            return (DateTime) fields[index].getValue();
        }
        return DateTime.MIN_VALUE;
    }

    private int extractUInt16(Variant[] fields, int index) {
        if (index < fields.length && fields[index].getValue() != null) {
            try {
                return ((Number) fields[index].getValue()).intValue();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private boolean extractBoolean(Variant[] fields, int index) {
        if (index < fields.length && fields[index].getValue() instanceof Boolean) {
            return (Boolean) fields[index].getValue();
        }
        return false;
    }

    /**
     * Create or get a subscription with alarm-specific handling
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
                    public void onEventNotification(
                            UaSubscription sub,
                            List<UaMonitoredItem> monitoredItems,
                            List<Variant[]> eventFieldLists,
                            DateTime publishTime) {

                        try {
                            for (UaMonitoredItem item : monitoredItems) {
                                DataPoint dp = nodeIdToPointIdMap
                                        .get(item.getReadValueId().getNodeId().toString());
                                processAlarmEvents(dp, eventFieldLists.get(monitoredItems.indexOf(item)), batchHandler);
                            }
                            logger.debug("Received {} alarm events at {}", eventFieldLists.size(), publishTime);
                        } catch (Exception e) {
                            logger.error("Error processing alarm events: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onStatusChangedNotification(UaSubscription subscription, StatusCode status) {
                        logger.info("Alarm subscription " + subscription.getSubscriptionId() +
                                " status changed: " + status);
                    }
                });
                return newSubscription;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return subscription;
    }

    @Override
    public void closeSubscription(double interval) {
        UaSubscription subscription = subscriptionMap.remove(interval);
        if (subscription == null) {
            return;
        }
        try {
            connection.getClient().getSubscriptionManager()
                    .deleteSubscription(subscription.getSubscriptionId()).get();
        } catch (Exception e) {
            logger.error("Error closing alarm subscription: " + e.getMessage());
        }
    }

    /**
     * Internal alarm data structure
     */
    public static class AlarmData {
        public ByteString eventId;
        public NodeId eventType;
        public NodeId sourceNode;
        public String sourceName;
        public DateTime time;
        public DateTime receiveTime;
        public String message;
        public int severity;
        public String conditionName;
        public boolean retain;
        public String enabledState;
        public String ackedState;
        public String confirmedState;
        public String activeState;
    }

    /**
     * Alarm severity filter configuration
     */
    public static class AlarmSeverityFilter {
        private final int minimumSeverity;

        public AlarmSeverityFilter(int minimumSeverity) {
            this.minimumSeverity = minimumSeverity;
        }

        public int getMinimumSeverity() {
            return minimumSeverity;
        }
    }

    /**
     * Factory interface for creating TSValue from alarm data
     */
    private class AlarmTSValueFactory {
        TSValue createAlarmTSValue(int pointId, Variant[] fields, DataWriteGroup writeGroup) {
            AlarmData alarmData = new AlarmData();
            alarmData.eventId = extractByteString(fields, 0);
            alarmData.eventType = extractNodeId(fields, 1);
            alarmData.sourceNode = extractNodeId(fields, 2);
            alarmData.sourceName = extractString(fields, 3);
            alarmData.time = extractDateTime(fields, 4);
            alarmData.receiveTime = extractDateTime(fields, 5);
            alarmData.message = extractString(fields, 6);
            alarmData.severity = extractUInt16(fields, 7);
            alarmData.conditionName = extractString(fields, 8);
            alarmData.retain = extractBoolean(fields, 10);
            alarmData.enabledState = extractString(fields, 11);
            alarmData.ackedState = extractString(fields, 12);
            alarmData.confirmedState = extractString(fields, 13);
            alarmData.activeState = extractString(fields, 14);

            long timestamp = alarmData.time.getJavaTime();

            return new TSValue(pointId, timestamp, (Object) alarmData, true, writeGroup);

        }
    }
}
