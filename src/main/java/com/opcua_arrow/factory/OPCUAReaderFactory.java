package com.opcua_arrow.factory;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.interfaces.IOPCUAConnection;
import com.opcua_arrow.interfaces.IOPCUAReader;
import com.opcua_arrow.interfaces.IRetryPolicy;
import com.opcua_arrow.opcua.MiloOPCUAConnection;
import com.opcua_arrow.opcua.MiloOPCUAReader;
import com.opcua_arrow.opcua.filters.BaseMiloValuesFilter;
import com.opcua_arrow.retry.Resilience4jRetryPolicy;

import java.util.List;
import java.util.Map;

/**
 * Factory for creating OPC-UA Reader.
 */
public class OPCUAReaderFactory {
    
    /**
     * Creates a retry policy from configuration.
     * 
     * @param config The retry policy configuration
     * @return A new retry policy
     */
    private static IRetryPolicy createRetryPolicy(RetryPolicyConfig config) {
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
    private static IOPCUAConnection createOPCUAConnection(
            OPCUAClientConfig config,
            IRetryPolicy retryPolicy) {
        
        return new MiloOPCUAConnection(config, retryPolicy);
    }
    
    /**
     * Creates an OPC-UA reader from configuration.
     * 
     * @param nodeIds The list of node IDs to read
     * @param idLookup Optional ID lookup map for converting node IDs to integers
     * @param retryConfig The retry policy configuration
     * @param clientConfig The OPC-UA client configuration
     * @return A new OPC-UA reader
     */
    public static <T> IOPCUAReader<T> createOPCUAReader(
            List<String> nodeIds,
            Map<String, Integer> idLookup,
            RetryPolicyConfig retryConfig,
            OPCUAClientConfig clientConfig
        ) {

        IRetryPolicy retryPolicy = createRetryPolicy(retryConfig);
        
        BaseMiloValuesFilter<T> valuesFilter = OPCUAValuesFilterFactory.createValuesFilter(clientConfig.getFilterType(),nodeIds, idLookup);

        IOPCUAConnection connection = createOPCUAConnection(clientConfig, retryPolicy);

        return new MiloOPCUAReader<>(nodeIds, connection, retryPolicy, valuesFilter);
    }
    
}

