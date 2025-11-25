package com.opcua_arrow.read;

import java.util.List;

import com.opcua_arrow.data.DataPoint;

public interface IReader {

    List<DataPoint> getDataPoint();

    void addDataPoint(DataPoint DataPoint);

    void removeDataPoint(DataPoint DataPoint);

    void start();

    void stop();

}
