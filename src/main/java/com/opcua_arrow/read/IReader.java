package com.opcua_arrow.read;

public interface IReader {
    void read();

    void setIntervalSeconds(Long interval_seconds);
}
