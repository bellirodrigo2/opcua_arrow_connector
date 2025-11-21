package com.opcua_arrow.factory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.transform.IDataPointParams;
import com.opcua_arrow.transform.IDataValue;
import com.opcua_arrow.transform.ITransform;
import com.opcua_arrow.transform.opcua.DataTransform;

public class DataTransformFactory {

    static ITransform createDataTransform(
            Long pollTimeoutSeconds,
            BlockingQueue<List<IOPCUADataValue<?>>> source,
            BlockingQueue<Map<String, List<IDataValue<?>>>> sink,
            Map<String, IDataPointParams> paramsMap) {
        return new DataTransform(source, pollTimeoutSeconds, sink, paramsMap);
    }
}