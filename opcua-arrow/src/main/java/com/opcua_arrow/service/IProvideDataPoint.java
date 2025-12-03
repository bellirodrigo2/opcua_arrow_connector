package com.opcua_arrow.service;

import java.time.Instant;
import java.util.List;

public interface IProvideDataPoint<T> {

    // OPCUAClientConfig getClientConfig(String sourceName);

    List<T> getDataPoints(String sourceName);

    List<T> getUpdatedDataPoints(String sourceName, Instant lastUpdate);

    List<String> getDeletedDataPoints(String sourceName, Instant lastUpdate);
}
