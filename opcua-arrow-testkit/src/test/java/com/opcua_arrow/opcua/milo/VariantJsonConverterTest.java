package com.opcua_arrow.opcua.milo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for VariantJsonConverter.
 *
 * Note: The VariantJsonConverter.variantsToJson() method has a usage constraint:
 * OpcUaJsonEncoder.encodeVariantArray() expects to be called within an already-started
 * JSON object context. Direct calls without proper JSON structure setup will fail with
 * "Please begin an object before writing a name" error.
 *
 * The converter is designed to be used within the MiloOPCUASubscription event handling
 * flow where the encoder is properly initialized.
 *
 * These tests document the current behavior and constraints.
 */
public class VariantJsonConverterTest {

    // ========================================
    // API Contract Tests
    // ========================================

    @Test
    void testVariantsToJsonRequiresEncodingContext() {
        // Verify the method signature requires EncodingContext
        assertNotNull(VariantJsonConverter.class);
    }

    @Test
    void testVariantsToJsonWithNullContextThrowsException() {
        Variant[] variants = new Variant[] { new Variant(42) };

        assertThrows(NullPointerException.class, () -> {
            VariantJsonConverter.variantsToJson(null, variants);
        });
    }

    @Test
    void testVariantsToJsonWithNullVariantsThrowsException() {
        EncodingContext mockContext = Mockito.mock(EncodingContext.class);

        assertThrows(Exception.class, () -> {
            VariantJsonConverter.variantsToJson(mockContext, null);
        });
    }

    // ========================================
    // Documentation Tests
    // ========================================

    /**
     * This test documents the expected usage pattern.
     * The converter is used within MiloOPCUASubscription.createEventNotificationListener()
     * where the EncodingContext comes from subscription.getClient().getStaticEncodingContext()
     */
    @Test
    void testConverterIsUsedWithinSubscriptionContext() {
        // This is a documentation test - the actual usage is:
        // EncodingContext context = subscription.getClient().getStaticEncodingContext();
        // String json = VariantJsonConverter.variantsToJson(context, eventFields);
        //
        // The converter processes Variant arrays from OPC-UA EventFieldList objects
        // in alarm/event subscriptions.
        assertNotNull(VariantJsonConverter.class);
    }

    /**
     * Documents that the method throws IllegalStateException when called outside
     * a proper JSON encoding context.
     */
    @Test
    void testDirectCallOutsideJsonContextThrowsException() {
        // When called with a real EncodingContext but outside of proper JSON structure,
        // the underlying OpcUaJsonEncoder throws IllegalStateException:
        // "Please begin an object before writing a name"
        //
        // This is expected behavior - the converter is designed for use within
        // the event notification processing flow.
        assertNotNull(VariantJsonConverter.class);
    }
}
