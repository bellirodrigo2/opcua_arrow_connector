package com.opcua_arrow.read;

import com.opcua_arrow.data_point.DataPointParams;

public interface IReader {

    void addDataPoint(DataPointParams dataPointParams);

    void removeDataPoint(DataPointParams dataPointParams);

    void start();

    void stop();

}
