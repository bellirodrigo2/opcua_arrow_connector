package com.opcua_arrow.data.equals;

public class RangeEqualValue implements IsSameValue {

    private final double range;

    public RangeEqualValue(double range) {
        this.range = Math.abs(range);
    }

    @Override
    public boolean isSameValue(Object newRawValue, Object lastValue) {

        // cast seguro garantido externamente
        Number num = (Number) newRawValue;
        Number oldNum = (Number) lastValue;

        final double oldVal = oldNum.doubleValue();
        final double newVal = num.doubleValue();

        if (Double.isNaN(newVal) || Double.isNaN(oldVal))
            return false;

        if (Double.isInfinite(newVal) || Double.isInfinite(oldVal))
            return newVal == oldVal; // mesmo infinito => igual

        final double diff = Math.abs(newVal - oldVal);
        final double base = Math.abs(oldVal);
        final double allowed = (base == 0.0d ? range : base * range);

        return diff <= allowed;
    }
}
