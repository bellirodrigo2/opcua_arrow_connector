package com.opcua_arrow.opcua.filters;

import com.opcua_arrow.interfaces.IOPCUAValuesFilter;
import com.opcua_arrow.opcua.MiloDataValueAdapter;
import com.opcua_arrow.interfaces.IOPCUADataValue;

import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.util.ArrayList;
import java.util.Map;

public class BaseMiloValuesFilter<T> implements IOPCUAValuesFilter<T, DataValue> {
    private DataValue[] lastValue = null;
    private List<Integer> pointIds;
    private List<String> nodeIds;

    public BaseMiloValuesFilter(List<String> nodeIds, Map<String, Integer> idLookup) {
        this.nodeIds = nodeIds;
        if (idLookup == null) {
            this.pointIds = null;
        }else{
            this.pointIds = new ArrayList<>();
            for (String nodeId : nodeIds) {
                Integer pointId = idLookup.get(nodeId);
                pointIds.add(pointId);
            }
        }
    }

    @Override
    public List<IOPCUADataValue<T>> filter(DataValue[] values) {
        List<IOPCUADataValue<T>> initialValues = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            String nodeId = nodeIds.get(i);
            Integer pointId = (pointIds != null) ? pointIds.get(i) : null;
            initialValues.add(adaptDataValue(values[i], nodeId, pointId));
        }
        return initialValues;
    }
    protected IOPCUADataValue<T> adaptDataValue(DataValue dataValue, String nodeId, Integer pointId) {
        return new MiloDataValueAdapter<T>(dataValue, nodeId, pointId);
    }
    
}