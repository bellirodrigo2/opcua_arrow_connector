package com.opcua_arrow.loop;

import com.opcua_arrow.data.DataPoint;

public interface ILoop {

    void addDataPoint(DataPoint DataPoint);

    void removeDataPoint(DataPoint DataPoint);

    void start();

    void stop();
}
