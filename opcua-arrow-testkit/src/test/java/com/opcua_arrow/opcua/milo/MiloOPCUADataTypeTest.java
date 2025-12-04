package com.opcua_arrow.opcua.milo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.opcua.OPCUAServerExtension;
import com.opcua_arrow.opcua.retry.IRetryPolicy;
import com.opcua_arrow.opcua.retry.resilience4j.Resilience4jRetryPolicy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for MiloOPCUADataType.
 *
 * Tests data type discovery and Java type mapping for OPC-UA nodes.
 */
@ExtendWith(OPCUAServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MiloOPCUADataTypeTest {

    private MiloOPCUAConnection connection;
    private MiloOPCUADataType dataType;
    private IRetryPolicy retryPolicy;

    @BeforeAll
    void setup() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(10));
        config.setSessionTimeout(Duration.ofSeconds(120));
        config.setKeepAliveInterval(Duration.ofMinutes(10));

        RetryPolicyConfig retryConfig = new RetryPolicyConfig()
                .setMaxAttempts(3)
                .setInitialDelay(Duration.ofMillis(100))
                .setMaxDelay(Duration.ofSeconds(2))
                .setUseJitter(true);

        retryPolicy = new Resilience4jRetryPolicy(retryConfig);
        connection = new MiloOPCUAConnection(config, retryPolicy);

        // Connect to server
        connection.connect().get();

        // Create data type instance
        dataType = new MiloOPCUADataType(connection, retryPolicy);
    }

    @AfterAll
    void cleanup() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    // ========================================
    // Basic Java Type Mapping Tests
    // ========================================

    @Test
    void testGetJavaTypeForInt32Node() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/Int32");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Integer.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Int32"));
    }

    @Test
    void testGetJavaTypeForDoubleNode() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/Double");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Double.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Double"));
    }

    @Test
    void testGetJavaTypeForBooleanNode() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/Boolean");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Boolean.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Boolean"));
    }

    @Test
    void testGetJavaTypeForStringNode() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/String");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(String.class, types.get("ns=2;s=HelloWorld/ScalarTypes/String"));
    }

    @Test
    void testGetJavaTypeForFloatNode() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/Float");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Float.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Float"));
    }

    @Test
    void testGetJavaTypeForInt16Node() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/Int16");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Short.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Int16"));
    }

    @Test
    void testGetJavaTypeForInt64Node() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/Int64");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Long.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Int64"));
    }

    // ========================================
    // Batch Operations Tests
    // ========================================

    @Test
    void testGetJavaTypesForMultipleNodes() throws Exception {
        List<String> nodeIds = List.of(
                "ns=2;s=HelloWorld/ScalarTypes/Int32",
                "ns=2;s=HelloWorld/ScalarTypes/Double",
                "ns=2;s=HelloWorld/ScalarTypes/Boolean",
                "ns=2;s=HelloWorld/ScalarTypes/String");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(4, types.size());
        assertEquals(Integer.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Int32"));
        assertEquals(Double.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Double"));
        assertEquals(Boolean.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Boolean"));
        assertEquals(String.class, types.get("ns=2;s=HelloWorld/ScalarTypes/String"));
    }

    @Test
    void testGetJavaTypesPreservesOrder() throws Exception {
        List<String> nodeIds = List.of(
                "ns=2;s=HelloWorld/ScalarTypes/String",
                "ns=2;s=HelloWorld/ScalarTypes/Int32",
                "ns=2;s=HelloWorld/ScalarTypes/Boolean");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        // LinkedHashMap preserves insertion order
        List<String> keys = types.keySet().stream().toList();
        assertEquals("ns=2;s=HelloWorld/ScalarTypes/String", keys.get(0));
        assertEquals("ns=2;s=HelloWorld/ScalarTypes/Int32", keys.get(1));
        assertEquals("ns=2;s=HelloWorld/ScalarTypes/Boolean", keys.get(2));
    }

    @Test
    void testGetJavaTypesWithEmptyListThrowsException() {
        // OPC-UA servers return Bad_NothingToDo when reading an empty list
        // This is expected behavior - the method doesn't guard against empty input
        List<String> nodeIds = List.of();

        assertThrows(Exception.class, () -> {
            dataType.getJavaTypes(nodeIds);
        });
    }

    // ========================================
    // Dynamic Node Tests
    // ========================================

    @Test
    void testGetJavaTypeForDynamicInt32Node() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/Dynamic/Int32");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Integer.class, types.get("ns=2;s=HelloWorld/Dynamic/Int32"));
    }

    @Test
    void testGetJavaTypeForDynamicDoubleNode() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/Dynamic/Double");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(Double.class, types.get("ns=2;s=HelloWorld/Dynamic/Double"));
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    void testGetJavaTypeForInvalidNodeReturnsNull() throws Exception {
        List<String> nodeIds = List.of("ns=99;s=NonExistent/Node");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(1, types.size());
        // Invalid node should return null for the class
        assertNull(types.get("ns=99;s=NonExistent/Node"));
    }

    @Test
    void testGetJavaTypesWithMixedValidAndInvalidNodes() throws Exception {
        List<String> nodeIds = List.of(
                "ns=2;s=HelloWorld/ScalarTypes/Int32", // Valid
                "ns=99;s=NonExistent/Node", // Invalid
                "ns=2;s=HelloWorld/ScalarTypes/Double" // Valid
        );

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(3, types.size());
        assertEquals(Integer.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Int32"));
        assertNull(types.get("ns=99;s=NonExistent/Node"));
        assertEquals(Double.class, types.get("ns=2;s=HelloWorld/ScalarTypes/Double"));
    }

    @Test
    void testGetJavaTypesWhenDisconnectedThrowsException() throws Exception {
        // Create a disconnected instance
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(60));
        config.setKeepAliveInterval(Duration.ofMinutes(10));

        MiloOPCUAConnection disconnectedConnection = new MiloOPCUAConnection(config, retryPolicy);
        MiloOPCUADataType disconnectedDataType = new MiloOPCUADataType(disconnectedConnection, retryPolicy);

        // Should throw because not connected
        assertThrows(IllegalStateException.class, () -> {
            disconnectedDataType.getJavaTypes(List.of("ns=2;s=HelloWorld/ScalarTypes/Int32"));
        });
    }

    // ========================================
    // Data Type Tree Caching Tests
    // ========================================

    @Test
    void testDataTypeTreeIsCached() throws Exception {
        // First call builds the tree
        Map<String, Class<?>> types1 = dataType.getJavaTypes(
                List.of("ns=2;s=HelloWorld/ScalarTypes/Int32"));

        // Second call should use cached tree
        Map<String, Class<?>> types2 = dataType.getJavaTypes(
                List.of("ns=2;s=HelloWorld/ScalarTypes/Double"));

        // Both should work correctly
        assertEquals(Integer.class, types1.get("ns=2;s=HelloWorld/ScalarTypes/Int32"));
        assertEquals(Double.class, types2.get("ns=2;s=HelloWorld/ScalarTypes/Double"));
    }

    @Test
    void testMultipleCallsReturnConsistentResults() throws Exception {
        List<String> nodeIds = List.of("ns=2;s=HelloWorld/ScalarTypes/Int32");

        Map<String, Class<?>> types1 = dataType.getJavaTypes(nodeIds);
        Map<String, Class<?>> types2 = dataType.getJavaTypes(nodeIds);
        Map<String, Class<?>> types3 = dataType.getJavaTypes(nodeIds);

        assertEquals(types1.get("ns=2;s=HelloWorld/ScalarTypes/Int32"),
                types2.get("ns=2;s=HelloWorld/ScalarTypes/Int32"));
        assertEquals(types2.get("ns=2;s=HelloWorld/ScalarTypes/Int32"),
                types3.get("ns=2;s=HelloWorld/ScalarTypes/Int32"));
    }

    // ========================================
    // All Scalar Types Test
    // ========================================

    @Test
    void testGetJavaTypesForAllScalarTypes() throws Exception {
        List<String> nodeIds = List.of(
                "ns=2;s=HelloWorld/ScalarTypes/Boolean",
                "ns=2;s=HelloWorld/ScalarTypes/Byte",
                "ns=2;s=HelloWorld/ScalarTypes/Int16",
                "ns=2;s=HelloWorld/ScalarTypes/Int32",
                "ns=2;s=HelloWorld/ScalarTypes/Int64",
                "ns=2;s=HelloWorld/ScalarTypes/Float",
                "ns=2;s=HelloWorld/ScalarTypes/Double",
                "ns=2;s=HelloWorld/ScalarTypes/String");

        Map<String, Class<?>> types = dataType.getJavaTypes(nodeIds);

        assertNotNull(types);
        assertEquals(8, types.size());

        // All types should be resolved
        for (String nodeId : nodeIds) {
            assertNotNull(types.get(nodeId), "Type for " + nodeId + " should not be null");
        }
    }
}
