package com.opcua_arrow.transform;

public interface IDataPointParams {

    /**
     * Gets the point ID associated with this data value.
     * 
     * @return The point ID as an integer
     */
    int getPointId();

    /**
     * Get the equals Object
     * 
     * @return
     */
    IDataPointEqual getEquals();

    /**
     * 
     * @return
     */

    String getGroup();

}
