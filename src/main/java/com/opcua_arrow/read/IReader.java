package com.opcua_arrow.read;

import com.opcua_arrow.data_point.DataReadGroup;

public interface IReader {
    void addNodeId(DataReadGroup readGroup, String nodeId);

    void removeNodeId(DataReadGroup readGroup, String nodeId);

    void read();

    void stop();

}
