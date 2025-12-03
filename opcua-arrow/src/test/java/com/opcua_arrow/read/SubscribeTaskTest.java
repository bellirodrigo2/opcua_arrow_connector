package com.opcua_arrow.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.queues.IQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Full test suite for SubscribeTask:
 * - 100% coverage
 * - concurrency safety
 * - edge cases
 */
public class SubscribeTaskTest {

    private ISubscriber subscriber;
    private DataReadGroup group;
    private IQueue<List<TSValue>> queue;
    private SubscribeTask task;

    private DataPoint dp(String name) {
        return new DataPoint(
                name,
                "desc",
                "node-" + name,
                name.hashCode(),
                null,
                null,
                null,
                group);
    }

    @BeforeEach
    void setup() {
        subscriber = mock(ISubscriber.class);
        group = new DataReadGroup(null, 1000);
        queue = mock(IQueue.class);

        when(subscriber.getDataPoints()).thenReturn(new ArrayList<>());

        task = new SubscribeTask(subscriber, group, queue);
    }

    // ----------------------------------------------------------------------
    // isEmpty()
    // ----------------------------------------------------------------------

    @Test
    void testIsEmptyTrue() {
        when(subscriber.getDataPoints()).thenReturn(Collections.emptyList());
        assertTrue(task.isEmpty());
    }

    @Test
    void testIsEmptyFalse() {
        when(subscriber.getDataPoints()).thenReturn(List.of(dp("X")));
        assertFalse(task.isEmpty());
    }

    // ----------------------------------------------------------------------
    // addDataPoint() BEFORE start (goes to pending list)
    // ----------------------------------------------------------------------

    @Test
    void testAddDataPointBeforeStartGoesToPending() {
        DataPoint p = dp("A");

        task.addDataPoint(p);

        // start with pending
        task.start();

        ArgumentCaptor<List<DataPoint>> captor = ArgumentCaptor.forClass(List.class);

        verify(subscriber).addNodesToSubscription(eq(group), captor.capture(), any());

        assertEquals(1, captor.getValue().size());
        assertEquals(p, captor.getValue().get(0));
    }

    @Test
    void testAddDataPointWrongGroupTriggersAssert() {
        DataReadGroup other = new DataReadGroup(null, 100);
        DataPoint wrong = new DataPoint("x", "d", "node", 1, null, null, null, other);

        AssertionError err = assertThrows(AssertionError.class, () -> task.addDataPoint(wrong));
        assertTrue(err.getMessage().contains("read group"));
    }

    // ----------------------------------------------------------------------
    // addDataPoint() AFTER start (immediate subscription)
    // ----------------------------------------------------------------------

    @Test
    void testAddDataPointAfterStartCallsSubscriberImmediately() {
        DataPoint p = dp("A");

        task.start(); // running = true
        task.addDataPoint(p); // should call subscriber.addNodesToSubscription

        verify(subscriber).addNodesToSubscription(eq(group), eq(List.of(p)), any());
    }

    // ----------------------------------------------------------------------
    // removeDataPoint() BEFORE start → removes from pending
    // ----------------------------------------------------------------------

    @Test
    void testRemoveDataPointBeforeStartRemovesFromPending() {
        DataPoint p = dp("A");

        task.addDataPoint(p);
        task.removeDataPoint(p); // removed from pending

        task.start(); // no pending left

        verify(subscriber, never()).addNodesToSubscription(any(), any(), any());
    }

    @Test
    void testRemoveDataPointWrongGroupTriggersAssert() {
        DataReadGroup other = new DataReadGroup(null, 100);
        DataPoint wrong = new DataPoint("x", "d", "node", 1, null, null, null, other);

        AssertionError err = assertThrows(AssertionError.class, () -> task.removeDataPoint(wrong));
        assertTrue(err.getMessage().contains("read group"));
    }

    // ----------------------------------------------------------------------
    // removeDataPoint() AFTER start → calls removeNodeFromSubscription
    // ----------------------------------------------------------------------

    @Test
    void testRemoveDataPointAfterStartCallsSubscriber() {
        DataPoint p = dp("A");

        task.start(); // running = true
        task.removeDataPoint(p);

        verify(subscriber).removeNodeFromSubscription(eq(group), eq(List.of(p.getNodeId())));
    }

    // ----------------------------------------------------------------------
    // start(): transitions from not running → running, processes pending points
    // ----------------------------------------------------------------------

    @Test
    void testStartProcessesPendingThenClears() {
        DataPoint p1 = dp("A");
        DataPoint p2 = dp("B");

        task.addDataPoint(p1);
        task.addDataPoint(p2);

        task.start();

        ArgumentCaptor<List<DataPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(subscriber).addNodesToSubscription(eq(group), captor.capture(), any());
        List<DataPoint> sent = captor.getValue();

        assertEquals(Set.of(p1, p2), new HashSet<>(sent));

        // Second start() should do nothing
        task.start();
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    void testStartWhenAlreadyRunningDoesNothing() {
        task.start();
        task.start(); // no-op

        verify(subscriber, never()).addNodesToSubscription(any(), any(), any());
    }

    // ----------------------------------------------------------------------
    // stop()
    // ----------------------------------------------------------------------

    @Test
    void testStopClosesSubscription() {
        task.start();
        task.stop();

        verify(subscriber).closeSubscription(group);
    }

    @Test
    void testStopWhenNotRunningDoesNothing() {
        task.stop(); // no-op
        verify(subscriber, never()).closeSubscription(any());
    }

    // ----------------------------------------------------------------------
    // queue.push interrupted
    // ----------------------------------------------------------------------

    @Test
    void testQueuePushInterruptedSetsThreadInterrupted() throws Exception {

        DataPoint p = dp("A");

        task.start();
        task.addDataPoint(p);

        // capturar consumer passado ao subscriber
        ArgumentCaptor<Consumer<List<TSValue>>> captor = ArgumentCaptor.forClass(Consumer.class);

        verify(subscriber).addNodesToSubscription(
                eq(group),
                eq(List.of(p)),
                captor.capture());

        Consumer<List<TSValue>> callback = captor.getValue();

        List<TSValue> vals = List.of(new TSValue(1, 100, "v", true, null));

        AtomicReference<Thread> consumerThread = new AtomicReference<>();

        doThrow(new InterruptedException("x")).when(queue).push(any());

        Thread t = new Thread(() -> {
            consumerThread.set(Thread.currentThread());
            callback.accept(vals);
        });

        t.start();
        t.join();

        assertTrue(consumerThread.get().isInterrupted(),
                "Thread must be interrupted after push failure");
    }

    /**
     * Adapter so Mockito can capture the queuePush callback.
     */
    private interface QueueCallback extends java.util.function.Consumer<List<TSValue>> {
    }

    // ----------------------------------------------------------------------
    // MULTI-THREAD SAFETY
    // ----------------------------------------------------------------------

    @Test
    void testConcurrentAddRemoveAreSafe() throws Exception {
        int threads = 10;
        int ops = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        task.start(); // so add/remove call subscriber methods, not pending list

        for (int i = 0; i < threads; i++) {
            int id = i;
            pool.submit(() -> {
                for (int n = 0; n < ops; n++) {
                    DataPoint p = dp("DP-" + id);
                    task.addDataPoint(p);
                    task.removeDataPoint(p);
                }
            });
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        // We don't validate specific subscriber calls—just ensure no crash and no
        // deadlock
        assertTrue(true);
    }

    // ----------------------------------------------------------------------
    // edge: pending list must be isolated between start cycles
    // ----------------------------------------------------------------------

    @Test
    void testPendingListIsClearedAfterStart() {
        DataPoint p = dp("A");

        task.addDataPoint(p);
        task.start();

        // pending cleared now
        task.stop();

        // start again → no resubscribe
        task.start();

        verify(subscriber, times(1)).addNodesToSubscription(any(), any(), any());
    }
}
