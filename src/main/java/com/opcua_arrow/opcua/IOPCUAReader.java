package com.opcua_arrow.opcua;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;

public interface IOPCUAReader extends AutoCloseable {

    List<DataPoint> getDataPoints();

    void setDataPoints(List<DataPoint> dataPoints);

    void addDataPoint(DataPoint dataPoint);

    void removeDataPoint(DataPoint dataPoint);

    CompletableFuture<List<TSValue>> read();

    CompletableFuture<Void> start();

    CompletableFuture<Void> stop();

    boolean isStarted();
}
