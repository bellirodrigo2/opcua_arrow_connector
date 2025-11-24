package com.opcua_arrow.service;

import java.time.Instant;
import java.util.List;

import com.opcua_arrow.config.OPCUAClientConfig;

public interface IProvideDataPoint {

    OPCUAClientConfig getClientConfig();

    List<DataPointDTO> getDataPoints();

    List<DataPointDTO> getUpdatedDataPoints(Instant lastUpdate);

    List<String> getDeletedDataPoints(Instant lastUpdate);
}
