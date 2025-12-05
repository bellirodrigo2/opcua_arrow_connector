package com.opcua_arrow.data;

public class DataWriteGroup {

    private final EDataType dataType;
    private final IntRange pointIdRange;

    public DataWriteGroup(EDataType dataType, IntRange pointIdRange) {
        this.dataType = dataType;
        this.pointIdRange = pointIdRange;
    }

    public String toString() {
        return "DataWriteGroup{dataType=" + dataType + ", pointIdRange=" + pointIdRange + "}";
    }

    public EDataType getDataType() {
        return dataType;
    }

    public IntRange getPointIdRange() {
        return pointIdRange;
    }

}
