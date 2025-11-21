package com.opcua_arrow.writer.arrow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.transform.IDataValue;
import com.opcua_arrow.writer.IArrowBatchBuffer;
import com.opcua_arrow.writer.IWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriterArrowAdapter implements IWriter {

    private static final Logger logger = LoggerFactory.getLogger(WriterArrowAdapter.class);
    private final Map<String, IArrowBatchBuffer> batchBuffers;
    private final BlockingQueue<Map<String, List<IDataValue<?>>>> source;
    private final ISend sender;

    private final long pollTimeoutSeconds;

    public WriterArrowAdapter(Map<String, IArrowBatchBuffer> batchBuffers,
            BlockingQueue<Map<String, List<IDataValue<?>>>> source,
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
                Map<String, List<IDataValue<?>>> data = source.poll(pollTimeoutSeconds, TimeUnit.SECONDS);
                if (data == null) {
                    logger.debug("No data to write, continuing...");
                    continue;
                }

                for (String group : data.keySet()) {
                    List<IDataValue<?>> nodeData = data.get(group);
                    if (!nodeData.isEmpty()) {
                        ingestData(group, nodeData);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void ingestData(String group, List<IDataValue<?>> nodeData) {

        IArrowBatchBuffer batchBuffer = batchBuffers.get(group);
        if (batchBuffer == null) {
            logger.warn("No batch buffer found for group: " + group);
            return;
        }

        for (IDataValue<?> dataValue : nodeData) {

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

        // flush
        byte[] arrowData = batchBuffer.flush();
        if (arrowData != null) {
            try {
                sender.send(group, arrowData);
            } catch (Exception e) {
                logger.error("Failed to send data for group: {}", group, e);
                // decidir se continua ou para
            }
        }
    }

    // private void groupByNodeId(List<IDataValue<?>> data) {
    // for (List<IDataValue<?>> list : groupedData.values()) {
    // list.clear();
    // }

    // for (IDataValue<?> dataValue : data) {
    // String nodeId = dataValue.getParams().getGroup();
    // groupedData.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(dataValue);
    // }
    // }

}
