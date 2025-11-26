package com.opcua_arrow.opcua;

import java.util.List;
import java.util.function.Consumer;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;

public interface IOPCUASubscriber {

    void addNodesToSubscription(
            double interval,
            List<DataPoint> dataPoints,
            Consumer<List<TSValue>> batchHandler);

    void removeNodeFromSubscription(double interval, String nodeId);

    List<DataPoint> getDataPoints();

    void closeSubscription(double interval);
}
