package com.opcua_arrow.loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.opcua_arrow.ICallBack;
import com.opcua_arrow.ICallBack.ICallBackObject;
import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.data.BufferPackage;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.di.FactoryModule.BatchBufferFactory;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopWriter implements ILoop {
    private static final Logger logger = LoggerFactory.getLogger(LoopWriter.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final IQueue<List<TSValue>> source;
    private final IQueue<BufferPackage> sink;
    private final BatchBufferFactory factory;
    private final ICallBack callBack;

    private final ConcurrentHashMap<DataWriteGroup, IBufferBuilder> writerMap = new ConcurrentHashMap<>();

    private final List<List<TSValue>> reusableDataList = new ArrayList<>();
    private final Map<DataWriteGroup, List<TSValue>> reusableDataMap = new HashMap<>();
    private final String label = "WriteLoop";

    public LoopWriter(
            IQueue<List<TSValue>> source,
            IQueue<BufferPackage> sink,
            BatchBufferFactory factory,
            ICallBack callBack) {
        this.source = source;
        this.sink = sink;
        this.factory = factory;
        this.callBack = callBack;
    }

    @Override
    public void addDataPoint(DataPoint DataPoint) {
        DataWriteGroup newWriteGroup = DataPoint.getWriteGroup();
        writerMap.computeIfAbsent(newWriteGroup, factory::createBatchBuffer);
    }

    @Override
    public void removeDataPoint(DataPoint DataPoint) {
        DataWriteGroup writeGroup = DataPoint.getWriteGroup();
        IBufferBuilder bufferBuilder = writerMap.remove(writeGroup);
        if (bufferBuilder != null) {
            bufferBuilder.close();
        }
    }

    @Override
    public void start() {
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {

                    reusableDataList.clear();
                    source.pop(reusableDataList);
                    if (reusableDataList.isEmpty())
                        continue;
                    ICallBackObject ac = callBack.startCallback(label, reusableDataList);
                    try {
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
                    } finally {
                        ac.close();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void internalWrite(Map<DataWriteGroup, List<TSValue>> data) {
        for (DataWriteGroup group : data.keySet()) {

            List<TSValue> nodeData = data.get(group);
            if (nodeData.isEmpty())
                continue;

            IBufferBuilder batchBuffer = writerMap.get(group);
            if (batchBuffer == null) {
                logger.warn("No batch buffer found for group: " + group);
                return;
            }

            batchBuffer.appendList(nodeData);
            byte[] batch = batchBuffer.flush();
            if (batch != null && batch.length > 0) {
                try {
                    sink.push(new BufferPackage(batch, group));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ;
            }
        }
    }

    @Override
    public void stop() {
        for (IBufferBuilder batchBuffer : writerMap.values()) {
            if (batchBuffer != null) {
                batchBuffer.close();
            }
        }
        executor.shutdownNow();
    }
}
