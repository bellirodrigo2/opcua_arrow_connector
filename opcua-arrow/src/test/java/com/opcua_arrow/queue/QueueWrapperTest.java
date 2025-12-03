package com.opcua_arrow.queue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.opcua_arrow.queues.QueueWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for QueueWrapper including thread-safety,
 * edge cases, and concurrent scenarios.
 *
 * Uses only JUnit 5 assertions for simplicity and no external dependencies.
 */
class QueueWrapperTest {

    private static final int DEFAULT_CAPACITY = 10;
    private static final long DEFAULT_TIMEOUT_MS = 1000;
    private static final long SHORT_TIMEOUT_MS = 100;

    private QueueWrapper<Integer> queue;

    @BeforeEach
    void setUp() {
        queue = new QueueWrapper<>(DEFAULT_CAPACITY, DEFAULT_TIMEOUT_MS);
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("Should push and pop single element")
        void testPushAndPopSingleElement() throws InterruptedException {
            // Given
            Integer value = 42;
            List<Integer> result = new ArrayList<>();

            // When
            queue.push(value);
            queue.pop(result);

            // Then
            assertEquals(1, result.size(), "Result should contain exactly one element");
            assertEquals(value, result.get(0), "Element should match pushed value");
        }

        @Test
        @DisplayName("Should push and pop multiple elements maintaining FIFO order")
        void testFIFOOrder() throws InterruptedException {
            // Given
            List<Integer> values = List.of(1, 2, 3, 4, 5);
            List<Integer> result = new ArrayList<>();

            // When
            for (Integer val : values) {
                queue.push(val);
            }
            queue.pop(result);

            // Then
            assertEquals(values, result, "Elements should maintain FIFO order");
        }

        @Test
        @DisplayName("Should clear list before populating in pop()")
        void testPopClearsList() throws InterruptedException {
            // Given
            List<Integer> result = new ArrayList<>(List.of(99, 98, 97));
            queue.push(1);
            queue.push(2);

            // When
            queue.pop(result);

            // Then
            assertFalse(result.contains(99), "Old value 99 should be cleared");
            assertFalse(result.contains(98), "Old value 98 should be cleared");
            assertFalse(result.contains(97), "Old value 97 should be cleared");
            assertTrue(result.contains(1), "Should contain pushed value 1");
            assertTrue(result.contains(2), "Should contain pushed value 2");
            assertEquals(2, result.size(), "Should only contain the 2 pushed values");
        }

        @Test
        @DisplayName("Should drain all available elements in single pop")
        void testDrainAllElements() throws InterruptedException {
            // Given
            int count = 5;
            List<Integer> expected = IntStream.range(0, count)
                    .boxed()
                    .collect(Collectors.toList());
            List<Integer> result = new ArrayList<>();

            // When
            for (Integer val : expected) {
                queue.push(val);
            }
            queue.pop(result);

            // Then
            assertEquals(expected, result, "Should drain all elements at once");
        }
    }

    @Nested
    @DisplayName("Timeout Behavior")
    class TimeoutBehavior {

        @Test
        @DisplayName("Should timeout and return empty list when queue is empty")
        void testTimeoutOnEmptyQueue() throws InterruptedException {
            // Given
            QueueWrapper<Integer> shortTimeoutQueue = new QueueWrapper<>(DEFAULT_CAPACITY, SHORT_TIMEOUT_MS);
            List<Integer> result = new ArrayList<>();

            // When
            long startTime = System.currentTimeMillis();
            shortTimeoutQueue.pop(result);
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertTrue(result.isEmpty(), "Result should be empty after timeout");
            assertTrue(duration >= SHORT_TIMEOUT_MS,
                    "Should wait at least timeout duration: " + duration + "ms");
            assertTrue(duration < SHORT_TIMEOUT_MS + 150,
                    "Should not wait much longer than timeout: " + duration + "ms");
        }

        @Test
        @DisplayName("Should return immediately when elements are available")
        void testImmediateReturnWithElements() throws InterruptedException {
            // Given
            queue.push(1);
            List<Integer> result = new ArrayList<>();

            // When
            long startTime = System.currentTimeMillis();
            queue.pop(result);
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertEquals(1, result.size(), "Should pop one element");
            assertTrue(duration < 50,
                    "Should return almost immediately, took: " + duration + "ms");
        }

        @ParameterizedTest
        @ValueSource(longs = { 50, 100, 200, 500 })
        @DisplayName("Should respect different timeout values")
        void testDifferentTimeouts(long timeout) throws InterruptedException {
            // Given
            QueueWrapper<Integer> customQueue = new QueueWrapper<>(DEFAULT_CAPACITY, timeout);
            List<Integer> result = new ArrayList<>();

            // When
            long startTime = System.currentTimeMillis();
            customQueue.pop(result);
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertTrue(result.isEmpty(), "Result should be empty after timeout");
            assertTrue(duration >= timeout,
                    "Should wait at least " + timeout + "ms, waited: " + duration);
            assertTrue(duration < timeout + 150,
                    "Should not exceed timeout by much, waited: " + duration);
        }
    }

    @Nested
    @DisplayName("Capacity Constraints")
    class CapacityConstraints {

        @Test
        @DisplayName("Should block push when queue is at capacity")
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        void testBlockingWhenFull() throws InterruptedException {
            // Given
            QueueWrapper<Integer> smallQueue = new QueueWrapper<>(2, DEFAULT_TIMEOUT_MS);
            AtomicBoolean pushCompleted = new AtomicBoolean(false);

            // Fill the queue
            smallQueue.push(1);
            smallQueue.push(2);

            // When - try to push third element in separate thread
            Thread pusher = new Thread(() -> {
                try {
                    smallQueue.push(3);
                    pushCompleted.set(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            pusher.start();

            // Then - verify push is blocked
            Thread.sleep(200);
            assertFalse(pushCompleted.get(), "Push should be blocked when queue is full");

            // Pop one element to make space
            List<Integer> result = new ArrayList<>();
            smallQueue.pop(result);

            // Now push should complete
            pusher.join(500);
            assertTrue(pushCompleted.get(), "Push should complete after space is available");
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 5, 10, 100 })
        @DisplayName("Should respect different capacity values")
        void testDifferentCapacities(int capacity) throws InterruptedException {
            // Given
            QueueWrapper<Integer> customQueue = new QueueWrapper<>(capacity, SHORT_TIMEOUT_MS);
            CountDownLatch allPushed = new CountDownLatch(1);
            AtomicBoolean extraPushBlocked = new AtomicBoolean(true);

            // When - fill to capacity
            for (int i = 0; i < capacity; i++) {
                customQueue.push(i);
            }
            allPushed.countDown();

            // Try to push one more in separate thread
            Thread extraPusher = new Thread(() -> {
                try {
                    allPushed.await();

                    // This should block, so we interrupt after short wait
                    Thread blocker = Thread.currentThread();
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                    scheduler.schedule(() -> blocker.interrupt(), 100, TimeUnit.MILLISECONDS);

                    customQueue.push(999);
                    extraPushBlocked.set(false); // Should not reach here
                } catch (InterruptedException e) {
                    // Expected - thread was interrupted because push was blocked
                }
            });
            extraPusher.start();
            extraPusher.join(200);

            // Then
            assertTrue(extraPushBlocked.get(),
                    "Extra push should remain blocked at capacity " + capacity);

            // Verify we can drain exactly capacity elements
            List<Integer> result = new ArrayList<>();
            customQueue.pop(result);
            assertEquals(capacity, result.size(),
                    "Should pop exactly " + capacity + " elements");
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafety {

        @RepeatedTest(10) // Run multiple times to catch race conditions
        @DisplayName("Should handle concurrent push operations safely")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void testConcurrentPush() throws InterruptedException {
            // Given
            int threadCount = 10;
            int itemsPerThread = 100;
            QueueWrapper<Integer> largeQueue = new QueueWrapper<>(threadCount * itemsPerThread, DEFAULT_TIMEOUT_MS);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            // When - multiple threads push concurrently
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        startLatch.await(); // All threads start together
                        for (int i = 0; i < itemsPerThread; i++) {
                            largeQueue.push(threadId * 1000 + i);
                        }
                        doneLatch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            startLatch.countDown(); // Start all threads
            assertTrue(doneLatch.await(3, TimeUnit.SECONDS),
                    "All push threads should complete");

            // Then - verify all elements are present
            List<Integer> result = new ArrayList<>();
            largeQueue.pop(result);
            assertEquals(threadCount * itemsPerThread, result.size(),
                    "Should have all pushed elements");

            // Verify no elements were lost or duplicated
            Set<Integer> uniqueElements = result.stream().collect(Collectors.toSet());
            assertEquals(threadCount * itemsPerThread, uniqueElements.size(),
                    "All elements should be unique");
        }

        @RepeatedTest(10)
        @DisplayName("Should handle concurrent pop operations safely")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void testConcurrentPop() throws InterruptedException {
            // Given
            int elementCount = 1000;
            QueueWrapper<Integer> largeQueue = new QueueWrapper<>(elementCount, DEFAULT_TIMEOUT_MS);
            for (int i = 0; i < elementCount; i++) {
                largeQueue.push(i);
            }

            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ConcurrentLinkedQueue<Integer> allPopped = new ConcurrentLinkedQueue<>();

            // When - multiple threads pop concurrently
            for (int t = 0; t < threadCount; t++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        List<Integer> localResult = new ArrayList<>();
                        largeQueue.pop(localResult);
                        allPopped.addAll(localResult);
                        doneLatch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(3, TimeUnit.SECONDS),
                    "All pop threads should complete");

            // Then - verify thread safety (no lost or duplicated elements)
            assertEquals(elementCount, allPopped.size(),
                    "All elements should be popped exactly once");
            Set<Integer> unique = allPopped.stream().collect(Collectors.toSet());
            assertEquals(elementCount, unique.size(),
                    "No elements should be duplicated");
        }

        @RepeatedTest(20)
        @DisplayName("Should handle mixed concurrent push/pop operations")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void testConcurrentMixedOperations() throws InterruptedException {
            // Given
            int operationCount = 1000;
            QueueWrapper<String> stringQueue = new QueueWrapper<>(100, DEFAULT_TIMEOUT_MS);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicInteger pushCounter = new AtomicInteger(0);
            AtomicInteger popCounter = new AtomicInteger(0);

            // Producer thread
            Thread producer = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationCount; i++) {
                        stringQueue.push("item-" + i);
                        pushCounter.incrementAndGet();
                        if (i % 10 == 0) {
                            Thread.yield(); // Allow context switching
                        }
                    }
                    doneLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Consumer thread
            Thread consumer = new Thread(() -> {
                try {
                    startLatch.await();
                    List<String> batch = new ArrayList<>();
                    while (popCounter.get() < operationCount) {
                        stringQueue.pop(batch);
                        popCounter.addAndGet(batch.size());
                        batch.clear();
                    }
                    doneLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // When
            producer.start();
            consumer.start();
            startLatch.countDown();

            // Then
            assertTrue(doneLatch.await(8, TimeUnit.SECONDS),
                    "Producer and consumer should complete");
            assertEquals(operationCount, pushCounter.get(),
                    "Should push all items");
            assertEquals(operationCount, popCounter.get(),
                    "Should pop all items");
        }

        @Test
        @DisplayName("Should maintain thread safety with multiple producers and consumers")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void testMultipleProducersConsumers() throws InterruptedException {
            // Given
            int producerCount = 5;
            int consumerCount = 3;
            int itemsPerProducer = 200;
            int totalItems = producerCount * itemsPerProducer;

            QueueWrapper<Integer> queue = new QueueWrapper<>(50, 100);
            ConcurrentLinkedQueue<Integer> consumed = new ConcurrentLinkedQueue<>();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch producersDone = new CountDownLatch(producerCount);
            AtomicBoolean consumersStop = new AtomicBoolean(false);
            ExecutorService executor = Executors.newCachedThreadPool();

            // Start producers
            for (int p = 0; p < producerCount; p++) {
                final int producerId = p;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < itemsPerProducer; i++) {
                            queue.push(producerId * 1000 + i);
                        }
                        producersDone.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            // Start consumers
            CountDownLatch consumersDone = new CountDownLatch(consumerCount);
            for (int c = 0; c < consumerCount; c++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        List<Integer> batch = new ArrayList<>();
                        while (!consumersStop.get() || consumed.size() < totalItems) {
                            queue.pop(batch);
                            consumed.addAll(batch);
                            batch.clear();

                            if (consumed.size() >= totalItems) {
                                break;
                            }
                        }
                        consumersDone.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            // When
            startLatch.countDown();
            assertTrue(producersDone.await(5, TimeUnit.SECONDS),
                    "All producers should finish");

            // Wait for all items to be consumed
            int waitCount = 0;
            while (consumed.size() < totalItems && waitCount++ < 50) {
                Thread.sleep(100);
            }
            consumersStop.set(true);

            // Then
            assertTrue(consumersDone.await(2, TimeUnit.SECONDS),
                    "All consumers should finish");
            assertEquals(totalItems, consumed.size(),
                    "All items should be consumed");

            // Verify all items are unique
            Set<Integer> unique = consumed.stream().collect(Collectors.toSet());
            assertEquals(totalItems, unique.size(),
                    "No items should be lost or duplicated");

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should reject null values due to LinkedBlockingQueue constraint")
        void testNullValues() {
            // Given
            QueueWrapper<String> stringQueue = new QueueWrapper<>(10, DEFAULT_TIMEOUT_MS);

            // When/Then - LinkedBlockingQueue does not accept null values
            assertThrows(NullPointerException.class, () -> {
                stringQueue.push(null);
            }, "LinkedBlockingQueue should throw NullPointerException for null values");

            // Verify queue still works after null attempt
            assertDoesNotThrow(() -> {
                stringQueue.push("valid-value");
                List<String> result = new ArrayList<>();
                stringQueue.pop(result);
                assertEquals(1, result.size(), "Should pop valid value");
                assertEquals("valid-value", result.get(0), "Should contain the valid value");
            });
        }

        @Test
        @DisplayName("Should validate constructor parameters")
        void testConstructorValidation() {
            // Test invalid capacity
            assertThrows(IllegalArgumentException.class, () -> {
                new QueueWrapper<Integer>(0, DEFAULT_TIMEOUT_MS);
            }, "Should reject zero capacity");

            assertThrows(IllegalArgumentException.class, () -> {
                new QueueWrapper<Integer>(-1, DEFAULT_TIMEOUT_MS);
            }, "Should reject negative capacity");

            // Test invalid timeout
            assertThrows(IllegalArgumentException.class, () -> {
                new QueueWrapper<Integer>(DEFAULT_CAPACITY, -1);
            }, "Should reject negative timeout");

            // Test valid edge cases
            assertDoesNotThrow(() -> {
                new QueueWrapper<Integer>(1, 0); // Minimum valid capacity, zero timeout
            }, "Should accept minimum valid parameters");

            assertDoesNotThrow(() -> {
                new QueueWrapper<Integer>(Integer.MAX_VALUE, Long.MAX_VALUE);
            }, "Should accept maximum valid parameters");
        }

        @Test
        @DisplayName("Should reject null batch list in pop")
        void testNullBatchList() throws InterruptedException {
            // Given
            queue.push(1);

            // When/Then
            assertThrows(NullPointerException.class, () -> {
                queue.pop(null);
            }, "Should throw NullPointerException for null batch list");
        }

        @Test
        @DisplayName("Should handle interrupted threads gracefully during push")
        void testInterruptedPush() throws InterruptedException {
            // Given
            QueueWrapper<Integer> smallQueue = new QueueWrapper<>(1, DEFAULT_TIMEOUT_MS);
            smallQueue.push(1); // Fill queue

            AtomicBoolean wasInterrupted = new AtomicBoolean(false);
            Thread pusher = new Thread(() -> {
                try {
                    smallQueue.push(2); // This will block
                } catch (InterruptedException e) {
                    wasInterrupted.set(true);
                }
            });

            // When
            pusher.start();
            Thread.sleep(100); // Let it block
            pusher.interrupt();
            pusher.join(500);

            // Then
            assertTrue(wasInterrupted.get(),
                    "Thread should be interrupted while blocked on push");
        }

        @Test
        @DisplayName("Should handle interrupted threads during pop")
        void testInterruptedPop() throws InterruptedException {
            // Given
            AtomicBoolean wasInterrupted = new AtomicBoolean(false);
            Thread popper = new Thread(() -> {
                try {
                    List<Integer> result = new ArrayList<>();
                    queue.pop(result); // Will wait for timeout
                } catch (InterruptedException e) {
                    wasInterrupted.set(true);
                }
            });

            // When
            popper.start();
            Thread.sleep(100);
            popper.interrupt();
            popper.join(500);

            // Then
            assertTrue(wasInterrupted.get(),
                    "Thread should be interrupted while waiting on pop");
        }

        @Test
        @DisplayName("Should handle empty list parameter in pop")
        void testPopWithEmptyList() throws InterruptedException {
            // Given
            queue.push(1);
            queue.push(2);
            List<Integer> result = new ArrayList<>();

            // When
            queue.pop(result);

            // Then
            assertEquals(2, result.size(), "Should contain both pushed elements");
            assertTrue(result.contains(1), "Should contain element 1");
            assertTrue(result.contains(2), "Should contain element 2");
        }

        @Test
        @DisplayName("Should handle pre-populated list in pop")
        void testPopWithPrePopulatedList() throws InterruptedException {
            // Given
            queue.push(1);
            List<Integer> result = new ArrayList<>(List.of(99, 98, 97));
            int originalSize = result.size();

            // When
            queue.pop(result);

            // Then
            assertEquals(1, result.size(),
                    "List should be cleared and contain only new element");
            assertTrue(result.contains(1), "Should contain pushed element");
            assertFalse(result.contains(99) || result.contains(98) || result.contains(97),
                    "Should not contain old elements");
        }

        @Test
        @DisplayName("Should handle synchronized list wrapper correctly")
        void testPopWithSynchronizedList() throws InterruptedException {
            // Given
            queue.push(1);
            queue.push(2);
            List<Integer> originalList = new ArrayList<>();
            List<Integer> synchronizedList = Collections.synchronizedList(originalList);

            // When
            queue.pop(synchronizedList);

            // Then
            assertEquals(2, originalList.size(),
                    "Original list should be updated through synchronized wrapper");
            assertTrue(originalList.contains(1) && originalList.contains(2),
                    "Original list should contain pushed elements");
        }

        @Test
        @DisplayName("Should handle zero timeout correctly")
        void testZeroTimeout() throws InterruptedException {
            // Given
            QueueWrapper<Integer> zeroTimeoutQueue = new QueueWrapper<>(10, 0);
            List<Integer> result = new ArrayList<>();

            // When - pop from empty queue with zero timeout
            long start = System.currentTimeMillis();
            zeroTimeoutQueue.pop(result);
            long duration = System.currentTimeMillis() - start;

            // Then
            assertTrue(result.isEmpty(), "Should return empty immediately");
            assertTrue(duration < 50,
                    "Should return almost immediately with zero timeout: " + duration + "ms");
        }

        @Test
        @DisplayName("Should handle maximum capacity queue")
        void testMaximumCapacity() throws InterruptedException {
            // Given - queue with large capacity
            int largeCapacity = 10000;
            QueueWrapper<Integer> largeQueue = new QueueWrapper<>(largeCapacity, 100);

            // When - fill to capacity
            for (int i = 0; i < largeCapacity; i++) {
                largeQueue.push(i);
            }

            List<Integer> result = new ArrayList<>();
            largeQueue.pop(result);

            // Then
            assertEquals(largeCapacity, result.size(),
                    "Should handle large capacity: " + largeCapacity);
        }

        @Test
        @DisplayName("Should handle rapid push/pop cycles")
        void testRapidPushPopCycles() throws InterruptedException {
            // Given
            int cycles = 1000;
            List<Integer> result = new ArrayList<>();

            // When - rapid push/pop cycles
            for (int i = 0; i < cycles; i++) {
                queue.push(i);
                queue.pop(result);
                assertEquals(1, result.size(),
                        "Each cycle should pop exactly one element");
                assertEquals(Integer.valueOf(i), result.get(0),
                        "Element should match cycle number");
                result.clear();
            }

            // Then - queue should be empty
            queue.pop(result);
            assertTrue(result.isEmpty(),
                    "Queue should be empty after all cycles");
        }
    }

    @Nested
    @DisplayName("Performance Characteristics")
    class PerformanceTests {

        @Test
        @DisplayName("Should maintain high throughput performance")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void testHighThroughput() throws InterruptedException {
            // Given
            int itemCount = 100000;
            QueueWrapper<Integer> performanceQueue = new QueueWrapper<>(1000, 100);
            CountDownLatch halfway = new CountDownLatch(1);
            AtomicInteger consumed = new AtomicInteger(0);

            // Producer thread
            Thread producer = new Thread(() -> {
                try {
                    for (int i = 0; i < itemCount; i++) {
                        performanceQueue.push(i);
                        if (i == itemCount / 2) {
                            halfway.countDown();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Consumer thread
            Thread consumer = new Thread(() -> {
                try {
                    List<Integer> batch = new ArrayList<>();
                    while (consumed.get() < itemCount) {
                        performanceQueue.pop(batch);
                        consumed.addAndGet(batch.size());
                        batch.clear();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // When
            long start = System.currentTimeMillis();
            producer.start();
            consumer.start();

            assertTrue(halfway.await(5, TimeUnit.SECONDS),
                    "Should process half items within 5 seconds");

            producer.join(10000);
            consumer.join(10000);
            long duration = System.currentTimeMillis() - start;

            // Then
            assertEquals(itemCount, consumed.get(),
                    "All items should be consumed");

            double throughput = (itemCount / (duration / 1000.0));
            System.out.println("Throughput: " + String.format("%.2f", throughput) + " items/second");
            assertTrue(throughput > 1000,
                    "Should maintain at least 1000 items/second throughput, got: " + throughput);
        }

        @Test
        @DisplayName("Should efficiently drain large batches")
        void testLargeBatchDrain() throws InterruptedException {
            // Given
            int batchSize = 1000;
            QueueWrapper<Integer> batchQueue = new QueueWrapper<>(batchSize * 2, 100);

            // Fill with batch
            for (int i = 0; i < batchSize; i++) {
                batchQueue.push(i);
            }

            // When
            List<Integer> result = new ArrayList<>();
            long start = System.nanoTime();
            batchQueue.pop(result);
            long duration = System.nanoTime() - start;

            // Then
            assertEquals(batchSize, result.size(),
                    "Should drain full batch of " + batchSize);

            double milliseconds = duration / 1_000_000.0;
            System.out.println(
                    "Batch drain time: " + String.format("%.3f", milliseconds) + "ms for " + batchSize + " items");
            assertTrue(duration < 10_000_000L,
                    "Draining " + batchSize + " items should take less than 10ms, took: " + milliseconds + "ms");
        }
    }
}
