package com.opcua_arrow.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.transform.IDataValue;
import com.opcua_arrow.writer.IArrowBatchBuffer;
import com.opcua_arrow.writer.arrow.WriterArrowAdapter;

public class ArrowWriterFactory {

    static WriterArrowAdapter createArrowWriterAdapter(
            // Map<String, IArrowBatchBuffer> batchBuffers,
            int initialCapacity,
            boolean compress,
            Map<String, String> nodeIdTypesMap,
            BlockingQueue<Map<String, List<IDataValue<?>>>> source,
            long poll_timeout_seconds,
            ISend sender) {

        Map<String, IArrowBatchBuffer> batchBuffers = createBuffersMap(nodeIdTypesMap, initialCapacity, compress);
        return new WriterArrowAdapter(batchBuffers, source, poll_timeout_seconds, sender);
    }

    static Map<String, IArrowBatchBuffer> createBuffersMap(Map<String, String> nodeIdTypesMap, int initialCapacity,
            boolean compress) {
        Map<String, IArrowBatchBuffer> batchBuffers = new HashMap<>();

        return batchBuffers;
    }

}