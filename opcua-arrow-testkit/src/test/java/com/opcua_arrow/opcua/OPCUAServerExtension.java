package com.opcua_arrow.opcua;

import com.opcua_arrow.server.ExampleServer;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class OPCUAServerExtension implements BeforeAllCallback, AfterAllCallback {

    private static ExampleServer server;
    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!started) {
            server = new ExampleServer();
            server.startup().get();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (server != null) {
                        server.shutdown().get();
                        System.out.println(">>> OPC-UA ExampleServer stopped (shutdown hook)");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            System.out.println(">>> OPC-UA ExampleServer started ONCE for entire test suite");
            started = true;
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // NÃO DESLIGUE AQUI — vários testes/classes ainda podem usar
        // O shutdown é garantido via JVM Shutdown Hook
        // (ou, se preferir, podemos desligar após a última classe — posso fazer isso
        // também)
    }

    public static ExampleServer getServer() {
        return server;
    }
}
