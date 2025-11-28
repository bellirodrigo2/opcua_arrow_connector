package com.opcua_arrow.data.equals;

import com.opcua_arrow.data.IDataPointEqual;

public abstract class BaseEqualValue implements IDataPointEqual {

    private final IsSameValue isSameValue;

    protected Object lastValue = null;
    protected boolean lastIsGood = false;
    protected long lastUpdateNanos = -1L;

    protected final long intervalNanos;

    protected BaseEqualValue(long intervalSeconds, IsSameValue isSameValue) {
        if (intervalSeconds < 0) {
            throw new IllegalArgumentException("Interval seconds must be non-negative");
        }
        this.intervalNanos = intervalSeconds * 1_000_000_000L;
        this.isSameValue = isSameValue;
    }

    @Override
    public final boolean isEqual(Object newValue, boolean newIsGood) {

        final long now = System.nanoTime();

        if (lastUpdateNanos == -1L) {
            updateState(newValue, newIsGood, now);
            return false;
        }
        if (now - lastUpdateNanos >= intervalNanos) {
            updateState(newValue, newIsGood, now);
            return false;
        }

        // Status mudou
        if (newIsGood != lastIsGood) {
            updateState(newValue, newIsGood, now);
            return false;
        }
        if (lastValue == null && newValue == null) {
            return true;
        }
        if (lastValue == null || newValue == null) {
            return false;
        }

        if (!isSameValue.isSameValue(newValue, lastValue)) {
            updateState(newValue, newIsGood, now);
            return false;
        }

        return true;
    }

    protected void updateState(Object value, boolean isGood, long nowNanos) {

        lastValue = value;
        lastIsGood = isGood;
        lastUpdateNanos = nowNanos;
    }
}
