package com.opcua_arrow.factory;

import com.opcua_arrow.collector.OPCUAArrowCollector;
import com.opcua_arrow.config.DataValueConfig;
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
     * @param dataValueConfig The data value configuration
     * @return A new OPC-UA Arrow collector
     */
    public static <TId,TValue> OPCUAArrowCollector<TId,TValue> createCollector(
            OPCUAClientConfig clientConfig,
            RetryPolicyConfig retryConfig,
            DataValueConfig dataValueConfig
        ) {
        
        Map<String, Integer> idLookup = dataValueConfig.getIdLookup();
        List<String> nodeIds = dataValueConfig.getNodeIds();

        IOPCUAReader<TValue> opcuaReader = OPCUAReaderFactory.createOPCUAReader(nodeIds, idLookup, retryConfig, clientConfig);
        
        // Create Arrow adapter
        IArrowAdapter<TId,TValue> arrowAdapter = ArrowAdapterFactory.createArrowAdapter(dataValueConfig);
        
        // Build and return collector
        return new OPCUAArrowCollector.Builder<TId,TValue>()
            .withOPCUAReader(opcuaReader)
            .withArrowAdapter(arrowAdapter)
            .build();
    }
    
}
