package com.opcua_arrow.data.equals;

import java.util.Arrays;

public class StrictEqualDoubleArray implements IsSameValue {

    @Override
    public boolean isSameValue(Object newRawValue, Object lastValue) {

        return Arrays.equals((double[]) newRawValue, (double[]) lastValue);
    }

}
