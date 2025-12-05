package com.opcua_arrow.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.context.Context;
import com.opcua_arrow.data.BufferPackage;
import com.opcua_arrow.queues.IQueue;
import com.opcua_arrow.service.DataPointLoader;

/**
 * Classe principal para bootstrap da aplicação com Guice
 */
public class ApplicationBootstrap {

    public static void main(String[] args) {

        // Inicializar o ConfigProvider (pode ser ajustado para carregar de
        // arquivos/env)
        var configProvider = ConfigProvider.fromMaps(System.getenv());

        // Criar o injector com todos os módulos
        Injector injector = Guice.createInjector(new ApplicationModule(configProvider));

        // Obter o contexto e o loader
        Context context = injector.getInstance(Context.class);

        DataPointLoader dataLoader = injector.getInstance(DataPointLoader.class);

        // Inicializar configuração de data points
        dataLoader.initialize();

        // Iniciar o processamento
        context.start();

        IQueue<BufferPackage> queue = injector.getInstance(
                Key.get(new TypeLiteral<IQueue<BufferPackage>>() {
                }));

        System.out.println("OPC UA Arrow Connector started successfully!");

        // Configurar shutdown hook para parar gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            dataLoader.shutdown();
            context.stop();
            System.out.println("Shutdown complete.");
        }));
    }
}
