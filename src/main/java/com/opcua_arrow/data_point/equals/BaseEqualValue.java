package com.opcua_arrow.data_point.equals;

import com.opcua_arrow.data_point.IDataPointEqual;

public abstract class BaseEqualValue implements IDataPointEqual {

    protected Object lastValue = null;
    protected boolean lastIsGood = false;
    protected long lastUpdateNanos = 0L;

    protected final long intervalNanos;

    protected BaseEqualValue(long intervalSeconds) {
        this.intervalNanos = intervalSeconds * 1_000_000_000L;
    }

    @Override
    public final boolean isEqual(Object newValue, boolean newIsGood) {

        final long now = System.nanoTime();

        // Primeira atualização ou intervalo vigente
        if (lastValue == null || (now - lastUpdateNanos) < intervalNanos) {
            return !updateState(newValue, newIsGood, now);
        }

        // Status mudou
        if (newIsGood != lastIsGood) {
            return !updateState(newValue, newIsGood, now);
        }

        // *** AQUI VEM A ESTRATÉGIA ***
        if (!isSameValue(newValue)) {
            return !updateState(newValue, newIsGood, now);
        }

        return true;
    }

    // strategy method
    protected abstract boolean isSameValue(Object newRawValue);

    protected final boolean updateState(Object value, boolean isGood, long nowNanos) {
        if (value == null)
            return false;

        lastValue = value;
        lastIsGood = isGood;
        lastUpdateNanos = nowNanos;
        return true;
    }
}
