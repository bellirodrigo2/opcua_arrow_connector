package com.opcua_arrow.data;

public class DataReadGroup {

    // private final EDataType dataType;
    private final EReadMode readMode;
    private final long interval;

    public DataReadGroup(EReadMode readMode, long interval) {
        // this.dataType = dataType;
        this.readMode = readMode;
        this.interval = interval;
    }

    // public EDataType getDataType() {
    // return dataType;
    // }

    public EReadMode getReadMode() {
        return readMode;
    }

    public long getInterval() {
        return interval;
    }
}
