package com.opcua_arrow.arrow;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for creating Arrow schemas.
 */
public class SchemaUtils {
    
    /**
     * Creates an Arrow schema for OPC-UA data.
     * 
     * @param valueType The Java class of the value type
     * @param idLookup Optional ID lookup map for converting node IDs to integers
     * @return The Arrow schema
     */
    public static Schema createSchema(Class<?> valueType, Class<?> idType) {
        List<Field> fields = new ArrayList<>();
        
        // Add pointid field (either string or int32 based on idLookup)
        if (idType == Integer.class || idType == int.class) {
            fields.add(Field.notNullable("pointid", new ArrowType.Int(32, true)));
        } else {
            if (idType != String.class) {
                fields.add(Field.notNullable("pointid", ArrowType.Utf8.INSTANCE));
            }else{
                throw new IllegalArgumentException("Unsupported ID type: " + idType);
            }
        }
        
        // Add timestamp field (nanosecond precision with UTC timezone)
        fields.add(Field.notNullable("timestamp", 
            new ArrowType.Timestamp(TimeUnit.NANOSECOND, "UTC")));
        
        // Add value field based on the value type
        ArrowType arrowValueType = getArrowType(valueType);
        fields.add(Field.nullable("value", arrowValueType));
        
        // Add statuscode field
        fields.add(Field.notNullable("statuscode", new ArrowType.Int(32, true)));
        
        return new Schema(fields);
    }
    
    /**
     * Maps Java types to Arrow types.
     * 
     * @param javaType The Java class
     * @return The corresponding Arrow type
     */
    public static ArrowType getArrowType(Class<?> javaType) {
        if (javaType == Double.class || javaType == double.class) {
            return new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE);
        } else if (javaType == Float.class || javaType == float.class) {
            return new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE);
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return ArrowType.Bool.INSTANCE;
        } else if (javaType == String.class) {
            return ArrowType.Utf8.INSTANCE;
        } else if (javaType == Integer.class || javaType == int.class) {
            return new ArrowType.Int(32, true);
        } else if (javaType == Long.class || javaType == long.class) {
            return new ArrowType.Int(64, true);
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + javaType);
        }
    }
}
