package com.opcua_arrow.data;

import com.opcua_arrow.opcua.IOPCUADataValue;

public class DataValue {

    private final IOPCUADataValue value;
    private final DataPointParams params;

    public DataValue(IOPCUADataValue value, DataPointParams params) {
        this.value = value;
        this.params = params;
    }

    public IOPCUADataValue getValue() {
        return value;
    }

    public DataPointParams getParams() {
        return params;
    }

}
