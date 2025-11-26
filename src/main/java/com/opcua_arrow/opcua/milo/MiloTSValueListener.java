package com.opcua_arrow.opcua.milo;

import java.util.ArrayList;
import java.util.List;

import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.loop.LoopReader;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription.NotificationListener;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiloTSValueListener implements NotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    private final TSValueFactory tsValueFactory;

    public MiloTSValueListener(TSValueFactory tsValueFactory) {
        this.tsValueFactory = tsValueFactory;
    }

    @Override
    public void onDataChangeNotification(
            UaSubscription sub,
            List<UaMonitoredItem> monitoredItems,
            List<DataValue> dataValues,
            DateTime publishTime) {

        int n = dataValues.size();
        List<TSValue> values = new ArrayList<>(n);

        // for (int i = 0; i < n; i++) {
        // // DataPoint dp = ids.get(i);
        // TSValue tsValue = tsValueFactory.createTSValue(dp.getPointId(),
        // dataValues.get(i),
        // dp.getWriteGroup());
        // if (tsValue.isConsistent() && dp.getEquals().isEqual(tsValue.value,
        // tsValue.isGood)) {
        // values.add(tsValue);
        // }
        // }

    }

    @Override
    public void onStatusChangedNotification(UaSubscription subscription, StatusCode status) {
        logger.debug("Subscription " + subscription.getSubscriptionId() +
                " status changed: " + status);
    }
}
