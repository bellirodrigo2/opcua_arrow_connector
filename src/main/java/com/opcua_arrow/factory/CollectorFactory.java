package com.opcua_arrow.factory;

import com.opcua_arrow.arrow.ArrowAdapter;
import com.opcua_arrow.collector.OPCUAArrowCollector;
import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.interfaces.IArrowAdapter;
import com.opcua_arrow.interfaces.IOPCUAConnection;
import com.opcua_arrow.interfaces.IOPCUAReader;
import com.opcua_arrow.interfaces.IRetryPolicy;
import com.opcua_arrow.opcua.MiloOPCUAConnection;
import com.opcua_arrow.opcua.MiloOPCUAReader;
import com.opcua_arrow.retry.Resilience4jRetryPolicy;

import java.util.List;
import java.util.Map;

/**
 * Factory for creating OPC-UA Arrow collectors.
 */
public class CollectorFactory {
    
    /**
     * Creates a new OPC-UA Arrow collector with the specified configurations.
     * 
     * @param <T> The type of scalar values
     * @param clientConfig The OPC-UA client configuration
     * @param retryConfig The retry policy configuration
     * @param nodeIds The list of node IDs to read
     * @param valueType The class of the value type
     * @param idLookup Optional ID lookup map for converting node IDs to integers
     * @return A new OPC-UA Arrow collector
     */
    public static <T> OPCUAArrowCollector<T> createCollector(
            OPCUAClientConfig clientConfig,
            RetryPolicyConfig retryConfig,
            List<String> nodeIds,
            Class<T> valueType,
            Map<String, Integer> idLookup) {
        
        // Create retry policy
        IRetryPolicy retryPolicy = createRetryPolicy(retryConfig);
        
        // Create OPC-UA connection
        IOPCUAConnection opcuaConnection = createOPCUAConnection(clientConfig, retryPolicy);
        
        // Create OPC-UA reader with the connection
        IOPCUAReader<T> opcuaReader = createOPCUAReader(nodeIds, valueType, opcuaConnection, retryPolicy);
        
        // Create Arrow adapter
        IArrowAdapter<T> arrowAdapter = createArrowAdapter(
            nodeIds, valueType, idLookup);
        
        // Build and return collector
        return new OPCUAArrowCollector.Builder<T>()
            .withOPCUAReader(opcuaReader)
            .withArrowAdapter(arrowAdapter)
            .build();
    }
    
    /**
     * Creates a retry policy from configuration.
     * 
     * @param config The retry policy configuration
     * @return A new retry policy
     */
    public static IRetryPolicy createRetryPolicy(RetryPolicyConfig config) {
        if (config == null) {
            config = new RetryPolicyConfig(); // Use defaults
        }
        return new Resilience4jRetryPolicy(config);
    }
    
    /**
     * Creates an OPC-UA connection from configuration.
     * 
     * @param config The OPC-UA client configuration
     * @param retryPolicy The retry policy to use
     * @return A new OPC-UA connection
     */
    public static IOPCUAConnection createOPCUAConnection(
            OPCUAClientConfig config,
            IRetryPolicy retryPolicy) {
        
        return new MiloOPCUAConnection(config, retryPolicy);
    }
    
    /**
     * Creates an OPC-UA reader from configuration.
     * 
     * @param <T> The type of scalar values
     * @param nodeIds The list of node IDs to read
     * @param valueType The class of the value type
     * @param connection The OPC-UA connection to use
     * @param retryPolicy The retry policy to use
     * @return A new OPC-UA reader
     */
    public static <T> IOPCUAReader<T> createOPCUAReader(
            List<String> nodeIds,
            Class<T> valueType,
            IOPCUAConnection connection,
            IRetryPolicy retryPolicy) {
        
        return new MiloOPCUAReader<>(nodeIds, valueType, connection, retryPolicy);
    }
    
    /**
     * Creates an Arrow adapter from configuration.
     * 
     * @param <T> The type of scalar values
     * @param nodeIds The list of node IDs
     * @param valueType The class of the value type
     * @param idLookup Optional ID lookup map
     * @return A new Arrow adapter
     */
    public static <T> IArrowAdapter<T> createArrowAdapter(
            List<String> nodeIds,
            Class<T> valueType,
            Map<String, Integer> idLookup) {
        
        return new ArrowAdapter<>(nodeIds, valueType, idLookup);
    }
    
    /**
     * Configuration builder for creating collectors.
     * 
     * @param <T> The type of scalar values
     */
    public static class ConfigBuilder<T> {
        private OPCUAClientConfig clientConfig = new OPCUAClientConfig();
        private RetryPolicyConfig retryConfig = new RetryPolicyConfig();
        private List<String> nodeIds;
        private Class<T> valueType;
        private Map<String, Integer> idLookup;
        
        public ConfigBuilder<T> withClientConfig(OPCUAClientConfig config) {
            this.clientConfig = config;
            return this;
        }
        
        public ConfigBuilder<T> withRetryConfig(RetryPolicyConfig config) {
            this.retryConfig = config;
            return this;
        }
        
        public ConfigBuilder<T> withNodeIds(List<String> nodeIds) {
            this.nodeIds = nodeIds;
            return this;
        }
        
        public ConfigBuilder<T> withValueType(Class<T> valueType) {
            this.valueType = valueType;
            return this;
        }
        
        public ConfigBuilder<T> withIdLookup(Map<String, Integer> idLookup) {
            this.idLookup = idLookup;
            return this;
        }
        
        public ConfigBuilder<T> withServerUrl(String serverUrl) {
            this.clientConfig.setServerUrl(serverUrl);
            return this;
        }
        
        public ConfigBuilder<T> withCredentials(String username, String password) {
            this.clientConfig.setUsername(username);
            this.clientConfig.setPassword(password);
            return this;
        }
        
        public OPCUAArrowCollector<T> build() {
            if (nodeIds == null || nodeIds.isEmpty()) {
                throw new IllegalArgumentException("Node IDs are required");
            }
            if (valueType == null) {
                throw new IllegalArgumentException("Value type is required");
            }
            if (clientConfig.getServerUrl() == null) {
                throw new IllegalArgumentException("Server URL is required");
            }
            
            return CollectorFactory.createCollector(
                clientConfig, retryConfig, nodeIds, valueType, idLookup);
        }
    }
}
