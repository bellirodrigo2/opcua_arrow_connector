package com.opcua_arrow.service;

public class DataPointDTO {
    public final String nodeId;
    public final String valueType; // define numeric, string, boolean

    public final Integer pointId; // unique integer id
    public final String groupName; // map to partitions
    public final int minRange;
    public final int maxRange;
    public final double filterRange;
    public final long filterIntervalSeconds;

    public final String readType;
    public final long interval_seconds; // loop interval

    public DataPointDTO(String nodeId, String valueType, Integer pointId, String groupName, int minRange,
            int maxRange, double filterRange, long filterIntervalSeconds,
            long interval_seconds, String readType) {
        this.nodeId = nodeId;
        this.valueType = valueType;
        this.pointId = pointId;
        this.groupName = groupName;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.filterRange = filterRange;
        this.filterIntervalSeconds = filterIntervalSeconds;
        this.interval_seconds = interval_seconds;
        this.readType = readType;
    }
}
