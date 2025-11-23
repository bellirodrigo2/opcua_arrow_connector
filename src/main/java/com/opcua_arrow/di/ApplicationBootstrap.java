package com.opcua_arrow.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.opcua_arrow.context.Context;

/**
 * Classe principal para bootstrap da aplicação com Guice
 */
public class ApplicationBootstrap {

    public static void main(String[] args) {
        // Criar o injector com todos os módulos
        Injector injector = Guice.createInjector(new ApplicationModule());

        // Obter a factory do contexto
        ContextFactory contextFactory = injector.getInstance(ContextFactory.class);

        // Criar o contexto com todas as dependências injetadas
        Context context = contextFactory.createContext();

        // Iniciar o processamento
        context.start();

        // Configurar shutdown hook para parar gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            context.stop();
        }));
    }
}