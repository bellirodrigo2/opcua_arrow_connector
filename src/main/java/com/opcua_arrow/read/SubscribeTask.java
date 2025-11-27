package com.opcua_arrow.read;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.loop.IReaderTask;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribeTask implements IReaderTask {

    private final Logger logger = LoggerFactory.getLogger(SubscribeTask.class);
    private final ISubscriber subscriber;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<DataPoint> pendingDataPoints = new ArrayList<>();
    private final DataReadGroup readGroup;
    private final IQueue<List<TSValue>> queue;
    private final Object lock = new Object();

    public SubscribeTask(ISubscriber subscriber, DataReadGroup readGroup, IQueue<List<TSValue>> queue) {
        this.subscriber = subscriber;
        this.readGroup = readGroup;
        this.queue = queue;
    }

    private void queuePush(List<TSValue> value) {
        try {
            queue.push(value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isEmpty() {
        return subscriber.getDataPoints().isEmpty();
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        assert dataPoint.getReadGroup() == readGroup
                : "DataPoint's read group does not match SubscribeTask's read group.";
        logger.debug("Adding DataPoint to SubscribeTask: {}", dataPoint);
        synchronized (lock) {
            if (!running.get()) {
                pendingDataPoints.add(dataPoint);
            } else {
                subscriber.addNodesToSubscription(
                        readGroup,
                        List.of(dataPoint),
                        this::queuePush);
            }
        }
    }

    @Override
    public void removeDataPoint(DataPoint dataPoint) {
        assert dataPoint.getReadGroup() == readGroup
                : "DataPoint's read group does not match SubscribeTask's read group.";
        logger.debug("Removing DataPoint from SubscribeTask: {}", dataPoint);
        synchronized (lock) {
            if (!running.get()) {
                pendingDataPoints.remove(dataPoint);
            } else {
                subscriber.removeNodeFromSubscription(
                        readGroup,
                        List.of(dataPoint.getNodeId()));
            }
        }
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (running.compareAndSet(false, true)) {
                if (!pendingDataPoints.isEmpty()) {
                    subscriber.addNodesToSubscription(
                            readGroup,
                            new ArrayList<>(pendingDataPoints),
                            this::queuePush);
                    pendingDataPoints.clear();
                }
            }
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            subscriber.closeSubscription(readGroup);
        }
    }
}
