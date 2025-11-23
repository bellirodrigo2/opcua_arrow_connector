package com.opcua_arrow.data_point;

public class DataWriteGroup {

    private final String group;
    private final Class<?> valueType;
    private final IntRange pointIdRange;

    public DataWriteGroup(String group, Class<?> valueType, IntRange pointIdRange) {
        this.group = group;
        this.valueType = valueType;
        this.pointIdRange = pointIdRange;
    }

    public String getGroupName() {
        return group;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public IntRange getPointIdRange() {
        return pointIdRange;
    }

}