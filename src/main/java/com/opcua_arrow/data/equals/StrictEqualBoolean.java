package com.opcua_arrow.data.equals;

public class StrictEqualBoolean implements IsSameValue {

    @Override
    public Boolean isSameValue(Object newRawValue, Object lastValue) {

        return newRawValue == lastValue;
    }
}
