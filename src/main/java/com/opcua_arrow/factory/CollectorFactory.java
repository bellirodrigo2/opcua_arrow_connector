package com.opcua_arrow.factory;

import com.opcua_arrow.collector.OPCUAArrowCollector;
import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.interfaces.IArrowAdapter;
import com.opcua_arrow.interfaces.IOPCUAReader;

import java.util.List;
import java.util.Map;

/**
 * Factory for creating OPC-UA Arrow collectors.
 */
public class CollectorFactory {
    
    /**
     * Creates a new OPC-UA Arrow collector with the specified configurations.
     * 
     * @param <TValue> The type of scalar values
     * @param clientConfig The OPC-UA client configuration
     * @param retryConfig The retry policy configuration
     * @param nodeIds The list of node IDs to read
     * @param valueType The class of the value type
     * @param idLookup Optional ID lookup map for converting node IDs to integers
     * @return A new OPC-UA Arrow collector
     */
    public static <TId,TValue> OPCUAArrowCollector<TId,TValue> createCollector(
            OPCUAClientConfig clientConfig,
            RetryPolicyConfig retryConfig,
            List<String> nodeIds,
            Map<String, Integer> idLookup,
            String valueType,
            int initialCapacity, boolean compressionEnabled) {
        
        IOPCUAReader<TValue> opcuaReader = OPCUAReaderFactory.createOPCUAReader(nodeIds, idLookup, retryConfig, clientConfig);
        
        // Create Arrow adapter
        IArrowAdapter<TId,TValue> arrowAdapter = ArrowAdapterFactory.createArrowAdapter(nodeIds, idLookup, valueType, initialCapacity,compressionEnabled);
        
        // Build and return collector
        return new OPCUAArrowCollector.Builder<TId,TValue>()
            .withOPCUAReader(opcuaReader)
            .withArrowAdapter(arrowAdapter)
            .build();
    }
    
}
