package com.opcua_arrow.factory;

import com.opcua_arrow.arrow.ArrowAdapter;
import com.opcua_arrow.arrow.IArrowBatchBuffer;
import com.opcua_arrow.arrow.builders.IIdColumn;
import com.opcua_arrow.arrow.builders.IValueColumn;
import com.opcua_arrow.arrow.builders.IntIdColumn;
import com.opcua_arrow.arrow.builders.StringIdColumn;
import com.opcua_arrow.arrow.builders.BaseArrowBatchBuilder;
import com.opcua_arrow.arrow.builders.BooleanValueColumn;
import com.opcua_arrow.arrow.builders.DoubleValueColumn;
import com.opcua_arrow.arrow.builders.IntegerValueColumn;
import com.opcua_arrow.arrow.builders.StringValueColumn;
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
     * @param nodeIds
     * @param idLookup
     * @param valueType
     * @param initialCapacity
     * @param compressionEnabled
     * @return
     */
    public static <TId,TValue> IArrowAdapter<TId,TValue> createArrowAdapter(
            List<String> nodeIds,
            Map<String, Integer> idLookup,
            String valueType,
            int initialCapacity, boolean compressionEnabled
        ) {

        IArrowBatchBuffer<TId,TValue> builder = createArrowBatchBuilder(idLookup, valueType, initialCapacity,compressionEnabled);
        
        // Create Arrow adapter
        return new ArrowAdapter<>(nodeIds, idLookup != null, builder);
        
    }
    /**
     * Creates an Arrow batch builder.
     */
    private static <TId,TValue> IArrowBatchBuffer<TId,TValue> createArrowBatchBuilder(
        Map<String, Integer> idLookup,
        String valueType,
        int initialCapacity, boolean compressionEnabled){
        IIdColumn<TId> idColumn = idLookup != null ? (IIdColumn<TId>) new IntIdColumn() : (IIdColumn<TId>) new StringIdColumn();

        IValueColumn<TValue> valueColumn;

        if (valueType.equals("Double")) {
            valueColumn= (IValueColumn<TValue>) new DoubleValueColumn();
        } else if (valueType.equals("Integer")) {
            valueColumn = (IValueColumn<TValue>) new IntegerValueColumn();
        } else if (valueType.equals("String")) {
            valueColumn = (IValueColumn<TValue>) new StringValueColumn();
        } else if (valueType.equals("Boolean")){
            valueColumn = (IValueColumn<TValue>) new BooleanValueColumn();
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + valueType);
        }

        return new BaseArrowBatchBuilder(
            initialCapacity,
            compressionEnabled,
            idColumn,
            valueColumn
        );
        }

        
    }
