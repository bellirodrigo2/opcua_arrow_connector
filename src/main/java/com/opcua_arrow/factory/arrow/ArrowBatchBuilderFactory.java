package com.opcua_arrow.factory.arrow;

import com.opcua_arrow.writer.IArrowBatchBuffer;
import com.opcua_arrow.writer.arrow.AcumBatchArrowBuilder;
import com.opcua_arrow.writer.arrow.IValueColumn;
import com.opcua_arrow.writer.arrow.columns.BooleanValueColumn;
import com.opcua_arrow.writer.arrow.columns.DoubleValueColumn;
import com.opcua_arrow.writer.arrow.columns.IntegerValueColumn;
import com.opcua_arrow.writer.arrow.columns.StringValueColumn;

public class ArrowBatchBuilderFactory {

    static IArrowBatchBuffer createArrowBatchBuffer(String dataType, int initialCapacity, boolean compress) {
        IValueColumn valueColumn = createValueColumn(dataType);
        return new AcumBatchArrowBuilder(initialCapacity, compress, valueColumn);
    }

    static IValueColumn createValueColumn(String dataType) {

        String dataTypeLower = dataType.toLowerCase();

        if (dataTypeLower.equals("double") || dataTypeLower.equals("float") || dataTypeLower.equals("numeric"))
            return new DoubleValueColumn();
        if (dataTypeLower.equals("int") || dataTypeLower.equals("integer") || dataTypeLower.equals("long"))
            return new IntegerValueColumn();
        if (dataTypeLower.equals("string") || dataTypeLower.equals("varchar") || dataTypeLower.equals("text"))
            return new StringValueColumn();
        if (dataTypeLower.equals("boolean") || dataTypeLower.equals("bool"))
            return new BooleanValueColumn();
        throw new IllegalArgumentException("Unsupported data type: " + dataType);
    }
}