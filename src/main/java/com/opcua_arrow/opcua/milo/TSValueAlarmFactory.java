package com.opcua_arrow.opcua.milo;

import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.TSValue;

import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class TSValueAlarmFactory {
    TSValue createTSValue(int pointId, Variant[] fields, DataWriteGroup writeGroup) {
        long timestamp = System.nanoTime();

        return new TSValue(pointId, timestamp, (Object) fields, true, writeGroup);

    }
}
