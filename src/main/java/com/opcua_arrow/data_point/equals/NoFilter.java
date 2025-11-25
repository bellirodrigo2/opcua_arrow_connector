package com.opcua_arrow.data_point.equals;

import com.opcua_arrow.data_point.IDataPointEqual;

public class NoFilter implements IDataPointEqual {

    @Override
    public final boolean isEqual(Object newValue, boolean newIsGood) {
        return false;
    }
}
