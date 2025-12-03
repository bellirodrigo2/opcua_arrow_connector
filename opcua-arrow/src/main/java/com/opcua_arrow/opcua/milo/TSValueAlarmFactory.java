package com.opcua_arrow.opcua.milo;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;

public class TSValueAlarmFactory {
    static TSValue createTSValue(DataPoint dp, String alarmJsonString) {
        long timestamp = System.nanoTime();

        return new TSValue(dp.getPointId(), timestamp, (Object) alarmJsonString, true, dp.getWriteGroup());

    }

}
