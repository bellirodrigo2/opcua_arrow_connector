package com.opcua_arrow.factory;

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
import java.util.Map;

/**
 * Factory for creating Arrow Adapters.
 * 
 */
public class ArrowBatchBuilderFactory {

    /**
     * 
     * @param <TId>
     * @param <TValue>
     * @param idLookup
     * @param valueType
     * @param initialCapacity
     * @param compressionEnabled
     * @return
     */

    public static <TId,TValue> IArrowBatchBuffer<TId,TValue> createArrowBatchBuilder(
        Map<String, Integer> idLookup,
        String valueType,
        int initialCapacity, boolean compressionEnabled){
        IIdColumn<TId> idColumn = idLookup != null ? (IIdColumn<TId>) new IntIdColumn() : (IIdColumn<TId>) new StringIdColumn();

        IValueColumn<TValue> valueColumn;

        String valueTypeLower = valueType.toLowerCase();

        if (valueTypeLower.equals("double") || valueTypeLower.equals("numeric")) {
            valueColumn= (IValueColumn<TValue>) new DoubleValueColumn();
        } else if (valueTypeLower.equals("integer")) {
            valueColumn = (IValueColumn<TValue>) new IntegerValueColumn();
        } else if (valueTypeLower.equals("string")) {
            valueColumn = (IValueColumn<TValue>) new StringValueColumn();
        } else if (valueTypeLower.equals("boolean")){
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