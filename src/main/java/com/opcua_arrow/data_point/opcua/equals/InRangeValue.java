package com.opcua_arrow.data_point.opcua.equals;

import com.opcua_arrow.data_point.IDataPointEqual;
import com.opcua_arrow.opcua.IOPCUADataValue;

public class InRangeValue implements IDataPointEqual {

    private IOPCUADataValue<?> lastValue = null;
    private final double range; // percentual entre 0 e 1 (ex.: 0.05 = 5%)

    public InRangeValue(double range) {
        this.range = range;
    }

    @Override
    public boolean isEqual(IOPCUADataValue<?> newValue) {

        if (lastValue == null) {
            lastValue = newValue;
            return false; // primeiro commit
        }

        // Se valor inconsistente, não commita
        if (!newValue.isConsistent()) {
            return true;
        }

        Number oldNumber = (Number) lastValue.getValue();
        Number newNumber = (Number) newValue.getValue();

        double oldVal = oldNumber.doubleValue();
        double newVal = newNumber.doubleValue();

        double diff = Math.abs(newVal - oldVal);
        double allowed = Math.abs(oldVal * range);

        if (diff <= allowed) {
            return true;
        }

        lastValue = newValue;
        return false;
    }
}
