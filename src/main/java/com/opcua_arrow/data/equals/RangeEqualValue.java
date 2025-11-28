package com.opcua_arrow.data.equals;

public class RangeEqualValue implements IsSameValue {

    private final double range;

    public RangeEqualValue(double range) {
        this.range = Math.abs(range);
    }

    @Override
    public Boolean isSameValue(Object newRawValue, Object lastValue) {

        // cast seguro garantido externamente
        Number num = (Number) newRawValue;
        Number oldNum = (Number) lastValue;

        final double oldVal = oldNum.doubleValue();
        final double newVal = num.doubleValue();

        // if (Double.isNaN(newVal) || Double.isNaN(oldVal)) {
        // return false;
        // }
        // if (Double.isInfinite(newVal) || Double.isInfinite(oldVal)) {
        // return newVal == oldVal; // mesmo infinito => igual
        // }

        final double diff = Math.abs(newVal - oldVal);

        double base = Math.abs(oldVal);
        double allowed;

        // Base 0: usa range como tolerância absoluta (testZeroValueBase)
        if (base == 0.0d) {
            allowed = range;
        } else {
            allowed = base * range;
        }
        // final double allowed = oldVal * range;

        return diff <= allowed;
    }
}
