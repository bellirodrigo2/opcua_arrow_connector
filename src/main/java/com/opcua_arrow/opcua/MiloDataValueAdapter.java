package com.opcua_arrow.opcua;

import com.opcua_arrow.interfaces.IOPCUADataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import java.time.Instant;

public class MiloDataValueAdapter<T> implements IOPCUADataValue<T> {

    private final DataValue dataValue;
    private final String nodeId;
    private final Integer pointId;

    public MiloDataValueAdapter(DataValue dataValue, String nodeId, Integer pointId) {
        this.dataValue = dataValue;
        this.nodeId = nodeId;
        this.pointId = pointId;
    }


    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public Integer getPointId() {
        return pointId;
    }

    @Override
    public Instant getSourceTimestamp() {
        DateTime sourceTime = dataValue.getSourceTime();
        if (sourceTime == null || sourceTime.isNull()) return null;

        Instant baseInstant = sourceTime.getJavaInstant();

        UShort picos = dataValue.getSourcePicoseconds();
        if (picos != null && picos.longValue() > 0) {
            baseInstant = baseInstant.plusNanos(picos.longValue() / 1000);
        }

        return baseInstant;
    }

    @Override
    public Instant getServerTimestamp() {
        DateTime serverTime = dataValue.getServerTime();
        if (serverTime == null || serverTime.isNull()) return null;

        Instant baseInstant = serverTime.getJavaInstant();

        UShort picos = dataValue.getServerPicoseconds();
        if (picos != null && picos.longValue() > 0) {
            baseInstant = baseInstant.plusNanos(picos.longValue() / 1000);
        }

        return baseInstant;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getValue() {
        Variant variant = dataValue.getValue();
        if (variant == null) return null;

        Object raw = variant.getValue();
        if (raw == null) return null;

        return (T) raw; // "Pass-through", sem conversões
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
}
