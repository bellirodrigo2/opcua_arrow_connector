package com.opcua_arrow.transform;

import com.opcua_arrow.opcua.IOPCUADataValue;

public interface IDataValue<T> {

    IOPCUADataValue<T> getValue();

    IDataPointParams getParams();

}