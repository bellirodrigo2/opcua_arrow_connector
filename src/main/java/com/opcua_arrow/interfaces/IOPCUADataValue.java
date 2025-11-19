package com.opcua_arrow.interfaces;

import java.time.Instant;

/**
 * Interface for OPC-UA data values, abstracting away implementation details.
 * This allows the Arrow adapter to work without being coupled to specific OPC-UA libraries.
 * 
 * @param <T> The type of the value contained in this data value
 */
public interface IOPCUADataValue<T> {
    
    /**
     * Gets the source timestamp of the data value.
     * 
     * @return The source timestamp, or null if not available
     */
    Instant getSourceTimestamp();
    
    /**
     * Gets the server timestamp of the data value.
     * 
     * @return The server timestamp, or null if not available
     */
    Instant getServerTimestamp();
    
    /**
     * Gets the actual value with proper typing.
     * 
     * @return The value, or null if not available
     */
    T getValue();
    
    /**
     * Gets the status code indicating the quality of the data.
     * 
     * @return The status code as an integer
     */
    int getStatusCode();
    
    /**
     * Checks if the status code indicates good quality data.
     * 
     * @return true if the data quality is good, false otherwise
     */
    boolean isGood();
}