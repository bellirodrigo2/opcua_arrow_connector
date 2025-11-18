package com.opcua.arrow.factory;

import com.opcua.arrow.adapter.ArrowAdapter;
import com.opcua.arrow.collector.OPCUAArrowCollector;
import com.opcua.arrow.config.OPCUAClientConfig;
import com.opcua.arrow.config.RetryPolicyConfig;
import com.opcua.arrow.interfaces.IArrowAdapter;
import com.opcua.arrow.interfaces.IOPCUAClient;
import com.opcua.arrow.interfaces.IRetryPolicy;
import com.opcua.arrow.opcua.MiloOPCUAClientThreadSafe;
import com.opcua.arrow.retry.Resilience4jRetryPolicy;

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
        
        // Create OPC-UA client
        IOPCUAClient<T> opcuaClient = createOPCUAClient(
            clientConfig, nodeIds, valueType, retryPolicy);
        
        // Create Arrow adapter
        IArrowAdapter<T> arrowAdapter = createArrowAdapter(
            nodeIds, valueType, idLookup);
        
        // Build and return collector
        return new OPCUAArrowCollector.Builder<T>()
            .withOPCUAClient(opcuaClient)
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
     * Creates an OPC-UA client from configuration.
     * 
     * @param <T> The type of scalar values
     * @param config The OPC-UA client configuration
     * @param nodeIds The list of node IDs to read
     * @param valueType The class of the value type
     * @param retryPolicy The retry policy to use
     * @return A new OPC-UA client
     */
    public static <T> IOPCUAClient<T> createOPCUAClient(
            OPCUAClientConfig config,
            List<String> nodeIds,
            Class<T> valueType,
            IRetryPolicy retryPolicy) {
        
        return new MiloOPCUAClientThreadSafe<>(config, nodeIds, valueType, retryPolicy);
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
