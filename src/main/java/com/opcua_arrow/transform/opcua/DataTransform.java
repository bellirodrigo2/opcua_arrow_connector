package com.opcua_arrow.transform.opcua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.IDataPointEqual;
import com.opcua_arrow.data_point.opcua.DataPointParams;
import com.opcua_arrow.data_point.opcua.DataValue;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.transform.ITransform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTransform implements ITransform {

    private static final Logger logger = LoggerFactory.getLogger(DataTransform.class);
    private final BlockingQueue<List<IOPCUADataValue<?>>> source;
    private final BlockingQueue<Map<DataWriteGroup, List<DataValue<?>>>> sink;
    private final Map<String, DataPointParams> paramsMap;
    private final long pollTimeoutSeconds;
    private final Map<DataWriteGroup, List<DataValue<?>>> groupedDataValues = new HashMap<>();
    private final List<List<IOPCUADataValue<?>>> batchList = new ArrayList<>();

    public DataTransform(BlockingQueue<List<IOPCUADataValue<?>>> source, long pollTimeoutSeconds,
            BlockingQueue<Map<DataWriteGroup, List<DataValue<?>>>> sink, Map<String, DataPointParams> paramsMap) {
        this.source = source;
        this.sink = sink;
        this.paramsMap = paramsMap;
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    @Override
    public void transform() {

        try {
            while (true) {
                for (List<DataValue<?>> list : groupedDataValues.values()) {
                    list.clear();
                }
                batchList.clear();

                // Poll primeiro batch (blocking)
                List<IOPCUADataValue<?>> firstBatch = source.poll(pollTimeoutSeconds, TimeUnit.SECONDS);
                if (firstBatch == null) {
                    continue;
                }

                batchList.add(firstBatch);

                // Drain batches adicionais (non-blocking)
                source.drainTo(batchList);

                // Processa todos os batches
                for (List<IOPCUADataValue<?>> opcuaValues : batchList) {
                    if (opcuaValues != null) {
                        processOpcuaValues(opcuaValues);
                    }
                }

                if (!groupedDataValues.isEmpty()) {
                    sink.put(groupedDataValues);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processOpcuaValues(List<IOPCUADataValue<?>> opcuaValues) {

        for (IOPCUADataValue<?> opcuaval : opcuaValues) {
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
                        .add(new DataValue<>(opcuaval, params));
            }
        }

    }
}