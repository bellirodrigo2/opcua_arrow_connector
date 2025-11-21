package com.opcua_arrow.transform.opcua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.transform.IDataPointEqual;
import com.opcua_arrow.transform.IDataPointParams;
import com.opcua_arrow.transform.IDataValue;
import com.opcua_arrow.transform.ITransform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTransform implements ITransform {

    private static final Logger logger = LoggerFactory.getLogger(DataTransform.class);
    private final BlockingQueue<List<IOPCUADataValue<?>>> source;
    private final BlockingQueue<Map<String, List<IDataValue<?>>>> sink;
    private final Map<String, IDataPointParams> paramsMap;
    private final long pollTimeoutSeconds;
    private final Map<String, List<IDataValue<?>>> groupedDataValues = new HashMap<>();
    private final List<List<IOPCUADataValue<?>>> batchList = new ArrayList<>();

    public DataTransform(BlockingQueue<List<IOPCUADataValue<?>>> source, long pollTimeoutSeconds,
            BlockingQueue<Map<String, List<IDataValue<?>>>> sink, Map<String, IDataPointParams> paramsMap) {
        this.source = source;
        this.sink = sink;
        this.paramsMap = paramsMap;
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    @Override
    public void transform() {

        try {
            while (true) {
                for (List<IDataValue<?>> list : groupedDataValues.values()) {
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
            IDataPointParams params = paramsMap.get(nodeId);
            if (params == null) {
                logger.warn("No parameters found for nodeId: " + nodeId);
                continue;
            }
            String group = params.getGroup();
            IDataPointEqual equalChecker = params.getEquals();
            if (equalChecker != null && !equalChecker.isEqual(opcuaval)) {
                groupedDataValues.computeIfAbsent(group, k -> new ArrayList<>())
                        .add(new DataValue<>(opcuaval, params));
            }
        }

    }
}