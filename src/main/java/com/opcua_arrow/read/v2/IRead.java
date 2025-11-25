package com.opcua_arrow.read.v2;

import java.util.List;

import com.opcua_arrow.data_point.equals2.DataPointParams;

public interface IRead {
    List<DataPointParams> geDataPointParams();

    void addDataPoint(DataPointParams dataPointParams);

    void removeDataPoint(DataPointParams dataPointParams);

    void start();

    void stop();

}
