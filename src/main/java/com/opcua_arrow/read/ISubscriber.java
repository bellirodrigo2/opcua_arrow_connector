package com.opcua_arrow.read;

import java.util.List;
import java.util.function.Consumer;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.TSValue;

public interface ISubscriber {

        void addNodesToSubscription(
                        DataReadGroup dataReadGroup,
                        List<DataPoint> dataPoints,
                        Consumer<List<TSValue>> batchHandler);

        void removeNodeFromSubscription(
                        DataReadGroup dataReadGroup,
                        List<String> nodeIds);

        List<DataPoint> getDataPoints();

        void closeSubscription(
                        DataReadGroup dataReadGroup);
}
