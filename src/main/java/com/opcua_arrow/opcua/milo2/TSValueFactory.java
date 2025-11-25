package com.opcua_arrow.opcua.milo2;

import java.time.Instant;

import com.opcua_arrow.data_point.DataWriteGroup;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

public class TSValueFactory {

    public TSValue createTSValue(int id, DataValue dv, DataWriteGroup group) {
        // Pre-extract status code
        StatusCode status = dv.getStatusCode();
        Variant variant = dv.getValue();
        long timestampNanos = computeTimestampNanos(dv);

        return new TSValue(
                id,
                timestampNanos,
                (variant != null) ? variant.getValue() : null,
                status != null && status.isGood(),
                group);

    }

    private long computeTimestampNanos(DataValue dataValue) {

        DateTime sourceTime = dataValue.getSourceTime();
        if (sourceTime != null && !sourceTime.isNull()) {
            return computeTimestampNanos(sourceTime, dataValue.getSourcePicoseconds());
        } else {
            DateTime serverTime = dataValue.getServerTime();
            if (serverTime != null && !serverTime.isNull()) {
                return computeTimestampNanos(serverTime, dataValue.getServerPicoseconds());
            } else {
                return -1L;
            }
        }
    }

    private static long computeTimestampNanos(DateTime dateTime, UShort picos) {
        Instant baseInstant = dateTime.getJavaInstant();
        if (picos != null && picos.longValue() > 0) {
            baseInstant = baseInstant.plusNanos(picos.longValue() / 1000);
        }
        return baseInstant.getEpochSecond() * 1_000_000_000L + baseInstant.getNano();
    }
}
