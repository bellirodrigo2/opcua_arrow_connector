package com.opcua_arrow.service;

import java.time.Instant;
import java.util.List;

import com.opcua_arrow.config.OPCUAClientConfig;

public interface IProvideDataPoint {

    OPCUAClientConfig getClientConfig(String sourceName);

    List<DataPointDTO> getDataPoints(String sourceName);

    List<DataPointDTO> getUpdatedDataPoints(String sourceName, Instant lastUpdate);

    List<String> getDeletedDataPoints(String sourceName, Instant lastUpdate);
}
