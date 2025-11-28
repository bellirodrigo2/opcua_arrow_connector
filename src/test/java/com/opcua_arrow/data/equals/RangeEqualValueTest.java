package com.opcua_arrow.data.equals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RangeEqualValueTest {

    private RangeEqualValue filter;

    @BeforeEach
    void setUp() {
        filter = new RangeEqualValue(0.1, 1L); // 10% de range, 1 segundo de intervalo
    }

    @Test
    void testFirstUpdateAlwaysReturnsFalse() {
        assertFalse(filter.isEqual(100.0, true));
    }

    @Test
    void testValueWithinRangeReturnsTrue() {
        assertFalse(filter.isEqual(100.0, true));

        // 105 está dentro do range de 10% de 100
        assertTrue(filter.isEqual(105.0, true));

        // 95 está dentro do range de 10% de 100
        assertTrue(filter.isEqual(95.0, true));

        // 110 está exatamente no limite (10% de 100 = 10)
        assertTrue(filter.isEqual(110.0, true));

        // 90 está exatamente no limite
        assertTrue(filter.isEqual(90.0, true));
    }

    @Test
    void testValueOutsideRangeReturnsFalse() {
        assertFalse(filter.isEqual(100.0, true));

        // 111 está fora do range de 10% de 100
        assertFalse(filter.isEqual(111.0, true));

        // Agora o valor base é 111
        assertTrue(filter.isEqual(111.0, true));

        // 89 estaria fora do range original de 100
        assertFalse(filter.isEqual(89.0, true));
    }

    @Test
    void testStatusChangeReturnsFalse() {
        assertFalse(filter.isEqual(100.0, true));

        // Mudança de status sempre retorna false
        assertFalse(filter.isEqual(100.0, false));
    }

    @Test
    void testNullValueHandling() {
        // null na primeira atualização retorna true (não atualiza)
        assertTrue(filter.isEqual(null, true));

        // Após null, qualquer valor é primeira atualização
        assertFalse(filter.isEqual(100.0, true));

        // null após valor válido retorna false (isSameValue retorna false para null)
        assertFalse(filter.isEqual(null, true));
    }

    @Test
    void testZeroRange() {
        RangeEqualValue zeroRangeFilter = new RangeEqualValue(0.0, 1L);

        assertFalse(zeroRangeFilter.isEqual(100.0, true));

        // Com range 0, apenas valores exatamente iguais retornam true
        assertTrue(zeroRangeFilter.isEqual(100.0, true));
        assertFalse(zeroRangeFilter.isEqual(100.00001, true));
    }

    @Test
    void testLargeRange() {
        RangeEqualValue largeRangeFilter = new RangeEqualValue(1.0, 1L); // 100% range

        assertFalse(largeRangeFilter.isEqual(100.0, true));

        // 200 está dentro do range de 100% de 100
        assertTrue(largeRangeFilter.isEqual(200.0, true));

        // 0 está dentro do range de 100% de 100
        assertTrue(largeRangeFilter.isEqual(0.0, true));

        // -100 está exatamente no limite
        assertTrue(largeRangeFilter.isEqual(-100.0, true));
    }

    @Test
    void testNegativeValues() {
        assertFalse(filter.isEqual(-100.0, true));

        // -105 está dentro do range de 10% de -100
        assertTrue(filter.isEqual(-105.0, true));

        // -95 está dentro do range de 10% de -100
        assertTrue(filter.isEqual(-95.0, true));

        // -111 está fora do range
        assertFalse(filter.isEqual(-111.0, true));
    }

    @Test
    void testZeroValueBase() {
        assertFalse(filter.isEqual(0.0, true));

        // Com base 0, o range é 0 * 0.1 = 0
        assertTrue(filter.isEqual(0.0, true));
        assertFalse(filter.isEqual(0.00001, true));
        assertFalse(filter.isEqual(-0.00001, true));
    }

    @Test
    void testWithIntegerValues() {
        assertFalse(filter.isEqual(100, true));

        // Integers são convertidos para double
        assertTrue(filter.isEqual(105, true));
        assertTrue(filter.isEqual(95, true));
        assertFalse(filter.isEqual(111, true));
    }

    @Test
    void testWithLongValues() {
        assertFalse(filter.isEqual(1000000L, true));

        // 10% de 1000000 = 100000
        assertTrue(filter.isEqual(1100000L, true));
        assertTrue(filter.isEqual(900000L, true));
        assertFalse(filter.isEqual(1100001L, true));
    }

    @Test
    void testWithFloatValues() {
        assertFalse(filter.isEqual(10.0f, true));

        // 10% de 10 = 1
        assertTrue(filter.isEqual(11.0f, true));
        assertTrue(filter.isEqual(9.0f, true));
        assertFalse(filter.isEqual(11.1f, true));
    }

    @Test
    void testWithByteValues() {
        assertFalse(filter.isEqual((byte) 100, true));

        assertTrue(filter.isEqual((byte) 110, true));
        assertTrue(filter.isEqual((byte) 90, true));
        assertFalse(filter.isEqual((byte) 111, true));
    }

    @Test
    void testWithShortValues() {
        assertFalse(filter.isEqual((short) 1000, true));

        // 10% de 1000 = 100
        assertTrue(filter.isEqual((short) 1100, true));
        assertTrue(filter.isEqual((short) 900, true));
        assertFalse(filter.isEqual((short) 1101, true));
    }

    @Test
    void testAfterIntervalExpired() throws Exception {
        RangeEqualValue shortIntervalFilter = new RangeEqualValue(0.1, 0L);

        assertFalse(shortIntervalFilter.isEqual(100.0, true));

        // Força expiração do intervalo
        Field lastUpdateField = BaseEqualValue.class.getDeclaredField("lastUpdateNanos");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(shortIntervalFilter, System.nanoTime() - 1_000_000_000L);

        // Após intervalo expirado, mesmo valor dentro do range retorna false
        assertFalse(shortIntervalFilter.isEqual(100.0, true));
    }

    @Test
    void testPrecisionEdgeCases() {
        assertFalse(filter.isEqual(100.0, true));

        // Testa limites com precisão de ponto flutuante
        double base = 100.0;
        double range = base * 0.1;

        assertTrue(filter.isEqual(base + range, true));
        assertTrue(filter.isEqual(base - range, true));

        // Ligeiramente fora do range
        assertFalse(filter.isEqual(base + range + 0.00001, true));
    }

    @Test
    void testSequentialUpdatesWithinRange() {
        assertFalse(filter.isEqual(100.0, true));
        assertTrue(filter.isEqual(105.0, true));
        assertTrue(filter.isEqual(102.0, true));
        assertTrue(filter.isEqual(108.0, true));

        // Sai do range
        assertFalse(filter.isEqual(120.0, true));
    }

    @Test
    void testNegativeRange() {
        // Range negativo deve funcionar como valor absoluto
        RangeEqualValue negRangeFilter = new RangeEqualValue(-0.1, 1L);

        assertFalse(negRangeFilter.isEqual(100.0, true));

        // Deveria funcionar igual ao range positivo
        assertTrue(negRangeFilter.isEqual(105.0, true));
        assertTrue(negRangeFilter.isEqual(95.0, true));
    }

    @Test
    void testInfinityValues() {
        assertFalse(filter.isEqual(Double.POSITIVE_INFINITY, true));

        // Infinity * 0.1 = Infinity, então qualquer valor finito está "dentro" do range
        assertFalse(filter.isEqual(1000000.0, true));

        // Mas outro infinity seria considerado igual
        assertTrue(filter.isEqual(Double.POSITIVE_INFINITY, true));
    }

    @Test
    void testNaNValues() {
        assertFalse(filter.isEqual(Double.NaN, true));

        // NaN não é igual a nada, nem a si mesmo
        assertFalse(filter.isEqual(Double.NaN, true));
    }

    @Test
    void testCastFromNonNumeric() {
        // Testa ClassCastException - assumindo que cast é garantido externamente
        try {
            filter.isEqual("not a number", true);
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // Esperado
        }
    }

    @Test
    void testImplementsInterface() {
        assertTrue(filter instanceof com.opcua_arrow.data.IDataPointEqual);
    }
}
