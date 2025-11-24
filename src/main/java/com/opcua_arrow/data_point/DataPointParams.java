package com.opcua_arrow.data_point;

import com.opcua_arrow.data_point.equals.RangeEqualValue;
import com.opcua_arrow.data_point.equals.StrictEqualValue;
import com.opcua_arrow.service.DataPointDTO;

public class DataPointParams {

    private final String nodeId;
    private final int pointId;
    private final Class<?> valueTypeClass;
    private final IDataPointEqual equals;
    private final DataWriteGroup writeGroup;
    private final DataReadGroup readGroup;

    public DataPointParams(String nodeId,
            int pointId,
            Class<?> valueTypeClass,
            IDataPointEqual equals,
            DataWriteGroup writeGroup,
            DataReadGroup readGroup) {
        this.nodeId = nodeId;
        this.pointId = pointId;
        this.valueTypeClass = valueTypeClass;
        this.equals = equals;
        this.writeGroup = writeGroup;
        this.readGroup = readGroup;

    }

    public static DataPointParams fromConfig(DataPointDTO config) {
        return DataPointParamsFactory.createDataPointParams(config);
    }

    public Class<?> getValueTypeClass() {
        return valueTypeClass;
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

    private static class DataPointParamsFactory {

        static public DataPointParams createDataPointParams(DataPointDTO config) {

            String nodeId = config.nodeId;
            Integer pointId = config.pointId;
            Class<?> valueTypeClass = getClassType(config.valueType);
            IDataPointEqual equals = createEquals(config.filterRange, config.filterIntervalSeconds,
                    isNumeric(valueTypeClass));
            DataWriteGroup group = createDataWriteGroup(config.groupName, valueTypeClass, config.minRange,
                    config.maxRange);
            DataReadGroup interval = createDataReadGroup(valueTypeClass, config.readType, config.interval_seconds);

            return new DataPointParams(nodeId, pointId, valueTypeClass, equals, group, interval);
        }

        static private DataReadGroup createDataReadGroup(Class<?> clazz, String readType, long interval) {
            return new DataReadGroup(clazz, readType, interval);
        }

        static private IDataPointEqual createEquals(double filterRange, long filterIntervalSeconds, boolean isNumeric) {
            if (filterRange > 0 && isNumeric) {
                return new RangeEqualValue(filterRange, filterIntervalSeconds);
            }
            return new StrictEqualValue(filterIntervalSeconds);
        }

        static private boolean isNumeric(Class<?> clazz) {
            return Number.class.isAssignableFrom(clazz)
                    || clazz.isPrimitive() && clazz != boolean.class && clazz != char.class;
        }

        static private DataWriteGroup createDataWriteGroup(String groupName, Class<?> clazz, int minRange,
                int maxRange) {
            IntRange intRange = new IntRange(minRange, maxRange);
            return new DataWriteGroup(groupName, clazz, intRange);
        }

        static private Class<?> getClassType(String valueType) {
            switch (valueType.toLowerCase()) {
                case "boolean":
                    return Boolean.class;
                case "string":
                    return String.class;
                case "float":
                    return Double.class;
                case "double":
                    return Double.class;
                case "int16":
                    return Double.class;
                case "uint16":
                    return Double.class;
                case "int32":
                    return Double.class;
                case "uint32":
                    return Double.class;
                case "int64":
                    return Double.class;
                case "uint64":
                    return Double.class;
                case "numeric":
                    return Double.class;
                default:
                    throw new IllegalArgumentException("Unsupported value type: " + valueType);
            }
        }

    }
}
