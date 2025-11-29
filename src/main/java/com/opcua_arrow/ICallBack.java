package com.opcua_arrow;

import java.util.Collection;

public interface ICallBack {
    ICallBackObject startCallback(String label, Collection<?> batch);

    public interface ICallBackObject {

        void close();
    }
}
