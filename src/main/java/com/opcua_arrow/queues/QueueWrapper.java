package com.opcua_arrow.queues;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.ICallBack;

public class QueueWrapper<T> implements IQueue<T> {

    private final BlockingQueue<T> queue;
    private final long timeoutMillis;
    private final ICallBack callBack;

    public QueueWrapper(int capacity, long timeoutMillis, ICallBack callBack) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.timeoutMillis = timeoutMillis;
        this.callBack = callBack;
    }

    @Override
    public void push(T value) throws InterruptedException {
        queue.put(value);
    }

    @Override
    public void pop(List<T> batch) throws InterruptedException {
        batch.clear();

        T first = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        if (first == null)
            return;

        batch.add(first);
        queue.drainTo(batch);
        if (callBack != null) {
            callBack.run(batch);
        }
    }
}
