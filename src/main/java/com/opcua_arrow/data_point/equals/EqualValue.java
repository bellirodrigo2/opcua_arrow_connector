package com.opcua_arrow.data_point.equals;

import java.util.Objects;

import com.opcua_arrow.data_point.IDataPointEqual;
import com.opcua_arrow.opcua.IOPCUADataValue;

public class EqualValue implements IDataPointEqual {

    private IOPCUADataValue lastValue = null;

    @Override
    public boolean isEqual(IOPCUADataValue newValue) {

        if (lastValue == null) {
            lastValue = newValue;
            return false;
        }

        if (newValue.isConsistent()) {
            boolean equalValue = Objects.equals(lastValue.getValue(), newValue.getValue());

            int lastSC = lastValue.getStatusCode();
            int newSC = newValue.getStatusCode();

            if (lastSC != newSC || !equalValue) {
                lastValue = newValue;
                return false;
            }
        }
        return true;
    }
}
