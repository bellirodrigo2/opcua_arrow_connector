package com.opcua_arrow.read;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.opcua_arrow.ICallBack;
import com.opcua_arrow.ICallBack.ICallBackObject;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.loop.IReaderTask;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task que faz leituras periódicas de forma síncrona,
 * executando cada leitura em um virtual thread.
 */
public class ReadTask implements IReaderTask {

    private static final Logger logger = LoggerFactory.getLogger(ReadTask.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> scheduledTask;

    private final IReader opcuaReader;
    private final Long intervalMilliSeconds;
    private final IQueue<List<TSValue>> queue;
    private final ICallBack callBack;
    private final String label;

    private final AtomicReference<List<DataPoint>> dataPointsRef = new AtomicReference<>(List.of());

    public ReadTask(IReader opcuaReader,
            Long intervalMilliSeconds,
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
        return dataPointsRef.get().isEmpty();
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        if (dataPoint == null) {
            throw new IllegalArgumentException("dataPoint cannot be null");
        }

        applyUpdate(old -> {
            List<DataPoint> newList = new ArrayList<>(old);
            newList.add(dataPoint);
            return List.copyOf(newList);
        });
    }

    @Override
    public void removeDataPoint(DataPoint dataPoint) {
        if (dataPoint == null) {
            return;
        }

        applyUpdate(old -> old.stream()
                .filter(dp -> !dp.equals(dataPoint))
                .collect(Collectors.toUnmodifiableList()));
    }

    private void applyUpdate(Function<List<DataPoint>, List<DataPoint>> updater) {
        while (true) {
            List<DataPoint> oldList = dataPointsRef.get();
            List<DataPoint> newList = updater.apply(oldList);

            if (dataPointsRef.compareAndSet(oldList, newList)) {
                return;
            }
        }
    }

    @Override
    public void start() {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            List<DataPoint> snapshot = dataPointsRef.get();

            if (snapshot.isEmpty()) {
                return;
            }

            // Cada ciclo de leitura roda em um virtual thread separado.
            Thread.startVirtualThread(() -> executeRead(snapshot));

        }, 0, intervalMilliSeconds, TimeUnit.MILLISECONDS);

        logger.info("ReadTask started with interval {}ms", intervalMilliSeconds);
    }

    private void executeRead(List<DataPoint> snapshot) {
        ICallBackObject ac = callBack.startCallback(label, snapshot);
        try {
            List<TSValue> values = opcuaReader.read(snapshot);
            List<TSValue> safeList = new ArrayList<>(values);
            queue.push(safeList);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while pushing to queue", e);
        } catch (Exception e) {
            logger.error("Read error for {} data points", snapshot.size(), e);
        } finally {
            ac.close();
        }
    }

    @Override
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            logger.info("ReadTask scheduled task cancelled");
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.warn("ReadTask scheduler force shutdown");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error("ReadTask scheduler shutdown interrupted", e);
        }

        logger.info("ReadTask stopped");
    }
}
