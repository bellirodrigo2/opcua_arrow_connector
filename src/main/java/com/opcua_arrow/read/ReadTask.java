package com.opcua_arrow.read;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadTask implements IReadTask {

    private static final Logger logger = LoggerFactory.getLogger(ReadTask.class);
    private ScheduledFuture<?> scheduledTask;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final IOPCUAReader opcuaReader;
    private final Long intervalSeconds;
    private final IQueue<List<IOPCUADataValue>> queue;

    public ReadTask(IOPCUAReader opcuaReader, Long intervalSeconds,
            IQueue<List<IOPCUADataValue>> queue) {
        this.opcuaReader = opcuaReader;
        this.intervalSeconds = intervalSeconds;
        this.queue = queue;
    }

    public List<String> getNodeIds() {
        return opcuaReader.getNodeIds();
    }

    public void addNodeId(String nodeId) {
        opcuaReader.addNodeId(nodeId);
    }

    public void removeNodeId(String nodeId) {
        opcuaReader.removeNodeId(nodeId);
    }

    public void start() {
        // iniciar execução periódica
        scheduledTask = executor.scheduleAtFixedRate(() -> {
            opcuaReader.read().thenAccept(values -> {
                try {
                    List<IOPCUADataValue> safeList = new ArrayList<>(values);
                    queue.push(safeList);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).exceptionally(ex -> {
                logger.error("Read error", ex);
                return null;
            });

        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        executor.shutdown();
    }
}
