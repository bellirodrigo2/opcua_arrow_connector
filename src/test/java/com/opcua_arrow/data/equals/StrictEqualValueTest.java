package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StrictEqualValueTest {

    private StrictEqualValue filter;

    @BeforeEach
    void setUp() {
        filter = new StrictEqualValue(1L); // 1 segundo de intervalo
    }

    @Test
    void testFirstUpdateAlwaysReturnsFalse() {
        // Primeira atualização sempre retorna false (diferente)
        assertFalse(filter.isEqual("first", true));
    }

    @Test
    void testSameValueWithinIntervalReturnsTrue() {
        assertFalse(filter.isEqual("value", true));

        // Mesmo valor dentro do intervalo retorna true (igual)
        assertTrue(filter.isEqual("value", true));
    }

    @Test
    void testDifferentValueWithinIntervalReturnsFalse() {
        assertFalse(filter.isEqual("value1", true));

        // Valor diferente dentro do intervalo retorna false (diferente)
        assertFalse(filter.isEqual("value2", true));
    }

    @Test
    void testStatusChangeReturnsFalse() {
        assertFalse(filter.isEqual("value", true));

        // Mudança de status (isGood) retorna false
        assertFalse(filter.isEqual("value", false));
    }

    @Test
    void testNullValueOnFirstUpdate() {
        // null na primeira atualização retorna true (não atualiza estado)
        assertTrue(filter.isEqual(null, true));

        // Após null, qualquer valor válido é considerado primeira atualização
        assertFalse(filter.isEqual("value", true));
    }

    @Test
    void testAfterIntervalExpiredReturnsFalse() throws Exception {
        StrictEqualValue shortIntervalFilter = new StrictEqualValue(0L); // 0 segundos

        assertFalse(shortIntervalFilter.isEqual("value", true));

        // Força expiração do intervalo mudando lastUpdateNanos via reflection
        Field lastUpdateField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(shortIntervalFilter, System.nanoTime() - 1_000_000_000L);

        // Após intervalo expirado, mesmo valor retorna false
        assertFalse(shortIntervalFilter.isEqual("value", true));
    }

    @Test
    void testEqualsWithNumbers() {
        assertFalse(filter.isEqual(42, true));
        assertTrue(filter.isEqual(42, true));
        assertFalse(filter.isEqual(43, true));
    }

    @Test
    void testEqualsWithBoxedPrimitives() {
        assertFalse(filter.isEqual(Integer.valueOf(100), true));
        assertTrue(filter.isEqual(Integer.valueOf(100), true));

        // Nova instância mas mesmo valor
        assertTrue(filter.isEqual(Integer.valueOf(100), true));
    }

    @Test
    void testEqualsWithArrays() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };

        assertFalse(filter.isEqual(array1, true));

        // Mesmo array retorna true
        assertTrue(filter.isEqual(array1, true));

        // Array diferente (mesmos valores mas objeto diferente) retorna false
        assertFalse(filter.isEqual(array2, true));
    }

    @Test
    void testConstructorWithDifferentIntervals() {
        StrictEqualValue filter0 = new StrictEqualValue(0L);
        StrictEqualValue filter10 = new StrictEqualValue(10L);
        StrictEqualValue filterMax = new StrictEqualValue(Long.MAX_VALUE / 1_000_000_000L);

        assertNotNull(filter0);
        assertNotNull(filter10);
        assertNotNull(filterMax);
    }

    @Test
    void testNegativeInterval() {
        StrictEqualValue negativeFilter = new StrictEqualValue(-1L);

        // Deve funcionar normalmente (intervalo negativo será sempre expirado)
        assertFalse(negativeFilter.isEqual("value", true));
        assertFalse(negativeFilter.isEqual("value", true));
    }

    @Test
    void testBooleanValueEquality() {
        assertFalse(filter.isEqual(Boolean.TRUE, true));
        assertTrue(filter.isEqual(Boolean.TRUE, true));
        assertFalse(filter.isEqual(Boolean.FALSE, true));
        assertTrue(filter.isEqual(Boolean.FALSE, false));
    }

    @Test
    void testStringEquality() {
        String str1 = new String("test");
        String str2 = new String("test");

        assertFalse(filter.isEqual(str1, true));
        assertTrue(filter.isEqual(str1, true));

        // Strings com mesmo conteúdo são equals
        assertTrue(filter.isEqual(str2, true));
    }

    @Test
    void testCustomObjectEquality() {
        class CustomObject {
            private final int value;

            CustomObject(int value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof CustomObject) {
                    return ((CustomObject) obj).value == this.value;
                }
                return false;
            }
        }

        CustomObject obj1 = new CustomObject(10);
        CustomObject obj2 = new CustomObject(10);
        CustomObject obj3 = new CustomObject(20);

        assertFalse(filter.isEqual(obj1, true));
        assertTrue(filter.isEqual(obj1, true));

        // obj2 tem mesmo valor, então equals retorna true
        assertTrue(filter.isEqual(obj2, true));

        // obj3 tem valor diferente
        assertFalse(filter.isEqual(obj3, true));
    }

    @Test
    void testSequentialUpdatesWithDifferentValues() {
        assertFalse(filter.isEqual("A", true));
        assertFalse(filter.isEqual("B", true));
        assertFalse(filter.isEqual("C", false));
        assertFalse(filter.isEqual("D", false));
        assertTrue(filter.isEqual("D", false));
    }

    @Test
    void testNullAfterValue() {
        assertFalse(filter.isEqual("value", true));

        // null após valor válido retorna true (não atualiza)
        assertTrue(filter.isEqual(null, true));

        // Estado anterior permanece
        assertTrue(filter.isEqual("value", true));
    }

    @Test
    void testImplementsInterface() {
        assertTrue(filter instanceof com.opcua_arrow.data.IDataPointEqual);
    }
}
