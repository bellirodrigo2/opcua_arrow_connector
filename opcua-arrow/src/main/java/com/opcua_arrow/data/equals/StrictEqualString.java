package com.opcua_arrow.data.equals;

public class StrictEqualString implements IsSameValue {

    @Override
    public boolean isSameValue(Object newRawValue, Object lastValue) {

        return newRawValue.equals(lastValue);
    }

}
