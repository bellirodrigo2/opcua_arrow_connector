package com.opcua_arrow.factory.transform;

import com.opcua_arrow.config.FilterConfig;
import com.opcua_arrow.transform.IDataPointEqual;
import com.opcua_arrow.transform.opcua.equals.EqualValue;
import com.opcua_arrow.transform.opcua.equals.InRangeValue;

public class FilterFactory {

    public static IDataPointEqual createFilter(FilterConfig filterConfig) {

        String type = filterConfig.getName().toLowerCase();

        switch (type) {

            case "equal":
                return new EqualValue();

            case "range":
                if (!filterConfig.isNumeric()) {
                    throw new IllegalArgumentException("Filter type 'range' requires numeric data");
                }
                Object rangeObj = filterConfig.getParameters().get("range");
                if (rangeObj == null) {
                    throw new IllegalArgumentException("Parameter 'range' is required for filter type 'range'");
                }

                double range = convertToDouble(rangeObj);
                return new InRangeValue(range);

            default:
                throw new IllegalArgumentException("Unknown filter type: " + type);
        }
    }

    private static double convertToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new IllegalArgumentException("Cannot convert parameter to double: " + value);
    }
}
