package com.opcua_arrow.connector;

import com.opcua_arrow.data.DataWriteGroup;

public interface ISend {
    void send(DataWriteGroup group, byte[] data);
}
