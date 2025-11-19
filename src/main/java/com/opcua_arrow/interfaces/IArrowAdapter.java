package com.opcua_arrow.interfaces;

import java.util.List;

/**
 * Interface for converting OPC-UA values to Arrow IPC format.
 *
 * @param <T> The type of scalar values
 */
public interface IArrowAdapter<TId, TValue> {
    
    /**
     * Converts a list of OPC-UA data values to Arrow IPC Stream format.
     * 
     * @param data The OPC-UA data values to convert
     * @return The Arrow IPC Stream as bytes
     */
    byte[] toArrowIPC(List<IOPCUADataValue<TValue>> data);
    
}
