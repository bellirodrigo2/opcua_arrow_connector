package com.opcua_arrow.data;

public class DataPoint {

    private final String name;
    private final String description;
    private final String nodeId;
    private final int pointId;
    private final EDataType dataType;
    private final IDataPointEqual equals;
    private final DataWriteGroup writeGroup;
    private final DataReadGroup readGroup;

    public DataPoint(String name,
            String description,
            String nodeId,
            int pointId,
            EDataType dataType,
            IDataPointEqual equals,
            DataWriteGroup writeGroup,
            DataReadGroup readGroup) {
        this.name = name;
        this.description = description;
        this.nodeId = nodeId;
        this.pointId = pointId;
        this.dataType = dataType;
        this.equals = equals;
        this.writeGroup = writeGroup;
        this.readGroup = readGroup;

    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public EDataType getDataType() {
        return dataType;
    }

    public int getPointId() {
        return pointId;
    }

    public IDataPointEqual getEquals() {
        return equals;
    }

    public DataWriteGroup getWriteGroup() {
        return writeGroup;
    }

    public DataReadGroup getReadGroup() {
        return readGroup;
    }

    public String getNodeId() {
        return nodeId;
    }

}
