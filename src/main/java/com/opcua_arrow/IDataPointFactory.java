package com.opcua_arrow;

import com.opcua_arrow.data.DataPoint;

@FunctionalInterface
public interface IDataPointFactory<T> {
    DataPoint createDataPoint(T dto);
}
