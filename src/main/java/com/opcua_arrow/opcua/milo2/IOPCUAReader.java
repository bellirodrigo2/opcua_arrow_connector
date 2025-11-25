package com.opcua_arrow.opcua.milo2;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.opcua_arrow.data_point.equals2.DataPointParams;

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
