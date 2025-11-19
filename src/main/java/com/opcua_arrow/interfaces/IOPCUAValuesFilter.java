package com.opcua_arrow.interfaces;

import java.util.List;

/**
 * Interface for OPC-UA Values Filtering.
 */
public interface IOPCUAValuesFilter<T,U> {

    /**
     * Filter an Array of values of T type.
     * 
     * @return A future containing the list of data values
     */
    List<IOPCUADataValue<T>> filter(U[] values);
}

