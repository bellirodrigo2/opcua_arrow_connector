package com.opcua_arrow.arrow;

import com.opcua_arrow.interfaces.IArrowAdapter;
import com.opcua_arrow.interfaces.IOPCUADataValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * High-performance implementation of IArrowAdapter that uses TypedArrowBatchBuilders
 * for efficient memory reuse and reduced allocations.
 * 
 * @param <TValue> The type of scalar values
 */
public class ArrowAdapter<TId, TValue> implements IArrowAdapter<TId, TValue>, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ArrowAdapter.class);
    
    private final List<String> nodeIds;
    private final boolean hasPointIds;
    private final IArrowBatchBuffer<TId, TValue> builder;
    
    public ArrowAdapter(List<String> nodeIds, boolean hasPointIds, IArrowBatchBuffer<TId, TValue> builder) {
        this.nodeIds = nodeIds;
        this.hasPointIds = hasPointIds;
        this.builder = builder;
    }
    
    
    @Override
    public byte[] toArrowIPC(List<IOPCUADataValue<TValue>> data) {
        if (data.size() != nodeIds.size()) {
            throw new IllegalArgumentException(
                String.format("Data size (%d) doesn't match node IDs size (%d)", 
                    data.size(), nodeIds.size()));
        }
        
        try {
            // Note: Builders don't have clear() method, so we need to use pop() to remove existing data
            while (builder.size() > 0) {
                builder.pop();
            }
            
            // Add all data points to builder
            for (int i = 0; i < data.size(); i++) {
                IOPCUADataValue<TValue> dataValue = data.get(i);
                
                // Convert timestamp to nanoseconds since epoch
                long timestampNanos = 0;
                Instant timestamp = getTimestamp(dataValue);
                if (timestamp != null) {
                    timestampNanos = timestamp.getEpochSecond() * 1_000_000_000L + timestamp.getNano();
                }
                TId id = hasPointIds
                    ? (TId) dataValue.getPointId()
                    : (TId) dataValue.getNodeId();
                // Get value (null if bad status)
                TValue value = dataValue.isGood() ? dataValue.getValue() : null;
                
                // Append to builder
                builder.append(id, timestampNanos, value, i);
            }
            
            // Flush and return IPC bytes
            return builder.flush();
            
        } catch (Exception e) {
            logger.error("Failed to convert to Arrow IPC using builder", e);
            throw new RuntimeException("Failed to convert to Arrow IPC", e);
        }
    }
    
    /**
     * Gets the most appropriate timestamp from the data value.
     * Prefers source timestamp, falls back to server timestamp.
     */
    private Instant getTimestamp(IOPCUADataValue<TValue> dataValue) {
        Instant sourceTimestamp = dataValue.getSourceTimestamp();
        return sourceTimestamp != null ? sourceTimestamp : dataValue.getServerTimestamp();
    }
    
    /**
     * Gets the current size (number of rows) in the builder.
     */
    public int size() {
        return builder.size();
    }
    
    /**
     * Gets the current capacity of the builder.
     */
    public int capacity() {
        return builder.capacity();
    }
    
    /**
     * Clears all data from the builder without deallocating memory.
     */
    public void clear() {
        while (builder.size() > 0) {
            builder.pop();
        }
    }
    
    @Override
    public void close() throws Exception {
        if (builder != null) {
            builder.close();
        }
    }
}