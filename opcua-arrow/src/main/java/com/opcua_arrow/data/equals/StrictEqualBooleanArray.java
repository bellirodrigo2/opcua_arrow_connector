package com.opcua_arrow.data.equals;

import java.util.Arrays;

public class StrictEqualBooleanArray implements IsSameValue {

    @Override
    public boolean isSameValue(Object newRawValue, Object lastValue) {

        return Arrays.equals((boolean[]) newRawValue, (boolean[]) lastValue);
    }

}
