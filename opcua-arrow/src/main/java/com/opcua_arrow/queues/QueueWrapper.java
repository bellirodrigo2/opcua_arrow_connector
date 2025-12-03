package com.opcua_arrow.queues;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe queue wrapper using LinkedBlockingQueue.
 * Provides batch operations with timeout support.
 *
 * Note: Does not support null values due to LinkedBlockingQueue limitations.
 *
 * @param <T> the type of elements in the queue (must not be null)
 */
public class QueueWrapper<T> implements IQueue<T> {

    private final BlockingQueue<T> queue;
    private final long timeoutMillis;

    /**
     * Creates a new QueueWrapper with specified capacity and timeout.
     *
     * @param capacity      the maximum number of elements the queue can hold
     * @param timeoutMillis timeout in milliseconds for pop operations
     * @throws IllegalArgumentException if capacity <= 0 or timeoutMillis < 0
     */
    public QueueWrapper(int capacity, long timeoutMillis) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive, got: " + capacity);
        }
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Timeout must be non-negative, got: " + timeoutMillis);
        }
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Pushes a value into the queue, blocking if necessary until space is
     * available.
     *
     * @param value the value to push (must not be null)
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if value is null
     */
    @Override
    public void push(T value) throws InterruptedException {
        Objects.requireNonNull(value, "Cannot push null value into queue");
        queue.put(value);
    }

    /**
     * Pops all available elements from the queue into the provided list.
     * Waits up to the configured timeout for at least one element to become
     * available.
     * The provided list is cleared before adding new elements.
     *
     * @param batch the list to populate with popped elements (will be cleared
     *              first)
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if batch is null
     */
    @Override
    public void pop(List<T> batch) throws InterruptedException {
        Objects.requireNonNull(batch, "Batch list cannot be null");
        batch.clear();

        // Wait for first element with timeout
        T first = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        if (first == null) {
            return; // Timeout reached, return empty batch
        }

        batch.add(first);
        // Drain remaining available elements without waiting
        queue.drainTo(batch);
    }

    /**
     * Returns the current size of the queue.
     *
     * @return the number of elements currently in the queue
     */
    public int size() {
        return queue.size();
    }

    /**
     * Returns the remaining capacity of the queue.
     *
     * @return the number of additional elements that can be added without blocking
     */
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue contains no elements
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Checks if the queue is at full capacity.
     *
     * @return true if the queue cannot accept more elements without blocking
     */
    public boolean isFull() {
        return queue.remainingCapacity() == 0;
    }
}
