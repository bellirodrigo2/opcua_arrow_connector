package com.opcua_arrow.factory;

import java.util.Map;
import java.util.Set;

import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.read.IReader;
import com.opcua_arrow.read.LoopReader;

public class LoopReaderFactory {

    static IReader createLoopReader(
            long intervalSeconds,
            Map<Long, Set<String>> nodeIdsIntervalMap,
            IOPCUAReader<?> opcuaReader) {

        return new LoopReader(intervalSeconds, nodeIdsIntervalMap, opcuaReader);
    }
}
