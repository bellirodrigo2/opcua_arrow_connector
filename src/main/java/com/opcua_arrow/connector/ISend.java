package com.opcua_arrow.connector;

public interface ISend {
    void send(String group, byte[] data);
}
