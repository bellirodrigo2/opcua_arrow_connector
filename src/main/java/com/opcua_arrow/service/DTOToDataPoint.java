package com.opcua_arrow.service;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.EDataType;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.IDataPointEqual;
import com.opcua_arrow.data.IntRange;
import com.opcua_arrow.data.equals.NoFilter;
import com.opcua_arrow.data.equals.RangeEqualValue;
import com.opcua_arrow.data.equals.StrictEqualValue;

public class DTOToDataPoint {

    static public DataPoint createDataPoint(DataPointDTO config) {

        String name = config.name;
        String description = config.description;
        String nodeId = config.nodeId;
        Integer pointId = config.pointId;
        EDataType dataType = getDataType(config.valueType);
        EReadMode readMode = EReadMode.valueOf(config.readType.toUpperCase());
        if (dataType == EDataType.EVENTS && readMode == EReadMode.EVENTS) {
            throw new IllegalArgumentException("DataPoint cannot have EVENTS data type and EVENTS read mode");
        }
        IDataPointEqual equals = config.hasFilter ? createEquals(config.filterRange, config.filterIntervalSeconds,
                isNumeric(dataType)) : new NoFilter();
        DataWriteGroup group = createDataWriteGroup(dataType, config.minRange,
                config.maxRange);
        DataReadGroup interval = createDataReadGroup(readMode, config.interval_seconds);

        return new DataPoint(name, description, nodeId, pointId, dataType, equals, group, interval);
    }

    static private DataReadGroup createDataReadGroup(EReadMode readMode, long interval) {
        return new DataReadGroup(readMode, interval);
    }

    static private IDataPointEqual createEquals(double filterRange, long filterIntervalSeconds, boolean isNumeric) {
        if (filterRange > 0 && isNumeric) {
            return new RangeEqualValue(filterRange, filterIntervalSeconds);
        }
        return new StrictEqualValue(filterIntervalSeconds);
    }

    static private boolean isNumeric(EDataType dataType) {
        return dataType == EDataType.NUMERIC;
    }

    static private DataWriteGroup createDataWriteGroup(EDataType dataType, int minRange,
            int maxRange) {
        IntRange intRange = new IntRange(minRange, maxRange);
        return new DataWriteGroup(dataType, intRange);
    }

    static private EDataType getDataType(String valueType) {
        return switch (valueType.toLowerCase()) {
            case "boolean" -> EDataType.BOOLEAN;
            case "string" -> EDataType.STRING;
            case "int16", "uint16", "int32", "uint32", "int64", "uint64", "float", "double" -> EDataType.NUMERIC;
            case "arrayboolean" -> EDataType.BOOLEAN_ARRAY;
            case "arraynumeric" -> EDataType.NUMERIC_ARRAY;
            default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
        };
    }
}
