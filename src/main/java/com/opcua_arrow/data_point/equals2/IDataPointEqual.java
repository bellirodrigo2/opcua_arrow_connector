package com.opcua_arrow.data_point.equals2;

public interface IDataPointEqual {

    /**
     *
     * @param other
     * @return
     */
    boolean isEqual(Object newValue, boolean newIsGood);
}
