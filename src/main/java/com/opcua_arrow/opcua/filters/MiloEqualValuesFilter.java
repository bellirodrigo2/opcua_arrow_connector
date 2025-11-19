package com.opcua_arrow.opcua.filters;

import com.opcua_arrow.interfaces.IOPCUADataValue;

import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.util.ArrayList;
import java.util.Map;

public class MiloEqualValuesFilter<T> extends BaseMiloValuesFilter<T> {
    private DataValue[] lastValue = null;
    private List<Integer> pointIds;
    private List<String> nodeIds;

    public MiloEqualValuesFilter(List<String> nodeIds, Map<String, Integer> idLookup) {
        super(nodeIds, idLookup);
    }

    @Override
    public List<IOPCUADataValue<T>> filter(DataValue[] values) {

        if (lastValue == null) {
            lastValue = values;
            List<IOPCUADataValue<T>> initialValues = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                String nodeId = nodeIds.get(i);
                Integer pointId = (pointIds != null) ? pointIds.get(i) : null;
                initialValues.add(adaptDataValue(values[i], nodeId, pointId));
            }
            return initialValues;
        }

        List<IOPCUADataValue<T>> filteredValues = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (!values[i].equals(lastValue[i])) {
                String nodeId = nodeIds.get(i);
                Integer pointId = (pointIds != null) ? pointIds.get(i) : null;
                filteredValues.add(adaptDataValue(values[i], nodeId, pointId));
            }
        }
        lastValue = values;
        return filteredValues;
    }
}
