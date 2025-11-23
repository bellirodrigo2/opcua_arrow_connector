package com.opcua_arrow.data_point;

public interface IDataPointParams {

    int getPointId();

    IDataPointEqual getEquals();

    DataWriteGroup getWriteGroup();

    DataReadGroup getReadGroup();

    String getNodeId();

    Class<?> getValueTypeClass();
}
