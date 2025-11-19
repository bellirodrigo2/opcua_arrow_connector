package com.opcua_arrow.config;

import java.util.List;
import java.util.Map;

public class DataValueConfig {
    private List<String> nodeIds;
    private Map<String, Integer> idLookup;
    private String valueType;
    private int initialCapacity;
    private boolean compressionEnabled;

    public List<String> getNodeIds() {
        return nodeIds;
    }
    public void setNodeIds(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }
    public Map<String, Integer> getIdLookup() {
        return idLookup;
    }
    public String getValueType() {
        return valueType;
    }
    public int getInitialCapacity() {
        return initialCapacity;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
}
