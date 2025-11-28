package com.opcua_arrow;

import com.opcua_arrow.data.DataPoint;

public interface IContext {

    void update(DataPoint dataPoint);

    void delete(String id);
}
