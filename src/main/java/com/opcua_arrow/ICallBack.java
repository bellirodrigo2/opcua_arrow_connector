package com.opcua_arrow;

import java.util.Collection;

public interface ICallBack {
    void run(Collection<?> batch);

    void onLoopStart();

    void onLoopEnd();
}
