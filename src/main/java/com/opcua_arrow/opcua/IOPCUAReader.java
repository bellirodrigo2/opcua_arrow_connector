package com.opcua_arrow.opcua;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.opcua_arrow.data_point.DataPointParams;
import com.opcua_arrow.data_point.TSValue;

public interface IOPCUAReader extends AutoCloseable {

    List<DataPointParams> getDataPoints();

    void setDataPoints(List<DataPointParams> dataPoints);

    void addDataPoint(DataPointParams dataPoint);

    void removeDataPoint(DataPointParams dataPoint);

    CompletableFuture<List<TSValue>> read();

    CompletableFuture<Void> start();

    CompletableFuture<Void> stop();

    boolean isStarted();
}
