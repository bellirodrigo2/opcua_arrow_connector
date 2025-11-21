package com.opcua_arrow.transform;

import com.opcua_arrow.opcua.IOPCUADataValue;

public interface IDataPointEqual {

    /**
     * 
     * @param other
     * @return
     */
    boolean isEqual(IOPCUADataValue<?> anObject);
}
