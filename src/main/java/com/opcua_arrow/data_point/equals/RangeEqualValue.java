package com.opcua_arrow.data_point.equals;

public class RangeEqualValue extends BaseEqualValue {

    private final double range;

    public RangeEqualValue(double range, long intervalSeconds) {
        super(intervalSeconds);
        this.range = range;
    }

    @Override
    protected boolean isSameValue(Object newRawValue) {
        // cast seguro garantido externamente
        Number num = (Number) newRawValue;
        Number oldNum = (Number) lastValue;

        if (num == null || oldNum == null)
            return false;

        final double oldVal = oldNum.doubleValue();
        final double newVal = num.doubleValue();

        final double diff = newVal - oldVal;
        final double allowed = oldVal * range;

        return diff <= allowed && diff >= -allowed;
    }
}
