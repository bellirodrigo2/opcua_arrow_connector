package com.opcua_arrow.read;

import java.util.List;

import com.opcua_arrow.data.DataPointParams;

public interface IReader {

    List<DataPointParams> getDataPointParams();

    void addDataPoint(DataPointParams dataPointParams);

    void removeDataPoint(DataPointParams dataPointParams);

    void start();

    void stop();

}
