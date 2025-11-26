package com.opcua_arrow.data;

import com.opcua_arrow.data.equals.NoFilter;
import com.opcua_arrow.data.equals.RangeEqualValue;
import com.opcua_arrow.data.equals.StrictEqualValue;
import com.opcua_arrow.service.DataPointDTO;

public class DataPoint {

    private final String nodeId;
    private final int pointId;
    private final EDataType dataType;
    private final IDataPointEqual equals;
    private final DataWriteGroup writeGroup;
    private final DataReadGroup readGroup;

    public DataPoint(String nodeId,
            int pointId,
            EDataType dataType,
            IDataPointEqual equals,
            DataWriteGroup writeGroup,
            DataReadGroup readGroup) {
        this.nodeId = nodeId;
        this.pointId = pointId;
        this.dataType = dataType;
        this.equals = equals;
        this.writeGroup = writeGroup;
        this.readGroup = readGroup;

    }

    public static DataPoint fromConfig(DataPointDTO config) {
        return DataPointFactory.createDataPoint(config);
    }

    public EDataType getDataType() {
        return dataType;
    }

    public int getPointId() {
        return pointId;
    }

    public IDataPointEqual getEquals() {
        return equals;
    }

    public DataWriteGroup getWriteGroup() {
        return writeGroup;
    }

    public DataReadGroup getReadGroup() {
        return readGroup;
    }

    public String getNodeId() {
        return nodeId;
    }

    private static class DataPointFactory {

        static public DataPoint createDataPoint(DataPointDTO config) {

            String nodeId = config.nodeId;
            Integer pointId = config.pointId;
            EDataType dataType = getDataType(config.valueType);
            EReadMode readMode = EReadMode.valueOf(config.readType.toUpperCase());
            IDataPointEqual equals = config.hasFilter ? createEquals(config.filterRange, config.filterIntervalSeconds,
                    isNumeric(dataType)) : new NoFilter();
            DataWriteGroup group = createDataWriteGroup(dataType, config.minRange,
                    config.maxRange);
            DataReadGroup interval = createDataReadGroup(readMode, config.interval_seconds);

            return new DataPoint(nodeId, pointId, dataType, equals, group, interval);
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
}
