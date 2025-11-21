package com.opcua_arrow.context;

@FunctionalInterface
public interface IntervalUpdateCallback {
    void onIntervalUpdate(String nodeId, Long oldInterval, Long newInterval);
}
