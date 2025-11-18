package com.opcua.arrow.adapter;

import com.opcua.arrow.interfaces.IArrowAdapter;
import com.opcua.arrow.interfaces.OPCUAValue;
import com.opcua.arrow.schema.SchemaUtils;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.pojo.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Implementation of IArrowAdapter that converts OPC-UA values to Arrow IPC format.
 * 
 * @param <T> The type of scalar values
 */
public class ArrowAdapter<T> implements IArrowAdapter<T>, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ArrowAdapter.class);
    
    private final List<String> nodeIds;
    private final Class<T> valueType;
    private final Map<String, Integer> idLookup;
    private final Schema schema;
    private final BufferAllocator allocator;
    
    public ArrowAdapter(List<String> nodeIds, Class<T> valueType, Map<String, Integer> idLookup) {
        this.nodeIds = nodeIds;
        this.valueType = valueType;
        this.idLookup = idLookup;
        this.schema = SchemaUtils.createSchema(valueType, idLookup);
        this.allocator = new RootAllocator(Long.MAX_VALUE);
    }
    
    @Override
    public byte[] toArrowIPC(List<OPCUAValue<T>> data) {
        if (data.size() != nodeIds.size()) {
            throw new IllegalArgumentException(
                String.format("Data size (%d) doesn't match node IDs size (%d)", 
                    data.size(), nodeIds.size()));
        }
        
        try (VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator)) {
            root.allocateNew();
            
            // Fill the vectors
            fillVectors(root, data);
            root.setRowCount(data.size());
            
            // Write to IPC format
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (WritableByteChannel channel = Channels.newChannel(out);
                 ArrowStreamWriter writer = new ArrowStreamWriter(root, null, channel)) {
                writer.start();
                writer.writeBatch();
                writer.end();
            }
            
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert to Arrow IPC", e);
        }
    }
    
    private void fillVectors(VectorSchemaRoot root, List<OPCUAValue<T>> data) {
        // Get vectors from root
        FieldVector pointIdVector = root.getVector("pointid");
        TimeStampNanoTZVector timestampVector = (TimeStampNanoTZVector) root.getVector("timestamp");
        FieldVector valueVector = root.getVector("value");
        IntVector statusCodeVector = (IntVector) root.getVector("statuscode");
        
        for (int i = 0; i < data.size(); i++) {
            OPCUAValue<T> opcValue = data.get(i);
            String nodeId = nodeIds.get(i);
            
            // Set pointid (either as integer or string)
            if (idLookup != null) {
                ((IntVector) pointIdVector).set(i, idLookup.get(nodeId));
            } else {
                ((VarCharVector) pointIdVector).set(i, nodeId.getBytes());
            }
            
            // Set timestamp (convert to nanoseconds since epoch)
            Instant timestamp = opcValue.getTimestamp();
            if (timestamp != null) {
                long nanos = timestamp.getEpochSecond() * 1_000_000_000L + timestamp.getNano();
                timestampVector.set(i, nanos);
            }
            
            // Set value
            T value = opcValue.getValue();
            if (value != null && !opcValue.isBad()) {
                setValueInVector(valueVector, i, value);
            } else {
                valueVector.setNull(i);
            }
            
            // Set status code
            statusCodeVector.set(i, opcValue.getStatusCode());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void setValueInVector(FieldVector vector, int index, T value) {
        if (value == null) {
            vector.setNull(index);
            return;
        }
        
        if (vector instanceof Float8Vector && value instanceof Double) {
            ((Float8Vector) vector).set(index, (Double) value);
        } else if (vector instanceof Float4Vector && value instanceof Float) {
            ((Float4Vector) vector).set(index, (Float) value);
        } else if (vector instanceof BitVector && value instanceof Boolean) {
            ((BitVector) vector).set(index, (Boolean) value ? 1 : 0);
        } else if (vector instanceof VarCharVector && value instanceof String) {
            ((VarCharVector) vector).set(index, ((String) value).getBytes());
        } else if (vector instanceof IntVector && value instanceof Integer) {
            ((IntVector) vector).set(index, (Integer) value);
        } else if (vector instanceof BigIntVector && value instanceof Long) {
            ((BigIntVector) vector).set(index, (Long) value);
        } else {
            logger.warn("Unsupported value type combination: {} for vector {}", 
                value.getClass(), vector.getClass());
            vector.setNull(index);
        }
    }
    
    @Override
    public Schema getSchema() {
        return schema;
    }
    
    @Override
    public void close() throws Exception {
        allocator.close();
    }
}
