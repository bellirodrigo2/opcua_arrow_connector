package com.opcua_arrow.data.equals;

public class StrictEqualString implements IsSameValue {

    @Override
    public Boolean isSameValue(Object newRawValue, Object lastValue) {

        return newRawValue.equals(lastValue);
    }

}
