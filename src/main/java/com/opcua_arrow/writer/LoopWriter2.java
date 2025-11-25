package com.opcua_arrow.writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opcua_arrow.batch_builder.v2.IBufferBuilder;
import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.maps.v2.BufferRegistry;
import com.opcua_arrow.opcua.milo2.TSValue;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopWriter2 implements IWriter {
    private static final Logger logger = LoggerFactory.getLogger(LoopWriter.class);
    private final IQueue<List<TSValue>> source;
    private final ISend sender;
    private final BufferRegistry bufferRegistry;

    private final List<List<TSValue>> reusableDataList = new ArrayList<>();
    private final Map<DataWriteGroup, List<TSValue>> reusableDataMap = new HashMap<>();

    public LoopWriter2(
            BufferRegistry bufferRegistry,
            IQueue<List<TSValue>> source,
            ISend sender) {
        this.bufferRegistry = bufferRegistry;
        this.source = source;
        this.sender = sender;
    }

    @Override
    public void write() {
        try {
            while (true) {
                reusableDataList.clear();
                source.pop(reusableDataList);
                for (List<TSValue> data : reusableDataMap.values()) {
                    data.clear();
                }
                for (List<TSValue> data : reusableDataList) {
                    for (TSValue tsValue : data) {
                        reusableDataMap
                                .computeIfAbsent(tsValue.writeGroup, k -> new ArrayList<>())
                                .add(tsValue);
                    }
                }
                internalWrite(reusableDataMap);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void internalWrite(Map<DataWriteGroup, List<TSValue>> data) {
        for (DataWriteGroup group : data.keySet()) {

            List<TSValue> nodeData = data.get(group);
            if (nodeData.isEmpty())
                continue;

            IBufferBuilder batchBuffer = bufferRegistry.get(group);
            if (batchBuffer == null) {
                logger.warn("No batch buffer found for group: " + group);
                return;
            }

            batchBuffer.appendList(nodeData);
            byte[] batch = batchBuffer.flush();
            if (batch != null && batch.length > 0) {
                sender.send(group, batch);
            }
        }
    }

    @Override
    public void stop() {
        for (IBufferBuilder batchBuffer : bufferRegistry.values()) {
            if (batchBuffer != null) {
                batchBuffer.close();
            }
        }
    }
}
