package com.opcua_arrow.opcua;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Callable;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.opcua.milo.MiloOPCUAConnection;
import com.opcua_arrow.opcua.retry.IRetryPolicy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(OPCUAServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MiloOPCUAConnectionTest {

    private MiloOPCUAConnection connection;

    @BeforeAll
    void setup() throws Exception {
        var config = new OPCUAClientConfig();
        config.setServerUrl("opc.tcp://localhost:12686/milo");
        config.setRequestTimeout(Duration.ofSeconds(5));
        config.setSessionTimeout(Duration.ofSeconds(10));
        config.setKeepAliveInterval(Duration.ofSeconds(3));
        config.setUsername(null);
        config.setPassword(null);

        // 🔥 Retry síncrono (compatível com nova interface)
        IRetryPolicy retry = new IRetryPolicy() {
            private final int maxAttempts = 3;

            @Override
            public <T> T executeWithRetry(Callable<T> action) {
                int attempt = 1;
                while (true) {
                    try {
                        return action.call();
                    } catch (Exception ex) {
                        if (attempt >= maxAttempts) {
                            throw new RuntimeException(ex);
                        }
                        attempt++;
                    }
                }
            }

            @Override
            public int getMaxAttempts() {
                return maxAttempts;
            }
        };

        connection = new MiloOPCUAConnection(config, retry);

        // connect() ainda retorna CompletableFuture → só aguardar
        connection.connect().get();
    }

    @AfterAll
    void cleanup() throws Exception {
        if (connection != null) {
            connection.disconnect().get();
        }
    }

    @Test
    void testConnectionIsConnected() {
        assertTrue(connection.isConnected());
        assertNotNull(connection.getClient());
    }

    @Test
    void testDisconnect() throws Exception {
        connection.disconnect().get();
        assertFalse(connection.isConnected());

        // connect again
        connection.connect().get();
        assertTrue(connection.isConnected());
    }

    @Test
    void testReadWriteLockExists() {
        assertNotNull(connection.getClientLock());
        assertNotNull(connection.getClientLock().readLock());
    }
}
