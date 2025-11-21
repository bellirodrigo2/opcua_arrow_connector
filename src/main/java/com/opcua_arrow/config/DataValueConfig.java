package com.opcua_arrow.config;

public class DataValueConfig {
    private String nodeId;
    private String valueType; // define numeric, string, boolean

    private Integer pointId; // unique integer id
    private String group; // map to partitions
    private FilterConfig filter;

    private long interval_seconds; // loop interval

    public String getNodeId() {
        return nodeId;
    }

    public Integer getPointId() {
        return pointId;
    }

    public String getValueType() {
        return valueType;
    }

    public String getGroup() {
        return group;
    }

    public long getInterval() {
        return interval_seconds;
    }

    public FilterConfig getFilter() {
        return filter;
    }
}
