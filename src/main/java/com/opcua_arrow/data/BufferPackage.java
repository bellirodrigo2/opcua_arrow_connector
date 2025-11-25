package com.opcua_arrow.data;

public class BufferPackage {

    private final byte[] buffer;
    private final DataWriteGroup group;

    public BufferPackage(byte[] buffer, DataWriteGroup group) {
        this.buffer = buffer;
        this.group = group;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public DataWriteGroup getGroup() {
        return group;
    }
}
