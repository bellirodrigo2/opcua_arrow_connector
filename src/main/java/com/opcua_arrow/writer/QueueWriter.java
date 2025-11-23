package com.opcua_arrow.writer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.batch_builder.IArrowBatchBuffer;
import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.data_point.opcua.DataValue;
import com.opcua_arrow.opcua.IOPCUADataValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueWriter implements IWriter {
    private static final Logger logger = LoggerFactory.getLogger(QueueWriter.class);
    private final BlockingQueue<Map<DataWriteGroup, List<DataValue<?>>>> source;
    private final ISend sender;
    private final long pollTimeoutSeconds;
    // IBufferBuilder bufferBuilder;
    private final Map<DataWriteGroup, IArrowBatchBuffer> batchBuffers;

    public QueueWriter(
            Map<DataWriteGroup, IArrowBatchBuffer> batchBuffers,
            BlockingQueue<Map<DataWriteGroup, List<DataValue<?>>>> source,
            long poll_timeout_seconds,
            ISend sender) {
        this.batchBuffers = batchBuffers;
        this.source = source;
        this.sender = sender;
        this.pollTimeoutSeconds = poll_timeout_seconds;
    }

    @Override
    public void write() {
        try {
            while (true) {
                Map<DataWriteGroup, List<DataValue<?>>> data = source.poll(pollTimeoutSeconds,
                        TimeUnit.SECONDS);
                if (data == null) {
                    logger.debug("No data to write, continuing...");
                    continue;
                }

                for (DataWriteGroup group : data.keySet()) {
                    List<DataValue<?>> nodeData = data.get(group);
                    if (!nodeData.isEmpty()) {
                        byte[] batch = buildBatch(group, nodeData);
                        if (batch != null && batch.length > 0) {
                            sender.send(group, batch);
                        }
                    }
                }
            }
        } catch (

        InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        for (IArrowBatchBuffer batchBuffer : batchBuffers.values()) {
            if (batchBuffer != null) {
                batchBuffer.close();
            }
        }
    }

    private byte[] buildBatch(DataWriteGroup group, List<DataValue<?>> nodeDataList) {

        IArrowBatchBuffer batchBuffer = batchBuffers.get(group);
        if (batchBuffer == null) {
            logger.warn("No batch buffer found for group: " + group);
            return null;
        }

        for (DataValue<?> dataValue : nodeDataList) {

            IOPCUADataValue<?> opcData = dataValue.getValue();

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
