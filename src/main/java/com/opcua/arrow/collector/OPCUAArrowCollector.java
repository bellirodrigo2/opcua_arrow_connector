package com.opcua.arrow.collector;

import com.opcua.arrow.interfaces.IArrowAdapter;
import com.opcua.arrow.interfaces.IOPCUAClient;
import com.opcua.arrow.interfaces.OPCUAValue;
import org.apache.arrow.vector.types.pojo.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main collector that combines OPC-UA client and Arrow adapter.
 * Reads data from OPC-UA server and converts it to Arrow IPC format.
 * 
 * @param <T> The type of scalar values
 */
public class OPCUAArrowCollector<T> implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OPCUAArrowCollector.class);
    
    private final IOPCUAClient<T> opcuaClient;
    private final IArrowAdapter<T> arrowAdapter;
    
    /**
     * Creates a new OPC-UA Arrow collector.
     * 
     * @param opcuaClient The OPC-UA client implementation
     * @param arrowAdapter The Arrow adapter implementation
     */
    public OPCUAArrowCollector(IOPCUAClient<T> opcuaClient, IArrowAdapter<T> arrowAdapter) {
        this.opcuaClient = opcuaClient;
        this.arrowAdapter = arrowAdapter;
    }
    
    /**
     * Connects to the OPC-UA server.
     * 
     * @return A future that completes when connected
     */
    public CompletableFuture<Void> connect() {
        logger.info("Connecting to OPC-UA server...");
        return opcuaClient.connect();
    }
    
    /**
     * Collects data from OPC-UA server and converts it to Arrow IPC format.
     * 
     * @return A future containing the Arrow IPC data as bytes
     */
    public CompletableFuture<byte[]> collect() {
        logger.debug("Collecting data from OPC-UA server...");
        
        return opcuaClient.read()
            .thenApply(data -> {
                logger.debug("Read {} values from OPC-UA server", data.size());
                byte[] arrowData = arrowAdapter.toArrowIPC(data);
                logger.debug("Converted to Arrow IPC format ({} bytes)", arrowData.length);
                return arrowData;
            })
            .exceptionally(throwable -> {
                logger.error("Failed to collect data", throwable);
                throw new RuntimeException("Collection failed", throwable);
            });
    }
    
    /**
     * Collects data synchronously.
     * 
     * @return The Arrow IPC data as bytes
     * @throws Exception if collection fails
     */
    public byte[] collectSync() throws Exception {
        return collect().get();
    }
    
    /**
     * Gets the Arrow schema.
     * 
     * @return The Arrow schema
     */
    public Schema getSchema() {
        return arrowAdapter.getSchema();
    }
    
    /**
     * Checks if the collector is connected to the OPC-UA server.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return opcuaClient.isConnected();
    }
    
    /**
     * Disconnects from the OPC-UA server.
     * 
     * @return A future that completes when disconnected
     */
    public CompletableFuture<Void> disconnect() {
        logger.info("Disconnecting from OPC-UA server...");
        return opcuaClient.disconnect();
    }
    
    @Override
    public void close() throws Exception {
        try {
            disconnect().get();
        } catch (Exception e) {
            logger.warn("Error during close: {}", e.getMessage());
        }
        
        if (opcuaClient instanceof AutoCloseable) {
            ((AutoCloseable) opcuaClient).close();
        }
        
        if (arrowAdapter instanceof AutoCloseable) {
            ((AutoCloseable) arrowAdapter).close();
        }
    }
    
    /**
     * Builder for creating OPCUAArrowCollector instances.
     * 
     * @param <T> The type of scalar values
     */
    public static class Builder<T> {
        private IOPCUAClient<T> opcuaClient;
        private IArrowAdapter<T> arrowAdapter;
        
        public Builder<T> withOPCUAClient(IOPCUAClient<T> client) {
            this.opcuaClient = client;
            return this;
        }
        
        public Builder<T> withArrowAdapter(IArrowAdapter<T> adapter) {
            this.arrowAdapter = adapter;
            return this;
        }
        
        public OPCUAArrowCollector<T> build() {
            if (opcuaClient == null) {
                throw new IllegalStateException("OPC-UA client is required");
            }
            if (arrowAdapter == null) {
                throw new IllegalStateException("Arrow adapter is required");
            }
            
            return new OPCUAArrowCollector<>(opcuaClient, arrowAdapter);
        }
    }
}
