package com.opcua_arrow.read;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.opcua_arrow.ICallBack;
import com.opcua_arrow.ICallBack.ICallBackObject;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;
import com.opcua_arrow.queues.IQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReadTaskTest {

    private IReader reader;
    private IQueue<List<TSValue>> queue;
    private ICallBack callback;

    private ReadTask newTask() {
        return new ReadTask(reader, 50L, queue, callback);
    }

    private DataPoint dp(int id) {
        return new DataPoint("n" + id, "d" + id, "node" + id, id, null, null, null, null);
    }

    private TSValue ts(int id) {
        return new TSValue(id, System.currentTimeMillis(), "v" + id, true, null);
    }

    // ------------------------------------------------------------
    // Base setup
    // ------------------------------------------------------------
    @BeforeEach
    void setup() {
        reader = mock(IReader.class);
        queue = mock(IQueue.class);
        callback = mock(ICallBack.class);
    }

    @AfterEach
    void tearDown() {
        // nothing to clean now
    }

    // ------------------------------------------------------------
    // add/remove
    // ------------------------------------------------------------

    @Test
    void testAddDataPointNullThrows() {
        ReadTask task = newTask();
        assertThrows(IllegalArgumentException.class, () -> task.addDataPoint(null));
    }

    @Test
    void testRemoveNullIsNoop() {
        ReadTask task = newTask();
        assertDoesNotThrow(() -> task.removeDataPoint(null));
    }

    @Test
    void testAddThenRemoveSinglePoint() {
        ReadTask task = newTask();

        DataPoint p = dp(1);

        task.addDataPoint(p);
        task.removeDataPoint(p);

        // If no datapoints exist, reader MUST NOT be called.
        when(reader.read(any())).thenReturn(List.of());

        ICallBackObject cbObj = mock(ICallBackObject.class);
        when(callback.startCallback(any(), any())).thenReturn(cbObj);

        task.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        verify(reader, never()).read(any());
    }

    // ------------------------------------------------------------
    // Concurrency test
    // ------------------------------------------------------------

    @Test
    void testConcurrentAddRemoveNoCrash() throws Exception {
        ReadTask task = newTask();

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    DataPoint x = dp(j);
                    task.addDataPoint(x);
                    task.removeDataPoint(x);
                }
            });
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    // ------------------------------------------------------------
    // start(): empty → no read
    // ------------------------------------------------------------

    @Test
    void testStartWithEmptyDoesNothing() throws Exception {
        ReadTask task = newTask();

        when(reader.read(any())).thenReturn(List.of());

        ICallBackObject cb = mock(ICallBackObject.class);
        when(callback.startCallback(any(), any())).thenReturn(cb);

        task.start();

        Thread.sleep(120);

        verify(reader, never()).read(any());
        verify(callback, never()).startCallback(any(), any());
    }

    // ------------------------------------------------------------
    // Happy path: read OK → queue.push OK
    // ------------------------------------------------------------

    @Test
    void testStartReadsAndPushesToQueue() throws Exception {
        ReadTask task = newTask();

        DataPoint p = dp(10);
        task.addDataPoint(p);

        List<TSValue> values = List.of(ts(10), ts(11));

        when(reader.read(any())).thenReturn(values);

        ICallBackObject cb = mock(ICallBackObject.class);
        when(callback.startCallback(any(), any())).thenReturn(cb);

        task.start();

        Thread.sleep(200);

        ArgumentCaptor<List<TSValue>> cap = ArgumentCaptor.forClass(List.class);
        verify(queue, atLeastOnce()).push(cap.capture());

        assertEquals(2, cap.getValue().size());
        verify(cb, atLeastOnce()).close();
    }

    // ------------------------------------------------------------
    // Reader throws exception → callback.close()
    // ------------------------------------------------------------

    @Test
    void testReaderExceptionClosesCallback() throws Exception {
        ReadTask task = newTask();

        DataPoint p = dp(3);
        task.addDataPoint(p);

        when(reader.read(any())).thenThrow(new RuntimeException("x"));

        ICallBackObject cb = mock(ICallBackObject.class);
        when(callback.startCallback(any(), any())).thenReturn(cb);

        task.start();
        Thread.sleep(150);

        verify(cb, atLeastOnce()).close();
        verify(queue, never()).push(any());
    }

    // ------------------------------------------------------------
    // queue.push throws InterruptedException
    // ------------------------------------------------------------

    @Test
    void testQueuePushInterrupted() throws Exception {
        ReadTask task = newTask();
        DataPoint p = dp(7);
        task.addDataPoint(p);

        when(reader.read(any())).thenReturn(List.of(ts(7)));

        AtomicBoolean interruptDetected = new AtomicBoolean(false);

        doAnswer(inv -> {
            interruptDetected.set(true);
            throw new InterruptedException("x");
        }).when(queue).push(any());

        ICallBackObject cb = mock(ICallBackObject.class);
        when(callback.startCallback(any(), any())).thenReturn(cb);

        task.start();
        Thread.sleep(150);

        verify(cb, atLeastOnce()).close();
        assertTrue(interruptDetected.get(),
                "Queue push interrupt handler must run");
    }

    // ------------------------------------------------------------
    // stop()
    // ------------------------------------------------------------

    @Test
    void testStopWithoutStartDoesNotThrow() {
        ReadTask task = newTask();
        assertDoesNotThrow(task::stop);
    }

    @Test
    void testStartThenStopWorks() throws Exception {
        ReadTask task = newTask();
        task.addDataPoint(dp(1));

        when(reader.read(any())).thenReturn(List.of());

        ICallBackObject cb = mock(ICallBackObject.class);
        when(callback.startCallback(any(), any())).thenReturn(cb);

        task.start();
        Thread.sleep(80);

        assertDoesNotThrow(task::stop);
    }
}
