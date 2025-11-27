package com.opcua_arrow.batch_builder;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opcua_arrow.data.TSValue;

public class JsonBufferBuilder implements IBufferBuilder {

    private ObjectMapper mapper = new ObjectMapper();
    private List<TSValue> buffer;

    @Override
    public void appendList(List<TSValue> dataValues) {

        buffer.addAll(dataValues);
    }

    @Override
    public byte[] flush() {
        try {
            // Extrai apenas os values
            List<Object> values = buffer.stream()
                    .map(ts -> ts.value)
                    .toList();

            // Converte para JSON e retorna como byte[]
            return mapper.writeValueAsBytes(values);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao serializar buffer para JSON", e);
        } finally {
            buffer.clear(); // importante: limpa o buffer
        }

    }

    @Override
    public void close() {
        buffer.clear();

    }
}
