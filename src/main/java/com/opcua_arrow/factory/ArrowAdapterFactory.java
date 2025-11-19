package com.opcua_arrow.factory;

import com.opcua_arrow.arrow.ArrowAdapter;
import com.opcua_arrow.arrow.IArrowBatchBuffer;
import com.opcua_arrow.config.DataValueConfig;
import com.opcua_arrow.interfaces.IArrowAdapter;

import java.util.List;
import java.util.Map;

/**
 * Factory for creating Arrow Adapters.
 */
public class ArrowAdapterFactory {
    
    /**
     * 
     * @param <TId>
     * @param <TValue>
     * @param dataValueConfig
     * @return
     */
    public static <TId,TValue> IArrowAdapter<TId,TValue> createArrowAdapter(
        DataValueConfig dataValueConfig
            // List<String> nodeIds,
            // Map<String, Integer> idLookup,
            // String valueType,
            // int initialCapacity, boolean compressionEnabled
        ) {

        List<String> nodeIds = dataValueConfig.getNodeIds();
        Map<String, Integer> idLookup = dataValueConfig.getIdLookup();
        String valueType = dataValueConfig.getValueType();
        int initialCapacity = dataValueConfig.getInitialCapacity();
        boolean compressionEnabled = dataValueConfig.isCompressionEnabled();

        IArrowBatchBuffer<TId,TValue> builder = ArrowBatchBuilderFactory.createArrowBatchBuilder(idLookup, valueType, initialCapacity,compressionEnabled);
        
        // Create Arrow adapter
        return new ArrowAdapter<>(nodeIds, idLookup != null, builder);
        
    }
    }
