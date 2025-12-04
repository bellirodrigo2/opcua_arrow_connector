package com.opcua_arrow;

import java.util.Collection;

public interface ICallBack {
    ICallBackObject startCallback(String label, Collection<?> batch);

    public interface ICallBackObject {
        void markFailure(Throwable t);

        void addKeyValue(String key, Object value);

        void close();
    }
}
