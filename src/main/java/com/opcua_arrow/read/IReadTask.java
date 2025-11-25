package com.opcua_arrow.read;

import java.util.List;

public interface IReadTask {
    List<String> getNodeIds();

    void addNodeId(String nodeId);

    void removeNodeId(String nodeId);

    void start();

    void stop();

}
