package com.opcua_arrow.data_point;

public class DataReadGroup {

    private final Class<?> valueType;
    private final String readType;
    private final long interval;

    public DataReadGroup(Class<?> valueType, String readType, long interval) {
        this.readType = readType;
        this.valueType = valueType;
        this.interval = interval;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public String getReadType() {
        return readType;
    }

    public long getInterval() {
        return interval;
    }
}
