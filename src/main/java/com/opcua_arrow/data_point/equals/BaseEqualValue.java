package com.opcua_arrow.data_point.equals;

import com.opcua_arrow.data_point.IDataPointEqual;
import com.opcua_arrow.opcua.IOPCUADataValue;

public abstract class BaseEqualValue implements IDataPointEqual {

    protected Object lastValue = null;
    protected int lastStatusCode = 0;
    protected long lastUpdateNanos = 0L;

    protected final long intervalNanos;

    protected BaseEqualValue(long intervalSeconds) {
        this.intervalNanos = intervalSeconds * 1_000_000_000L;
    }

    @Override
    public final boolean isEqual(IOPCUADataValue newValue) {

        if (!newValue.isConsistent())
            return true;

        final long now = System.nanoTime();
        final Object newRawValue = newValue.getValue();
        final int newStatus = newValue.getStatusCode();

        // Primeira atualização ou intervalo vigente
        if (lastValue == null || (now - lastUpdateNanos) < intervalNanos) {
            return !updateState(newRawValue, newStatus, now);
        }

        // Status mudou
        if (newStatus != lastStatusCode) {
            return !updateState(newRawValue, newStatus, now);
        }

        // *** AQUI VEM A ESTRATÉGIA ***
        if (!isSameValue(newRawValue)) {
            return !updateState(newRawValue, newStatus, now);
        }

        return true;
    }

    // strategy method
    protected abstract boolean isSameValue(Object newRawValue);

    protected final boolean updateState(Object value, int statusCode, long nowNanos) {
        if (value == null)
            return false;

        lastValue = value;
        lastStatusCode = statusCode;
        lastUpdateNanos = nowNanos;
        return true;
    }
}
