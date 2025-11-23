package com.opcua_arrow.data_point.opcua;

import java.util.Map;

import com.opcua_arrow.data_point.DataReadGroup;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.IDataPointEqual;
import com.opcua_arrow.data_point.IDataPointParams;
import com.opcua_arrow.data_point.IntRange;
import com.opcua_arrow.data_point.opcua.equals.EqualValue;
import com.opcua_arrow.data_point.opcua.equals.InRangeValue;
import com.opcua_arrow.data_point_provider.DataPointDTO;

public class DataPointParams implements IDataPointParams {

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

    @Override
    public Class<?> getValueTypeClass() {
        return valueTypeClass;
    }

    @Override
    public int getPointId() {
        return pointId;
    }

    @Override
    public IDataPointEqual getEquals() {
        return equals;
    }

    @Override
    public DataWriteGroup getWriteGroup() {
        return writeGroup;
    }

    @Override
    public DataReadGroup getReadGroup() {
        return readGroup;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    private static class DataPointParamsFactory {

        static public DataPointParams createDataPointParams(DataPointDTO config) {

            String nodeId = config.nodeId;
            Integer pointId = config.pointId;
            Class<?> valueTypeClass = getClassType(config.valueType);
            IDataPointEqual equals = createEquals(config.filterType, config.filterParameters, valueTypeClass);
            DataWriteGroup group = createDataWriteGroup(config.groupName, valueTypeClass, config.minRange,
                    config.maxRange);
            DataReadGroup interval = createDataReadGroup(valueTypeClass, config.readType, config.interval_seconds);

            return new DataPointParams(nodeId, pointId, valueTypeClass, equals, group, interval);
        }

        static private DataReadGroup createDataReadGroup(Class<?> clazz, String readType, long interval) {
            return new DataReadGroup(clazz, readType, interval);
        }

        static private double convertToDouble(Object value) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            throw new IllegalArgumentException("Cannot convert parameter to double: " + value);
        }

        static private IDataPointEqual createEquals(String fitlerName, Map<String, Object> parameters, Class<?> clazz) {
            String filterType = fitlerName.toLowerCase();

            switch (filterType) {

                case "equal":
                    return new EqualValue();

                case "range":
                    if (!isNumeric(clazz)) {
                        throw new IllegalArgumentException("Filter type 'range' requires numeric data");
                    }
                    Object rangeObj = parameters.get("range");
                    if (rangeObj == null) {
                        throw new IllegalArgumentException("Parameter 'range' is required for filter type 'range'");
                    }

                    double range = convertToDouble(rangeObj);
                    return new InRangeValue(range);

                default:
                    throw new IllegalArgumentException("Unknown filter type: " + filterType);
            }

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
