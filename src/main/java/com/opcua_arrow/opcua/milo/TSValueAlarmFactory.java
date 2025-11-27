package com.opcua_arrow.opcua.milo;

import java.util.Map;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;

public class TSValueAlarmFactory {
    static TSValue createTSValue(DataPoint dp, String json) {
        long timestamp = System.nanoTime();
        Map<String, String> map = Map.of("name", dp.getName(), "description", dp.getDescription(), "nodeId",
                dp.getNodeId(), "pointId", String.valueOf(dp.getPointId()), "content", json);

        return new TSValue(dp.getPointId(), timestamp, (Object) map, true, dp.getWriteGroup());

    }

}
