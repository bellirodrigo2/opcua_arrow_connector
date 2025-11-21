package com.opcua_arrow.transform.opcua;

import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.transform.IDataPointParams;
import com.opcua_arrow.transform.IDataValue;

public class DataValue<T> implements IDataValue<T> {

    private final IOPCUADataValue<T> value;
    private final IDataPointParams params;

    public DataValue(IOPCUADataValue<T> value, IDataPointParams params) {
        this.value = value;
        this.params = params;
    }

    @Override
    public IOPCUADataValue<T> getValue() {
        return value;
    }

    @Override
    public IDataPointParams getParams() {
        return params;
    }

}
