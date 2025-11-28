package com.opcua_arrow.data.equals;

public class StrictEqualDouble implements IsSameValue {

    @Override
    public Boolean isSameValue(Object newRawValue, Object lastValue) {

        double a = ((Number) lastValue).doubleValue();
        double b = ((Number) newRawValue).doubleValue();

        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return a == b;
    }

}
