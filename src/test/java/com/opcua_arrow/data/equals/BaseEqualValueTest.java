package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseEqualValueTest {

    private TestableBaseEqualValue filter;

    // Implementação concreta para testar BaseEqualValue
    private static class TestableBaseEqualValue extends BaseEqualValue {
        private boolean sameValueReturn = true;
        private int isSameValueCallCount = 0;

        TestableBaseEqualValue(long intervalSeconds) {
            super(intervalSeconds);
        }

        @Override
        protected boolean isSameValue(Object newRawValue) {
            isSameValueCallCount++;
            return sameValueReturn;
        }

        void setSameValueReturn(boolean value) {
            this.sameValueReturn = value;
        }

        int getIsSameValueCallCount() {
            return isSameValueCallCount;
        }
    }

    @BeforeEach
    void setUp() {
        filter = new TestableBaseEqualValue(1L);
    }

    @Test
    void testConstructorInitializesInterval() throws Exception {
        TestableBaseEqualValue filter1sec = new TestableBaseEqualValue(1L);
        TestableBaseEqualValue filter10sec = new TestableBaseEqualValue(10L);

        Field intervalField = BaseEqualValue.class.getDeclaredField("intervalNanos");
        intervalField.setAccessible(true);

        assertEquals(1_000_000_000L, intervalField.get(filter1sec));
        assertEquals(10_000_000_000L, intervalField.get(filter10sec));
    }

    @Test
    void testInitialStateValues() throws Exception {
        Field lastValueField = BaseEqualValue.class.getDeclaredField("lastValue");
        Field lastIsGoodField = BaseEqualValue.class.getDeclaredField("lastIsGood");
        Field lastUpdateNanosField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");

        lastValueField.setAccessible(true);
        lastIsGoodField.setAccessible(true);
        lastUpdateNanosField.setAccessible(true);

        assertNull(lastValueField.get(filter));
        assertFalse((boolean) lastIsGoodField.get(filter));
        assertEquals(0L, lastUpdateNanosField.get(filter));
    }

    @Test
    void testFirstUpdateAlwaysReturnsFalse() {
        // Primeira atualização sempre retorna false (não igual)
        assertFalse(filter.isEqual("first", true));

        // Verifica que isSameValue não foi chamado (lastValue era null)
        assertEquals(0, filter.getIsSameValueCallCount());
    }

    @Test
    void testNullValueOnFirstUpdate() {
        // null na primeira atualização retorna true (igual - não atualiza)
        assertTrue(filter.isEqual(null, true));

        // Estado não foi atualizado
        assertTrue(filter.isEqual(null, false));
    }

    @Test
    void testWithinIntervalAlwaysReturnsFalse() {
        assertFalse(filter.isEqual("value", true));

        // Dentro do intervalo sempre retorna false (não igual)
        assertFalse(filter.isEqual("different", true));
        assertFalse(filter.isEqual("another", false));

        // isSameValue não é chamado dentro do intervalo
        assertEquals(0, filter.getIsSameValueCallCount());
    }

    @Test
    void testStatusChangeAfterInterval() throws Exception {
        assertFalse(filter.isEqual("value", true));

        // Força expiração do intervalo
        Field lastUpdateField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(filter, System.nanoTime() - 2_000_000_000L);

        // Status mudou - retorna false
        assertFalse(filter.isEqual("value", false));

        // isSameValue não é chamado quando status muda
        assertEquals(0, filter.getIsSameValueCallCount());
    }

    @Test
    void testSameValueAfterInterval() throws Exception {
        assertFalse(filter.isEqual("value", true));

        // Força expiração do intervalo
        Field lastUpdateField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(filter, System.nanoTime() - 2_000_000_000L);

        filter.setSameValueReturn(true);

        // Mesmo valor após intervalo retorna true (igual)
        assertTrue(filter.isEqual("value", true));

        // isSameValue foi chamado
        assertEquals(1, filter.getIsSameValueCallCount());
    }

    @Test
    void testDifferentValueAfterInterval() throws Exception {
        assertFalse(filter.isEqual("value", true));

        // Força expiração do intervalo
        Field lastUpdateField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(filter, System.nanoTime() - 2_000_000_000L);

        filter.setSameValueReturn(false);

        // Valor diferente após intervalo retorna false (não igual)
        assertFalse(filter.isEqual("different", true));

        // isSameValue foi chamado
        assertEquals(1, filter.getIsSameValueCallCount());
    }

    @Test
    void testUpdateStateMethod() throws Exception {
        assertFalse(filter.isEqual("value", true));

        Field lastValueField = BaseEqualValue.class.getDeclaredField("lastValue");
        Field lastIsGoodField = BaseEqualValue.class.getDeclaredField("lastIsGood");
        Field lastUpdateNanosField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");

        lastValueField.setAccessible(true);
        lastIsGoodField.setAccessible(true);
        lastUpdateNanosField.setAccessible(true);

        assertEquals("value", lastValueField.get(filter));
        assertTrue((boolean) lastIsGoodField.get(filter));
        assertTrue((long) lastUpdateNanosField.get(filter) > 0);
    }

    @Test
    void testZeroIntervalBehavior() throws Exception {
        TestableBaseEqualValue zeroIntervalFilter = new TestableBaseEqualValue(0L);

        assertFalse(zeroIntervalFilter.isEqual("value", true));

        // Com intervalo 0, sempre expira imediatamente
        Thread.sleep(1); // Pequeno delay para garantir que nanoTime mudou

        zeroIntervalFilter.setSameValueReturn(false);
        assertFalse(zeroIntervalFilter.isEqual("different", true));

        // isSameValue foi chamado porque intervalo expirou
        assertEquals(1, zeroIntervalFilter.getIsSameValueCallCount());
    }

    @Test
    void testNegativeIntervalBehavior() throws Exception {
        TestableBaseEqualValue negativeIntervalFilter = new TestableBaseEqualValue(-1L);

        assertFalse(negativeIntervalFilter.isEqual("value", true));

        Thread.sleep(1);

        negativeIntervalFilter.setSameValueReturn(false);
        assertFalse(negativeIntervalFilter.isEqual("different", true));

        // Com intervalo negativo, sempre considera expirado
        assertEquals(1, negativeIntervalFilter.getIsSameValueCallCount());
    }

    @Test
    void testLargeIntervalBehavior() {
        TestableBaseEqualValue largeIntervalFilter = new TestableBaseEqualValue(Long.MAX_VALUE / 1_000_000_000L);

        assertFalse(largeIntervalFilter.isEqual("value", true));

        // Dentro do intervalo (que é muito grande)
        assertFalse(largeIntervalFilter.isEqual("different", true));

        // isSameValue não é chamado
        assertEquals(0, largeIntervalFilter.getIsSameValueCallCount());
    }

    @Test
    void testSequentialUpdates() throws Exception {
        assertFalse(filter.isEqual("A", true));
        assertFalse(filter.isEqual("B", true));
        assertFalse(filter.isEqual("C", false));

        Field lastValueField = BaseEqualValue.class.getDeclaredField("lastValue");
        Field lastIsGoodField = BaseEqualValue.class.getDeclaredField("lastIsGood");

        lastValueField.setAccessible(true);
        lastIsGoodField.setAccessible(true);

        // Último estado atualizado
        assertEquals("C", lastValueField.get(filter));
        assertFalse((boolean) lastIsGoodField.get(filter));
    }

    @Test
    void testNullValueAfterValidValue() throws Exception {
        assertFalse(filter.isEqual("value", true));

        // Força expiração do intervalo
        Field lastUpdateField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(filter, System.nanoTime() - 2_000_000_000L);

        // null após valor válido - updateState retorna false, então isEqual retorna
        // true
        assertTrue(filter.isEqual(null, true));

        Field lastValueField = BaseEqualValue.class.getDeclaredField("lastValue");
        lastValueField.setAccessible(true);

        // Estado não foi atualizado (ainda tem "value")
        assertEquals("value", lastValueField.get(filter));
    }

    @Test
    void testBooleanFlagChanges() throws Exception {
        assertFalse(filter.isEqual("value", true));

        // Força expiração do intervalo
        Field lastUpdateField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");
        lastUpdateField.setAccessible(true);

        lastUpdateField.set(filter, System.nanoTime() - 2_000_000_000L);
        assertFalse(filter.isEqual("value", false)); // Status mudou

        lastUpdateField.set(filter, System.nanoTime() - 2_000_000_000L);
        assertFalse(filter.isEqual("value", true)); // Status mudou novamente

        lastUpdateField.set(filter, System.nanoTime() - 2_000_000_000L);
        filter.setSameValueReturn(true);
        assertTrue(filter.isEqual("value", true)); // Mesmo status e valor
    }

    @Test
    void testFinalMethodsCannotBeOverridden() {
        // Verifica que isEqual é final
        try {
            BaseEqualValue.class.getMethod("isEqual", Object.class, boolean.class);
            assertTrue(java.lang.reflect.Modifier.isFinal(
                    BaseEqualValue.class.getMethod("isEqual", Object.class, boolean.class).getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("Method isEqual should exist");
        }

        // Verifica que updateState é final
        try {
            BaseEqualValue.class.getDeclaredMethod("updateState", Object.class, boolean.class, long.class);
            assertTrue(java.lang.reflect.Modifier.isFinal(
                    BaseEqualValue.class.getDeclaredMethod("updateState", Object.class, boolean.class, long.class)
                            .getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("Method updateState should exist");
        }
    }

    @Test
    void testImplementsInterface() {
        assertTrue(filter instanceof com.opcua_arrow.data.IDataPointEqual);
    }
}
