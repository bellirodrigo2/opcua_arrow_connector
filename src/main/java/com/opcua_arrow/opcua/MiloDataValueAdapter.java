package com.opcua_arrow.opcua;

import com.opcua_arrow.interfaces.IOPCUADataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import java.time.Instant;

/**
 * Optimized adapter that wraps Milo's DataValue to implement IOPCUADataValue interface.
 * This isolates Milo-specific code to the OPC-UA implementation layer while leveraging
 * Milo's full precision and built-in methods.
 * 
 * @param <T> The type of value contained in this data value
 */
public class MiloDataValueAdapter<T> implements IOPCUADataValue<T> {
    
    private final DataValue dataValue;
    private final Class<T> valueType;
    
    public MiloDataValueAdapter(DataValue dataValue, Class<T> valueType) {
        this.dataValue = dataValue;
        this.valueType = valueType;
    }
    
    @Override
    public Instant getSourceTimestamp() {
        DateTime sourceTime = dataValue.getSourceTime();
        if (sourceTime == null || sourceTime.isNull()) {
            return null;
        }
        
        // Use Milo's built-in conversion with full precision
        // This handles the OPC-UA epoch conversion and nanosecond precision correctly
        Instant baseInstant = sourceTime.getJavaInstant();
        
        // Add picosecond precision if available (convert to nanoseconds)
        UShort sourcePicos = dataValue.getSourcePicoseconds();
        if (sourcePicos != null && sourcePicos.longValue() > 0) {
            long additionalNanos = sourcePicos.longValue() / 1000; // Convert picoseconds to nanoseconds
            baseInstant = baseInstant.plusNanos(additionalNanos);
        }
        
        return baseInstant;
    }
    
    @Override
    public Instant getServerTimestamp() {
        DateTime serverTime = dataValue.getServerTime();
        if (serverTime == null || serverTime.isNull()) {
            return null;
        }
        
        // Use Milo's built-in conversion with full precision
        // This handles the OPC-UA epoch conversion and nanosecond precision correctly
        Instant baseInstant = serverTime.getJavaInstant();
        
        // Add picosecond precision if available (convert to nanoseconds)
        UShort serverPicos = dataValue.getServerPicoseconds();
        if (serverPicos != null && serverPicos.longValue() > 0) {
            long additionalNanos = serverPicos.longValue() / 1000; // Convert picoseconds to nanoseconds
            baseInstant = baseInstant.plusNanos(additionalNanos);
        }
        
        return baseInstant;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public T getValue() {
        Variant variant = dataValue.getValue();
        if (variant == null || variant.getValue() == null) {
            return null;
        }
        
        Object rawValue = variant.getValue();
        try {
            if (valueType.isInstance(rawValue)) {
                return (T) rawValue;
            } else {
                return convertValue(rawValue, valueType);
            }
        } catch (Exception e) {
            // Value conversion failed, return null
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T convertValue(Object rawValue, Class<T> valueType) {
        if (rawValue == null) {
            return null;
        }
        
        if (valueType == Double.class) {
            if (rawValue instanceof Number) {
                return (T) Double.valueOf(((Number) rawValue).doubleValue());
            }
        } else if (valueType == Float.class) {
            if (rawValue instanceof Number) {
                return (T) Float.valueOf(((Number) rawValue).floatValue());
            }
        } else if (valueType == Boolean.class) {
            return (T) Boolean.valueOf(rawValue.toString());
        } else if (valueType == String.class) {
            return (T) rawValue.toString();
        } else if (valueType == Integer.class) {
            if (rawValue instanceof Number) {
                return (T) Integer.valueOf(((Number) rawValue).intValue());
            }
        } else if (valueType == Long.class) {
            if (rawValue instanceof Number) {
                return (T) Long.valueOf(((Number) rawValue).longValue());
            }
        }
        
        throw new IllegalArgumentException("Cannot convert " + rawValue.getClass() + " to " + valueType);
    }
    
    @Override
    public int getStatusCode() {
        StatusCode statusCode = dataValue.getStatusCode();
        return statusCode != null 
            ? (int) statusCode.getValue()
            : 0x80000000; // Bad status code if null (high bit set indicates bad quality)
    }
    
    @Override
    public boolean isGood() {
        StatusCode statusCode = dataValue.getStatusCode();
        return statusCode != null && statusCode.isGood();
    }
}