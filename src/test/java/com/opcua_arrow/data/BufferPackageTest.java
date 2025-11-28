package com.opcua_arrow.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BufferPackageTest {

    private DataWriteGroup testGroup;

    @BeforeEach
    void setUp() {
        IntRange range = new IntRange(1, 10);
        testGroup = new DataWriteGroup(EDataType.NUMERIC, range);
    }

    @Test
    void testConstructorAndGetters() {
        byte[] buffer = { 1, 2, 3, 4, 5 };
        BufferPackage pkg = new BufferPackage(buffer, testGroup);

        assertSame(buffer, pkg.getBuffer());
        assertSame(testGroup, pkg.getGroup());
    }

    @Test
    void testWithEmptyBuffer() {
        byte[] buffer = new byte[0];
        BufferPackage pkg = new BufferPackage(buffer, testGroup);

        assertSame(buffer, pkg.getBuffer());
        assertEquals(0, pkg.getBuffer().length);
        assertSame(testGroup, pkg.getGroup());
    }

    @Test
    void testWithNullBuffer() {
        BufferPackage pkg = new BufferPackage(null, testGroup);

        assertNull(pkg.getBuffer());
        assertSame(testGroup, pkg.getGroup());
    }

    @Test
    void testWithNullGroup() {
        byte[] buffer = { 10, 20, 30 };
        BufferPackage pkg = new BufferPackage(buffer, null);

        assertSame(buffer, pkg.getBuffer());
        assertNull(pkg.getGroup());
    }

    @Test
    void testWithBothNull() {
        BufferPackage pkg = new BufferPackage(null, null);

        assertNull(pkg.getBuffer());
        assertNull(pkg.getGroup());
    }

    @Test
    void testBufferModification() {
        byte[] buffer = { 1, 2, 3 };
        BufferPackage pkg = new BufferPackage(buffer, testGroup);

        // Verifica que retorna a mesma referência
        byte[] retrievedBuffer = pkg.getBuffer();
        assertSame(buffer, retrievedBuffer);

        // Modifica o buffer original
        buffer[0] = 10;
        assertEquals(10, pkg.getBuffer()[0]);
    }

    @Test
    void testLargeBuffer() {
        byte[] buffer = new byte[1000000];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (i % 256);
        }

        BufferPackage pkg = new BufferPackage(buffer, testGroup);

        assertSame(buffer, pkg.getBuffer());
        assertEquals(1000000, pkg.getBuffer().length);
    }
}
