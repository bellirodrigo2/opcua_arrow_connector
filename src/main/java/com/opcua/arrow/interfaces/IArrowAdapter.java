package com.opcua.arrow.interfaces;

import java.util.List;
import org.apache.arrow.vector.types.pojo.Schema;

/**
 * Interface for converting OPC-UA values to Arrow IPC format.
 *
 * @param <T> The type of scalar values
 */
public interface IArrowAdapter<T> {
    
    /**
     * Converts a list of OPC-UA values to Arrow IPC Stream format.
     * 
     * @param data The OPC-UA values to convert
     * @return The Arrow IPC Stream as bytes
     */
    byte[] toArrowIPC(List<OPCUAValue<T>> data);
    
    /**
     * Gets the Arrow schema for the data.
     * 
     * @return The Arrow schema
     */
    Schema getSchema();
}
