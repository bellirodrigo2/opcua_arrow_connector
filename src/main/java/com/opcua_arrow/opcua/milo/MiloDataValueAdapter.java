package com.opcua_arrow.opcua.milo;

import java.time.Instant;

import com.opcua_arrow.opcua.IOPCUADataValue;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

public class MiloDataValueAdapter implements IOPCUADataValue {

    private final DataValue dataValue;
    private final String nodeId;

    public MiloDataValueAdapter(DataValue dataValue, String nodeId) {
        this.dataValue = dataValue;
        this.nodeId = nodeId;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public Instant getSourceTimestamp() {
        DateTime sourceTime = dataValue.getSourceTime();
        if (sourceTime == null || sourceTime.isNull())
            return null;

        Instant baseInstant = sourceTime.getJavaInstant();

        // UShort picos = dataValue.getSourcePicoseconds();
        // if (picos != null && picos.longValue() > 0) {
        // baseInstant = baseInstant.plusNanos(picos.longValue() / 1000);
        // }

        return baseInstant;
    }

    @Override
    public Instant getServerTimestamp() {
        DateTime serverTime = dataValue.getServerTime();
        if (serverTime == null || serverTime.isNull())
            return null;

        Instant baseInstant = serverTime.getJavaInstant();

        // UShort picos = dataValue.getServerPicoseconds();
        // if (picos == null || picos.longValue() == 0)
        // return baseInstant;
        // long nanos = Math.round(picos.longValue() / 1000.0);

        return baseInstant;
    }

    @Override
    public long getTimestampLong() {
        Instant timestamp = getSourceTimestamp();
        UShort picos = dataValue.getSourcePicoseconds();
        if (timestamp == null) {
            timestamp = getServerTimestamp();
            picos = dataValue.getServerPicoseconds();
        }

        if (picos != null && picos.longValue() > 0) {
            long additionalNanos = Math.round(picos.longValue() / 1000.0);
            timestamp = timestamp.plusNanos(additionalNanos);
        }

        return timestamp != null
                ? timestamp.getEpochSecond() * 1_000_000_000L + timestamp.getNano()
                : 0;
    }

    @Override
    public Object getValue() {
        Variant variant = dataValue.getValue();
        if (variant == null)
            return null;

        Object raw = variant.getValue();
        if (raw == null)
            return null;

        return raw; // "Pass-through", sem conversões
    }

    @Override
    public int getStatusCode() {
        StatusCode statusCode = dataValue.getStatusCode();
        return statusCode != null
                ? (int) statusCode.getValue()
                : 0x80000000;
    }

    @Override
    public boolean isGood() {
        StatusCode statusCode = dataValue.getStatusCode();
        return statusCode != null && statusCode.isGood();
    }

    @Override
    public boolean isConsistent() {
        // Milo's DataValue does not provide a direct method to check consistency.
        // Here we assume that if both timestamps are present, the data is consistent.
        return getSourceTimestamp() != null && getServerTimestamp() != null;
    }
}
