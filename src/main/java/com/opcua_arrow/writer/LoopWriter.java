package com.opcua_arrow.writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.opcua_arrow.batch_builder.IBufferBuilder;
import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.data_point.DataValue;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.queues.IQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopWriter implements IWriter {
    private static final Logger logger = LoggerFactory.getLogger(LoopWriter.class);
    private final IQueue<Map<DataWriteGroup, List<DataValue>>> source;
    private final ISend sender;
    private final Map<DataWriteGroup, IBufferBuilder> batchBuffers;

    private final List<Map<DataWriteGroup, List<DataValue>>> reusableDataList = new ArrayList<>();

    public LoopWriter(
            Map<DataWriteGroup, IBufferBuilder> batchBuffers,
            IQueue<Map<DataWriteGroup, List<DataValue>>> source,
            long poll_timeout_seconds,
            ISend sender) {
        this.batchBuffers = batchBuffers;
        this.source = source;
        this.sender = sender;
    }

    @Override
    public void write() {
        try {
            while (true) {
                for (Map<DataWriteGroup, List<DataValue>> data : reusableDataList) {
                    data.clear();
                }
                source.pop(reusableDataList);
                for (Map<DataWriteGroup, List<DataValue>> data : reusableDataList) {
                    internalWrite(data);
                }
            }
        } catch (

        InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void internalWrite(Map<DataWriteGroup, List<DataValue>> data) {
        for (DataWriteGroup group : data.keySet()) {
            List<DataValue> nodeData = data.get(group);
            if (!nodeData.isEmpty()) {
                byte[] batch = buildBatch(group, nodeData);
                if (batch != null && batch.length > 0) {
                    sender.send(group, batch);
                }
            }
        }
    }

    @Override
    public void stop() {
        for (IBufferBuilder batchBuffer : batchBuffers.values()) {
            if (batchBuffer != null) {
                batchBuffer.close();
            }
        }
    }

    private byte[] buildBatch(DataWriteGroup group, List<DataValue> nodeDataList) {

        IBufferBuilder batchBuffer = batchBuffers.get(group);
        if (batchBuffer == null) {
            logger.warn("No batch buffer found for group: " + group);
            return null;
        }

        for (DataValue dataValue : nodeDataList) {

            IOPCUADataValue opcData = dataValue.getValue();

            long timestamp = opcData.getTimestampLong();
            int id = dataValue.getParams().getPointId();

            // Get value (null if bad status)
            Object value = opcData.isGood() ? opcData.getValue() : null;

            int statusCode = opcData.getStatusCode();

            // Append to builder
            try {
                batchBuffer.append(id, timestamp, value, statusCode);
            } catch (Exception e) {
                logger.error("Failed to append data for group: {}, id: {}", group, id, e);
                // decidir se continua ou para
            }
        }

        return batchBuffer.flush();
    }

}
