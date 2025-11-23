package com.opcua_arrow.data_point;

public interface IDataValue<T> {

    T getValue();

    IDataPointParams getParams();
}
