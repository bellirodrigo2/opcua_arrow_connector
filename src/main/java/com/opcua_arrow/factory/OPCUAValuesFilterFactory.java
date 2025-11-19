package com.opcua_arrow.factory;

import java.util.List;
import java.util.Map;

import com.opcua_arrow.opcua.filters.MiloEqualValuesFilter;
import com.opcua_arrow.opcua.filters.BaseMiloValuesFilter;

public class OPCUAValuesFilterFactory {


    /**
     * Creates an equal values filter.
     * 
     * @param filterType The type of filter to create
     * @param nodeIds The list of node IDs
     * @param idLookup Optional ID lookup map
     * @return A new equal values filter
     */
    public static <T> BaseMiloValuesFilter<T> createValuesFilter(
            String filterType,
            List<String> nodeIds,
            Map<String, Integer> idLookup
        ) {
        
        String filterTypeLower = filterType.toLowerCase();

        if (filterTypeLower.equals("none")) {
            return new BaseMiloValuesFilter<T>(nodeIds, idLookup);
        }   else if (filterTypeLower.equals("equals")) {
            return new MiloEqualValuesFilter<T>(nodeIds, idLookup);
        } else {
            throw new IllegalArgumentException("Unsupported filter type: " + filterType);
        }
    }
}