package com.opcua_arrow.opcua.milo;

import java.time.Instant;
import com.opcua_arrow.opcua.IOPCUADataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

/**
 * Lightweight, immutable data value optimized for subscription scenarios.
 * Designed to minimize allocations and be cache-friendly.
 */
public final class MiloSubscriptionDataValue implements IOPCUADataValue {
    
    // Pre-compute and cache all values to avoid repeated calculations
    private final String nodeId;
    private final long timestampNanos;  // Pre-computed timestamp in nanos
    private final Object value;
    private final int statusCode;
    private final boolean good;
    
    // Cache instants only if needed (lazy initialization)
    private volatile Instant sourceTimestamp;
    private volatile Instant serverTimestamp;
    private final DateTime sourceDateTime;
    private final DateTime serverDateTime;
    
    /**
     * Creates a lightweight data value from Milo DataValue.
     * Pre-computes commonly accessed values to avoid repeated calculations.
     */
    public MiloSubscriptionDataValue(DataValue dataValue, String nodeId) {
        this.nodeId = nodeId;
        
        // Pre-extract status code
        StatusCode status = dataValue.getStatusCode();
        this.statusCode = status != null ? (int) status.getValue() : 0x80000000;
        this.good = status != null && status.isGood();
        
        // Extract value directly - no wrapper
        var variant = dataValue.getValue();
        this.value = (variant != null) ? variant.getValue() : null;
        
        // Store DateTime references for lazy Instant creation
        this.sourceDateTime = dataValue.getSourceTime();
        this.serverDateTime = dataValue.getServerTime();
        
        // Pre-compute timestamp as long for fast access
        this.timestampNanos = computeTimestampNanos(dataValue);
    }
    
    /**
     * Pre-computes the timestamp in nanoseconds for fast access.
     * This is the most commonly accessed field in downstream processing.
     */
    private long computeTimestampNanos(DataValue dataValue) {
        DateTime sourceTime = dataValue.getSourceTime();
        UShort sourcePicos = dataValue.getSourcePicoseconds();
        
        if (sourceTime == null || sourceTime.isNull()) {
            sourceTime = dataValue.getServerTime();
            sourcePicos = dataValue.getServerPicoseconds();
            
            if (sourceTime == null || sourceTime.isNull()) {
                return 0;
            }
        }
        
        Instant instant = sourceTime.getJavaInstant();
        long nanos = instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
        
        // Add picoseconds if present (converted to nanos)
        if (sourcePicos != null && sourcePicos.longValue() > 0) {
            nanos += sourcePicos.longValue() / 1000;
        }
        
        return nanos;
    }
    
    @Override
    public String getNodeId() {
        return nodeId;
    }
    
    @Override
    public Instant getSourceTimestamp() {
        if (sourceTimestamp == null && sourceDateTime != null && !sourceDateTime.isNull()) {
            sourceTimestamp = sourceDateTime.getJavaInstant();
        }
        return sourceTimestamp;
    }
    
    @Override
    public Instant getServerTimestamp() {
        if (serverTimestamp == null && serverDateTime != null && !serverDateTime.isNull()) {
            serverTimestamp = serverDateTime.getJavaInstant();
        }
        return serverTimestamp;
    }
    
    @Override
    public long getTimestampLong() {
        return timestampNanos;
    }
    
    @Override
    public Object getValue() {
        return value;
    }
    
    @Override
    public int getStatusCode() {
        return statusCode;
    }
    
    @Override
    public boolean isGood() {
        return good;
    }
    
    @Override
    public boolean isConsistent() {
        // For subscription data, we consider it consistent if we have a valid timestamp
        return timestampNanos > 0;
    }
}
