package com.opcua_arrow.data.equals;

import com.opcua_arrow.data.IDataPointEqual;

public class NoFilter implements IDataPointEqual {

    @Override
    public final boolean isEqual(Object newValue, boolean newIsGood) {
        return false;
    }
}
