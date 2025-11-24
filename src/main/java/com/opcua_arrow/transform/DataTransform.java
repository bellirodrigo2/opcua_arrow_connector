package com.opcua_arrow.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opcua_arrow.data_point.DataPointParams;
import com.opcua_arrow.data_point.DataValue;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.IDataPointEqual;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTransform implements ITransform {

    private static final Logger logger = LoggerFactory.getLogger(DataTransform.class);
    private final IQueue<List<IOPCUADataValue>> source;
    private final IQueue<Map<DataWriteGroup, List<DataValue>>> sink;
    private final Map<String, DataPointParams> paramsMap;

    // Reusable collections to minimize object creation
    private final Map<DataWriteGroup, List<DataValue>> groupedDataValues = new HashMap<>();
    private final List<List<IOPCUADataValue>> batchList = new ArrayList<>();

    public DataTransform(IQueue<List<IOPCUADataValue>> source, long pollTimeoutSeconds,
            IQueue<Map<DataWriteGroup, List<DataValue>>> sink, Map<String, DataPointParams> paramsMap) {
        this.source = source;
        this.sink = sink;
        this.paramsMap = paramsMap;
    }

    @Override
    public void transform() {

        try {
            while (true) {
                for (List<DataValue> list : groupedDataValues.values()) {
                    list.clear();
                }
                batchList.clear();

                source.pop(batchList);

                // Processa todos os batches
                for (List<IOPCUADataValue> opcuaValues : batchList) {
                    if (opcuaValues != null) {
                        processOpcuaValues(opcuaValues);
                    }
                }

                if (!groupedDataValues.isEmpty()) {
                    sink.push(groupedDataValues);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processOpcuaValues(List<IOPCUADataValue> opcuaValues) {

        for (IOPCUADataValue opcuaval : opcuaValues) {
            String nodeId = opcuaval.getNodeId();
            DataPointParams params = paramsMap.get(nodeId);
            if (params == null) {
                logger.warn("No parameters found for nodeId: " + nodeId);
                continue;
            }
            DataWriteGroup group = params.getWriteGroup();
            IDataPointEqual equalChecker = params.getEquals();
            if (equalChecker != null && !equalChecker.isEqual(opcuaval)) {
                groupedDataValues.computeIfAbsent(group, k -> new ArrayList<>())
                        .add(new DataValue(opcuaval, params));
            }
        }

    }
}
