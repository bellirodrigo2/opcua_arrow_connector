package com.opcua_arrow.data.equals;

@FunctionalInterface
public interface IsSameValue {

    public boolean isSameValue(Object newValue, Object oldValue);
}
