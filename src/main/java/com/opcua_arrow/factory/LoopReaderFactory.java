package com.opcua_arrow.factory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.read.LoopReader;

public class LoopReaderFactory {

    static IReader createLoopReader(
            long intervalSeconds,
            Map<Long, Set<String>> nodeIdsIntervalMap,
            IOPCUAReader<?> opcuaReader,
            BlockingQueue<List<IOPCUADataValue<?>>> queue) {

        return new LoopReader(intervalSeconds, nodeIdsIntervalMap, opcuaReader, queue);
    }
}
