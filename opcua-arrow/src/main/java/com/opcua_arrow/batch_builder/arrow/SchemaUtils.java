package com.opcua_arrow.batch_builder.arrow;

import java.util.ArrayList;
import java.util.List;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

/**
 * Utilities for creating Arrow schemas.
 */
public class SchemaUtils {

    /**
     * Creates an Arrow schema for OPC-UA data.
     *
     * @param valueType The Java class of the value type
     * @return The Arrow schema
     */
    public static Schema createSchema(Class<?> valueType) {
        List<Field> fields = new ArrayList<>();

        // Add pointid field
        fields.add(Field.notNullable("pointid", new ArrowType.Int(32, true)));

        // Add timestamp field (nanosecond precision with UTC timezone)
        fields.add(Field.notNullable("timestamp",
                new ArrowType.Timestamp(TimeUnit.NANOSECOND, "UTC")));

        // Add value field based on the value type
        Field valueField = createValueField(valueType);
        fields.add(valueField);

        // Add statuscode field
        fields.add(Field.notNullable("statuscode", ArrowType.Bool.INSTANCE));

        return new Schema(fields);
    }

    /**
     * Creates the value field based on the Java type
     */
    private static Field createValueField(Class<?> javaType) {
        if (javaType.isArray()) {
            // Para arrays, criar um ListVector com o tipo de elemento apropriado
            Class<?> componentType = javaType.getComponentType();
            ArrowType elementType = getScalarArrowType(componentType);

            // Campo do elemento dentro da lista
            Field elementField = Field.nullable("element", elementType);

            // Campo da lista em si - usando construtor com children
            List<Field> children = new ArrayList<>();
            children.add(elementField);

            return new Field("value",
                    new FieldType(true, new ArrowType.List(), null),
                    children);
        } else {
            // Para tipos escalares, usar diretamente
            ArrowType arrowType = getScalarArrowType(javaType);
            return Field.nullable("value", arrowType);
        }
    }

    /**
     * Maps scalar Java types to Arrow types.
     */
    private static ArrowType getScalarArrowType(Class<?> javaType) {
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

    /**
     * Maps Java types to Arrow types (mantido para compatibilidade).
     *
     * @param javaType The Java class
     * @return The corresponding Arrow type
     */
    @Deprecated
    public static ArrowType getArrowType(Class<?> javaType) {
        if (javaType.isArray()) {
            return new ArrowType.List();
        }
        return getScalarArrowType(javaType);
    }
}
