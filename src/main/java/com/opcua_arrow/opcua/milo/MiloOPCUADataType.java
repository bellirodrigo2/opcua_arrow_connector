package com.opcua_arrow.opcua.milo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opcua_arrow.opcua.retry.IRetryPolicy;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.typetree.DataTypeTreeBuilder;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class MiloOPCUADataType {

    private final IRetryPolicy retryPolicy;
    private final MiloOPCUAConnection connection;

    private volatile DataTypeTree dataTypeTree;

    public MiloOPCUADataType(MiloOPCUAConnection connection, IRetryPolicy retryPolicy) {
        this.connection = connection;
        this.retryPolicy = retryPolicy;
    }

    private DataTypeTree getOrBuildDataTypeTree() throws Exception {
        DataTypeTree local = dataTypeTree;
        if (local == null) {
            synchronized (this) {
                local = dataTypeTree;
                if (local == null) {
                    OpcUaClient client = connection.getClient();
                    if (client == null || !connection.isConnected()) {
                        throw new IllegalStateException("Client not connected");
                    }
                    dataTypeTree = local = DataTypeTreeBuilder.build(client);
                }
            }
        }
        return local;
    }

    public Map<String, Class<?>> getJavaTypes(List<String> nodeIds) throws Exception {
        OpcUaClient client = connection.getClient();
        if (client == null || !connection.isConnected()) {
            throw new IllegalStateException("Client not connected");
        }

        DataTypeTree tree = getOrBuildDataTypeTree();

        // montar leitura em batch do atributo DataType
        List<ReadValueId> readOps = new ArrayList<>();
        for (String id : nodeIds) {
            NodeId nid = NodeId.parse(id);
            readOps.add(new ReadValueId(
                    nid,
                    AttributeId.DataType.uid(),
                    null,
                    QualifiedName.NULL_VALUE));
        }

        // uma chamada só ao servidor
        DataValue[] values = client.read(
                0.0,
                TimestampsToReturn.Neither,
                readOps).getResults();

        Map<String, Class<?>> result = new LinkedHashMap<>();

        for (int i = 0; i < nodeIds.size(); i++) {
            DataValue dv = values[i];
            Object v = dv.getValue() != null ? dv.getValue().getValue() : null;

            Class<?> clazz = null;
            if (v instanceof NodeId dataTypeId) {
                clazz = tree.getBackingClass(dataTypeId);
            }

            result.put(nodeIds.get(i), clazz); // pode ser null se algo der errado
        }

        return result;
    }
}
