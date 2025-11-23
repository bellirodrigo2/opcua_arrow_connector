package com.opcua_arrow.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.connector.ISend;
import com.opcua_arrow.data_point.DataWriteGroup;
import com.opcua_arrow.opcua.IOPCUAReader;

/**
 * Módulo temporário para bindings que ainda não existem no código
 */
public class SenderModule extends AbstractModule {

    @Override
    protected void configure() {
        // Pode adicionar bindings estáticos aqui se necessário
    }

    @Provides
    @Singleton
    public ISend provideMockSender() {
        return new ISend() {
            @Override
            public void send(DataWriteGroup group, byte[] data) {
                System.out.println("Mock ISend: Sending " + data.length + " bytes for group: " + group);
                // TODO: Substituir por implementação real (Kafka connector)
            }
        };
    }

    @Provides
    public IOPCUAReader<?> provideMockOPCUAReader() {
        // TODO: Substituir por implementação real baseada em configuração
        throw new UnsupportedOperationException(
                "IOPCUAReader provider não implementado. " +
                        "Implemente baseado na configuração da conexão OPC UA.");
    }
}