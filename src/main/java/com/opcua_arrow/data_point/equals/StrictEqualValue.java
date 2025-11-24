package com.opcua_arrow.data_point.equals;

public class StrictEqualValue extends BaseEqualValue {

    public StrictEqualValue(long intervalSeconds) {
        super(intervalSeconds);
    }

    @Override
    protected boolean isSameValue(Object newRawValue) {
        return newRawValue.equals(lastValue);
    }
}
