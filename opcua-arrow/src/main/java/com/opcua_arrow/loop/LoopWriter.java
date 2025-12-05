package com.opcua_arrow.loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.opcua_arrow.ICallBack;
import com.opcua_arrow.ICallBack.ICallBackObject;
import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.data.BufferPackage;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopWriter implements ILoop {
    private static final Logger logger = LoggerFactory.getLogger(LoopWriter.class);

    // Virtual threads executor — 1 virtual thread per loop iteration
    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    private final IQueue<List<TSValue>> source;
    private final IQueue<BufferPackage> sink;
    private final IBatchBufferFactory factory;
    private final ICallBack callBack;

    private final ConcurrentHashMap<DataWriteGroup, IBufferBuilder> writerMap = new ConcurrentHashMap<>();

    // These are safe because loop runs in 1 VT (concurrency-safe)
    private final List<List<TSValue>> reusableDataList = new ArrayList<>();
    private final Map<DataWriteGroup, List<TSValue>> reusableDataMap = new HashMap<>();

    private final String label = "WriteLoop";

    public LoopWriter(
            IQueue<List<TSValue>> source,
            IQueue<BufferPackage> sink,
            IBatchBufferFactory factory,
            ICallBack callBack) {

        this.source = source;
        this.sink = sink;
        this.factory = factory;
        this.callBack = callBack;
    }

    @Override
    public void addDataPoint(DataPoint dp) {
        DataWriteGroup newWriteGroup = dp.getWriteGroup();
        writerMap.computeIfAbsent(newWriteGroup, factory::createBatchBuffer);
    }

    @Override
    public void removeDataPoint(DataPoint dp) {
        DataWriteGroup writeGroup = dp.getWriteGroup();
        IBufferBuilder bufferBuilder = writerMap.remove(writeGroup);
        if (bufferBuilder != null) {
            bufferBuilder.close();
        }
    }

    @Override
    public void start() {
        executor.submit(this::loop);
    }

    private void loop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {

                reusableDataList.clear();
                source.pop(reusableDataList); // blocking, safe in virtual thread
                if (reusableDataList.isEmpty())
                    continue;

                ICallBackObject ac = callBack.startCallback(label, reusableDataList);
                try {
                    // Reset buffers
                    reusableDataMap.values().forEach(List::clear);

                    // Group TSValue by write group
                    reusableDataList.stream()
                            .flatMap(List::stream)
                            .forEach(tsValue -> reusableDataMap
                                    .computeIfAbsent(tsValue.writeGroup, k -> new ArrayList<>())
                                    .add(tsValue));
                    // Add Key Value pairs to the callback ??
                    internalWrite(reusableDataMap);
                } catch (Exception e) {
                    logger.error("Error in LoopWriter loop: ", e);
                    ac.markFailure(e);
                    throw e;
                } finally {
                    ac.close();
                }
            }
        } catch (

        InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void internalWrite(Map<DataWriteGroup, List<TSValue>> data) {
        for (var entry : data.entrySet()) {
            DataWriteGroup group = entry.getKey();
            List<TSValue> nodeData = entry.getValue();

            if (nodeData.isEmpty())
                continue;

            IBufferBuilder batchBuffer = writerMap.get(group);
            if (batchBuffer == null) {
                logger.warn("No batch buffer found for group: {}", group);
                continue;
            }

            batchBuffer.appendList(nodeData);
            byte[] batch = batchBuffer.flush();

            if (batch != null && batch.length > 0) {
                try {
                    sink.push(new BufferPackage(batch, group));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void stop() {
        writerMap.values().forEach(IBufferBuilder::close);
        executor.shutdownNow();
    }
}
