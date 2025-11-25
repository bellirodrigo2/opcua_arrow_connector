package com.opcua_arrow.data_point;

public class DataReadGroup {

    private final EDataType dataType;
    private final EReadMode readMode;
    private final long interval;

    public DataReadGroup(EDataType dataType, EReadMode readMode, long interval) {
        this.dataType = dataType;
        this.readMode = readMode;
        this.interval = interval;
    }

    public EDataType getDataType() {
        return dataType;
    }

    public EReadMode getReadMode() {
        return readMode;
    }

    public long getInterval() {
        return interval;
    }
}
