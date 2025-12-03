package com.opcua_arrow.queues;

import java.util.List;

public interface IQueue<T> {
    void push(T value) throws InterruptedException;

    /**
     * Pega 1 item com timeout e depois drena o restante.
     *
     * @param batchList
     * @throws InterruptedException
     */
    void pop(List<T> batchList) throws InterruptedException;
}
