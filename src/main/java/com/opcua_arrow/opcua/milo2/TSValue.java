package com.opcua_arrow.opcua.milo2;

import com.opcua_arrow.data_point.DataWriteGroup;

public class TSValue {
    public final int id;
    public final long timestamp;
    public final Object value;
    public final boolean isGood;
    public final DataWriteGroup writeGroup;

    public TSValue(int id, long timestamp, Object value, boolean isGood, DataWriteGroup writeGroup) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
        this.isGood = isGood;
        this.writeGroup = writeGroup;
    }

    public boolean isConsistent() {
        return timestamp >= 0;
    }

}
