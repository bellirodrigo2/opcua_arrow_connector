package com.opcua_arrow.arrow;

import com.opcua_arrow.interfaces.IArrowAdapter;
import com.opcua_arrow.interfaces.IOPCUADataValue;

import org.apache.arrow.vector.types.pojo.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * High-performance implementation of IArrowAdapter that uses TypedArrowBatchBuilders
 * for efficient memory reuse and reduced allocations.
 * 
 * @param <T> The type of scalar values
 */
public class ArrowAdapter<T> implements IArrowAdapter<T>, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ArrowAdapter.class);
    
    private final List<String> nodeIds;
    private final Map<String, Integer> idLookup;
    private final Schema schema;
    private final ArrowBatchBuilders.BaseArrowBatchBuilder<T> builder;
    
    public ArrowAdapter(List<String> nodeIds, Class<T> valueType, Map<String, Integer> idLookup) {
        this(nodeIds, valueType, idLookup, 1000, false); // Default capacity and no compression
    }
    
    public ArrowAdapter(List<String> nodeIds, Class<T> valueType, Map<String, Integer> idLookup, 
                              int initialCapacity, boolean compressionEnabled) {
        this.nodeIds = nodeIds;
        this.idLookup = idLookup;
        this.schema = SchemaUtils.createSchema(valueType, idLookup);
        this.builder = createBuilder(valueType, schema, initialCapacity, compressionEnabled);
    }
    
    @SuppressWarnings("unchecked")
    private ArrowBatchBuilders.BaseArrowBatchBuilder<T> createBuilder(Class<T> valueType, Schema schema, 
                                                                           int initialCapacity, boolean compressionEnabled) {
        if (valueType == Double.class || valueType == double.class) {
            return (ArrowBatchBuilders.BaseArrowBatchBuilder<T>) 
                new ArrowBatchBuilders.FloatArrowBatchBuilder(schema, initialCapacity, compressionEnabled);
        } else if (valueType == Float.class || valueType == float.class) {
            return (ArrowBatchBuilders.BaseArrowBatchBuilder<T>) 
                new ArrowBatchBuilders.FloatArrowBatchBuilder(schema, initialCapacity, compressionEnabled);
        } else if (valueType == Integer.class || valueType == int.class) {
            return (ArrowBatchBuilders.BaseArrowBatchBuilder<T>) 
                new ArrowBatchBuilders.IntArrowBatchBuilder(schema, initialCapacity, compressionEnabled);
        } else if (valueType == Long.class || valueType == long.class) {
            return (ArrowBatchBuilders.BaseArrowBatchBuilder<T>) 
                new ArrowBatchBuilders.IntArrowBatchBuilder(schema, initialCapacity, compressionEnabled);
        } else if (valueType == Boolean.class || valueType == boolean.class) {
            return (ArrowBatchBuilders.BaseArrowBatchBuilder<T>) 
                new ArrowBatchBuilders.BoolArrowBatchBuilder(schema, initialCapacity, compressionEnabled);
        } else if (valueType == String.class) {
            return (ArrowBatchBuilders.BaseArrowBatchBuilder<T>) 
                new ArrowBatchBuilders.StringArrowBatchBuilder(schema, initialCapacity, compressionEnabled);
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + valueType);
        }
    }
    
    @Override
    public byte[] toArrowIPC(List<IOPCUADataValue<T>> data) {
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
                IOPCUADataValue<T> dataValue = data.get(i);
                String nodeId = nodeIds.get(i);
                
                // Get point ID (convert to Integer - builders expect Integer type)
                Integer pointId = (idLookup != null) ? idLookup.get(nodeId) : nodeId.hashCode();
                
                // Convert timestamp to nanoseconds since epoch
                long timestampNanos = 0;
                Instant timestamp = getTimestamp(dataValue);
                if (timestamp != null) {
                    timestampNanos = timestamp.getEpochSecond() * 1_000_000_000L + timestamp.getNano();
                }
                
                // Get value (null if bad status)
                T value = dataValue.isGood() ? dataValue.getValue() : null;
                
                // Append to builder
                appendToBuilder(builder, pointId, timestampNanos, value, dataValue.getStatusCode());
            }
            
            // Flush and return IPC bytes
            return builder.flush();
            
        } catch (Exception e) {
            logger.error("Failed to convert to Arrow IPC using builder", e);
            throw new RuntimeException("Failed to convert to Arrow IPC", e);
        }
    }
    
    private void appendToBuilder(ArrowBatchBuilders.BaseArrowBatchBuilder<T> builder, 
                                Integer pointId, long timestampNanos, T value, int statusCode) {
        
        if (builder instanceof ArrowBatchBuilders.FloatArrowBatchBuilder) {
            ArrowBatchBuilders.FloatArrowBatchBuilder floatBuilder = 
                (ArrowBatchBuilders.FloatArrowBatchBuilder) builder;
            Double doubleValue = (value != null) ? ((Number) value).doubleValue() : null;
            floatBuilder.append(pointId, timestampNanos, doubleValue, statusCode);
            
        } else if (builder instanceof ArrowBatchBuilders.IntArrowBatchBuilder) {
            ArrowBatchBuilders.IntArrowBatchBuilder intBuilder = 
                (ArrowBatchBuilders.IntArrowBatchBuilder) builder;
            Integer intValue = (value != null) ? ((Number) value).intValue() : null;
            intBuilder.append(pointId, timestampNanos, intValue, statusCode);
            
        } else if (builder instanceof ArrowBatchBuilders.BoolArrowBatchBuilder) {
            ArrowBatchBuilders.BoolArrowBatchBuilder boolBuilder = 
                (ArrowBatchBuilders.BoolArrowBatchBuilder) builder;
            Boolean boolValue = (Boolean) value;
            boolBuilder.append(pointId, timestampNanos, boolValue, statusCode);
            
        } else if (builder instanceof ArrowBatchBuilders.StringArrowBatchBuilder) {
            ArrowBatchBuilders.StringArrowBatchBuilder stringBuilder = 
                (ArrowBatchBuilders.StringArrowBatchBuilder) builder;
            String stringValue = (String) value;
            stringBuilder.append(pointId, timestampNanos, stringValue, statusCode);
            
        } else {
            throw new IllegalArgumentException("Unsupported builder type: " + builder.getClass());
        }
    }
    
    /**
     * Gets the most appropriate timestamp from the data value.
     * Prefers source timestamp, falls back to server timestamp.
     */
    private Instant getTimestamp(IOPCUADataValue<T> dataValue) {
        Instant sourceTimestamp = dataValue.getSourceTimestamp();
        return sourceTimestamp != null ? sourceTimestamp : dataValue.getServerTimestamp();
    }
    
    @Override
    public Schema getSchema() {
        return schema;
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