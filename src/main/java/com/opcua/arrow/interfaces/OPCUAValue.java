package com.opcua.arrow.interfaces;

import java.time.Instant;

/**
 * Represents a value read from an OPC-UA server.
 * 
 * @param <T> The type of the scalar value
 */
public class OPCUAValue<T> {
    private final Instant sourceTimestamp;
    private final Instant serverTimestamp;
    private final T value;
    private final int statusCode;

    public OPCUAValue(Instant sourceTimestamp, Instant serverTimestamp, T value, int statusCode) {
        this.sourceTimestamp = sourceTimestamp;
        this.serverTimestamp = serverTimestamp;
        this.value = value;
        this.statusCode = statusCode;
    }

    public Instant getSourceTimestamp() {
        return sourceTimestamp;
    }

    public Instant getServerTimestamp() {
        return serverTimestamp;
    }

    public T getValue() {
        return value;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isBad() {
        // OPC-UA bad status codes have the high bit set (0x80000000)
        return (statusCode & 0x80000000) != 0;
    }

    public Instant getTimestamp() {
        return sourceTimestamp != null ? sourceTimestamp : serverTimestamp;
    }
}
