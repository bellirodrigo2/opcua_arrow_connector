package com.opcua_arrow.read;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.opcua.IOPCUAReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopReader implements IReader {

    private Long intervalSeconds;
    private static final Logger logger = LoggerFactory.getLogger(LoopReader.class);
    private final Map<Long, Set<String>> nodeIdsIntervalMap;
    private final List<String> nodeIds;
    private final IOPCUAReader<?> opcuaReader;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduledTask;

    public LoopReader(Long intervalSeconds, Map<Long, Set<String>> nodeIdsIntervalMap,
            IOPCUAReader<?> opcuaReader) {

        this.intervalSeconds = intervalSeconds;
        this.nodeIdsIntervalMap = nodeIdsIntervalMap;
        this.nodeIds = new ArrayList<>(nodeIdsIntervalMap.get(intervalSeconds));
        this.opcuaReader = opcuaReader;
    }

    @Override
    public void read() {
        startScheduledTask(intervalSeconds);
    }

    private void startScheduledTask(Long intervalSeconds) {
        // iniciar execução periódica

        logger.info("Starting scheduled task with interval: {} seconds", intervalSeconds);
        scheduledTask = executor.scheduleAtFixedRate(() -> {

            opcuaReader.read(nodeIds);

            // Atualizar apenas se mudou
            Set<String> newSet = nodeIdsIntervalMap.get(intervalSeconds);
            if (newSet.size() != nodeIds.size() || !nodeIds.containsAll(newSet)) {
                nodeIds.clear();
                nodeIds.addAll(newSet);
            }

        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void setIntervalSeconds(Long newIntervalSeconds) {

        // 1. cancelar a tarefa atual
        if (scheduledTask != null) {
            scheduledTask.cancel(false); // não interrompe se estiver rodando
            logger.info("Scheduled task cancelled for interval change: from {} to {} .", intervalSeconds,
                    newIntervalSeconds);
        }

        // 2. iniciar nova tarefa com novo intervalo
        startScheduledTask(newIntervalSeconds);
        intervalSeconds = newIntervalSeconds;
    }
}
