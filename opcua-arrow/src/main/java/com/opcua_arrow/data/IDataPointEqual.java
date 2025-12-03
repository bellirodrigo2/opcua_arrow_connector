package com.opcua_arrow.data;

public interface IDataPointEqual {

    /**
     *
     * @param other
     * @return
     */
    boolean isEqual(Object newValue, boolean newIsGood);
}
