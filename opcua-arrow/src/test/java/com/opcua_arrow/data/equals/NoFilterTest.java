package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoFilterTest {

    @Test
    void testAlwaysReturnsFalse() {
        NoFilter filter = new NoFilter();

        assertFalse(filter.isEqual("value", true));
        assertFalse(filter.isEqual("value", false));
        assertFalse(filter.isEqual(null, true));
        assertFalse(filter.isEqual(null, false));
    }

    @Test
    void testWithDifferentValueTypes() {
        NoFilter filter = new NoFilter();

        assertFalse(filter.isEqual(42, true));
        assertFalse(filter.isEqual(3.14, false));
        assertFalse(filter.isEqual(true, true));
        assertFalse(filter.isEqual(false, false));
        assertFalse(filter.isEqual(new Object(), true));
    }

    @Test
    void testWithArrays() {
        NoFilter filter = new NoFilter();

        assertFalse(filter.isEqual(new int[] { 1, 2, 3 }, true));
        assertFalse(filter.isEqual(new String[] { "a", "b", "c" }, false));
        assertFalse(filter.isEqual(new Object[0], true));
    }

    @Test
    void testMultipleCalls() {
        NoFilter filter = new NoFilter();

        // Verifica que sempre retorna false independente de chamadas anteriores
        for (int i = 0; i < 10; i++) {
            assertFalse(filter.isEqual(i, i % 2 == 0));
        }
    }

    @Test
    void testWithSameValueMultipleTimes() {
        NoFilter filter = new NoFilter();
        Object sameValue = "same";

        // Mesmo passando o mesmo valor várias vezes, sempre retorna false
        assertFalse(filter.isEqual(sameValue, true));
        assertFalse(filter.isEqual(sameValue, true));
        assertFalse(filter.isEqual(sameValue, false));
        assertFalse(filter.isEqual(sameValue, false));
    }

    @Test
    void testImplementsInterface() {
        NoFilter filter = new NoFilter();

        // Verifica que implementa IDataPointEqual
        assertTrue(filter instanceof com.opcua_arrow.data.IDataPointEqual);
    }

    @Test
    void testWithExtremeBooleanCombinations() {
        NoFilter filter = new NoFilter();

        // Testa todas as combinações de boolean
        assertFalse(filter.isEqual(Boolean.TRUE, true));
        assertFalse(filter.isEqual(Boolean.TRUE, false));
        assertFalse(filter.isEqual(Boolean.FALSE, true));
        assertFalse(filter.isEqual(Boolean.FALSE, false));
    }

    @Test
    void testWithLargeObjects() {
        NoFilter filter = new NoFilter();

        // Testa com objetos grandes
        byte[] largeArray = new byte[1000000];
        assertFalse(filter.isEqual(largeArray, true));

        String largeString = "x".repeat(100000);
        assertFalse(filter.isEqual(largeString, false));
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        NoFilter filter = new NoFilter();

        // Testa uso concorrente
        Thread[] threads = new Thread[10];
        boolean[] results = new boolean[10];

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = filter.isEqual("value" + index, index % 2 == 0);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verifica que todos retornaram false
        for (boolean result : results) {
            assertFalse(result);
        }
    }
}
