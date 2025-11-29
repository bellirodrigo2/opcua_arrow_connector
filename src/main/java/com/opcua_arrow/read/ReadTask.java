package com.opcua_arrow.read;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.ICallBack;
import com.opcua_arrow.ICallBack.ICallBackObject;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.loop.IReaderTask;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadTask implements IReaderTask {

    private static final Logger logger = LoggerFactory.getLogger(ReadTask.class);
    private ScheduledFuture<?> scheduledTask;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final IReader opcuaReader;
    private final Long intervalMilliSeconds;
    private final IQueue<List<TSValue>> queue;
    private final ICallBack callBack;
    private final String label;

    public ReadTask(IReader opcuaReader, Long intervalMilliSeconds,
            IQueue<List<TSValue>> queue,
            ICallBack callBack) {
        this.opcuaReader = opcuaReader;
        this.intervalMilliSeconds = intervalMilliSeconds;
        this.queue = queue;
        this.callBack = callBack;
        this.label = "read_" + intervalMilliSeconds + "_ms";
    }

    @Override
    public boolean isEmpty() {
        return opcuaReader.getDataPoints().isEmpty();
    }

    @Override
    public void addDataPoint(DataPoint DataPoint) {
        opcuaReader.addDataPoint(DataPoint);
    }

    @Override
    public void removeDataPoint(DataPoint DataPoint) {
        opcuaReader.removeDataPoint(DataPoint);
    }

    @Override
    public void start() {
        // iniciar execução periódica
        scheduledTask = executor.scheduleAtFixedRate(() -> {
            ICallBackObject ac = callBack.startCallback(label, opcuaReader.getDataPoints());
            opcuaReader.read().thenAccept(values -> {
                try {
                    List<TSValue> safeList = new ArrayList<>(values);
                    queue.push(safeList);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    ac.close();
                }
            }).exceptionally(ex -> {
                logger.error("Read error", ex);
                return null;
            });

        }, 0, intervalMilliSeconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        executor.shutdown();
    }
}
