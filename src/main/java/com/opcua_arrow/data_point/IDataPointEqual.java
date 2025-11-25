package com.opcua_arrow.data_point;

public interface IDataPointEqual {

    /**
     *
     * @param other
     * @return
     */
    boolean isEqual(Object newValue, boolean newIsGood);
}
