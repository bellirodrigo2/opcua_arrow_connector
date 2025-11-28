package com.opcua_arrow.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.opcua_arrow.context.Context;
import com.opcua_arrow.service.DataPointLoader;

/**
 * Classe principal para bootstrap da aplicação com Guice
 */
public class ApplicationBootstrap {

    public static void main(String[] args) {
        // Criar o injector com todos os módulos
        Injector injector = Guice.createInjector(new ApplicationModule());

        // Obter o contexto e o loader
        Context context = injector.getInstance(Context.class);

        DataPointLoader dataLoader = injector.getInstance(DataPointLoader.class);

        // Inicializar configuração de data points
        dataLoader.initialize();

        // Iniciar o processamento
        context.start();

        System.out.println("OPC UA Arrow Connector started successfully!");

        // Configurar shutdown hook para parar gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            context.stop();
            dataLoader.shutdown();
            System.out.println("Shutdown complete.");
        }));
    }
}
