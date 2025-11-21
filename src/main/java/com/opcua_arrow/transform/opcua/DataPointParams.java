package com.opcua_arrow.transform.opcua;

import com.opcua_arrow.transform.IDataPointEqual;
import com.opcua_arrow.transform.IDataPointParams;

public class DataPointParams implements IDataPointParams {

    private final int pointId;
    private final IDataPointEqual equals;
    private final String group;

    public DataPointParams(int pointId, IDataPointEqual equals, String group) {
        this.pointId = pointId;
        this.equals = equals;
        this.group = group;
    }

    @Override
    public int getPointId() {
        return pointId;
    }

    @Override
    public IDataPointEqual getEquals() {
        return equals;
    }

    @Override
    public String getGroup() {
        return group;
    }

}
