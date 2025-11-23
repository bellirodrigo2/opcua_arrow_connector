package com.opcua_arrow.data_point.opcua;

import com.opcua_arrow.data_point.IDataValue;
import com.opcua_arrow.opcua.IOPCUADataValue;

public class DataValue<T> implements IDataValue<IOPCUADataValue<T>> {

    private final IOPCUADataValue<T> value;
    private final DataPointParams params;

    public DataValue(IOPCUADataValue<T> value, DataPointParams params) {
        this.value = value;
        this.params = params;
    }

    public IOPCUADataValue<T> getValue() {
        return value;
    }

    public DataPointParams getParams() {
        return params;
    }

}
