package com.opcua_arrow.opcua.milo;

import java.time.Instant;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

public class TSValueFactory {

    public static TSValue createTSValue(DataPoint dp, DataValue dv) {
        // Pre-extract status code
        StatusCode status = dv.getStatusCode();
        Variant variant = dv.getValue();
        long timestampNanos = computeTimestampNanos(dv);

        return new TSValue(
                dp.getPointId(),
                timestampNanos,
                (variant != null) ? variant.getValue() : null,
                status != null && status.isGood(),
                dp.getWriteGroup());

    }

    static private long computeTimestampNanos(DataValue dataValue) {

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
