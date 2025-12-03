package com.opcua_arrow.batch_builder;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opcua_arrow.data.TSValue;

public class JsonBufferBuilder implements IBufferBuilder {

    private final String sourceName;
    private ObjectMapper mapper = new ObjectMapper();
    private List<String> buffer = new ArrayList<>();

    public JsonBufferBuilder(String sourceName) {
        this.sourceName = sourceName;
    }

    @Override
    public void appendList(List<TSValue> dataValues) {

        buffer.addAll(dataValues.stream().map(ts -> (String) ts.value).toList());
    }

    @Override
    public byte[] flush() {
        try {
            buffer.add(0, "{\"source\":\"" + sourceName + "\"}");
            return mapper.writeValueAsBytes(buffer);
        } catch (Exception e) {
            throw new RuntimeException("Error while serializing buffer to JSON", e);
        } finally {
            buffer.clear(); // importante: limpa o buffer
        }

    }

    @Override
    public void close() {
        buffer.clear();

    }

    public int size() {
        return buffer.stream().mapToInt(String::length).sum();
    }
}
