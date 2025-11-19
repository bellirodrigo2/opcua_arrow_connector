package com.opcua_arrow.collector;

import com.opcua_arrow.interfaces.IArrowAdapter;
import com.opcua_arrow.interfaces.IOPCUAReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Main collector that combines OPC-UA reader and Arrow adapter.
 * Reads data from OPC-UA server and converts it to Arrow IPC format.
 * 
 * @param <TValue> The type of scalar values
 */
public class OPCUAArrowCollector<TId, TValue> implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OPCUAArrowCollector.class);
    
    private final IOPCUAReader<TValue> opcuaReader;
    private final IArrowAdapter<TId, TValue> arrowAdapter;
    
    /**
     * Creates a new OPC-UA Arrow collector.
     * 
     * @param opcuaReader The OPC-UA reader implementation (with embedded connection)
     * @param arrowAdapter The Arrow adapter implementation
     */
    public OPCUAArrowCollector(IOPCUAReader<TValue> opcuaReader, IArrowAdapter<TId, TValue> arrowAdapter) {
        this.opcuaReader = opcuaReader;
        this.arrowAdapter = arrowAdapter;
    }
    
    /**
     * Starts the collector by connecting to the OPC-UA server and validating configured nodes.
     * 
     * @return A future that completes when started
     */
    public CompletableFuture<Void> start() {
        logger.info("Starting OPC-UA Arrow collector...");
        return opcuaReader.start();
    }
    
    /**
     * Collects data from OPC-UA server and converts it to Arrow IPC format.
     * 
     * @return A future containing the Arrow IPC data as bytes
     */
    public CompletableFuture<byte[]> collect() {
        logger.debug("Collecting data from OPC-UA server...");
        
        return opcuaReader.read()
            .thenApply(dataValues -> {
                logger.debug("Read {} data values from OPC-UA server", dataValues.size());
                byte[] arrowData = arrowAdapter.toArrowIPC(dataValues);
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
     * Checks if the collector is started and connected to the OPC-UA server.
     * 
     * @return true if started and connected, false otherwise
     */
    public boolean isStarted() {
        return opcuaReader.isStarted();
    }
    
    /**
     * Stops the collector by disconnecting from the OPC-UA server.
     * 
     * @return A future that completes when stopped
     */
    public CompletableFuture<Void> stop() {
        logger.info("Stopping OPC-UA Arrow collector...");
        return opcuaReader.stop();
    }
    
    @Override
    public void close() throws Exception {
        try {
            stop().get();
        } catch (Exception e) {
            logger.warn("Error during close: {}", e.getMessage());
        }
        
        if (opcuaReader instanceof AutoCloseable) {
            ((AutoCloseable) opcuaReader).close();
        }
        
        if (arrowAdapter instanceof AutoCloseable) {
            ((AutoCloseable) arrowAdapter).close();
        }
    }
    
    /**
     * Builder for creating OPCUAArrowCollector instances.
     * 
     * @param <TValue> The type of scalar values
     */
    public static class Builder<TId,TValue> {
        private IOPCUAReader<TValue> opcuaReader;
        private IArrowAdapter<TId,TValue> arrowAdapter;
        
        public Builder<TId,TValue> withOPCUAReader(IOPCUAReader<TValue> reader) {
            this.opcuaReader = reader;
            return this;
        }
        
        public Builder<TId,TValue> withArrowAdapter(IArrowAdapter<TId,TValue> adapter) {
            this.arrowAdapter = adapter;
            return this;
        }
        
        public OPCUAArrowCollector<TId,TValue> build() {
            if (opcuaReader == null) {
                throw new IllegalStateException("OPC-UA reader is required");
            }
            if (arrowAdapter == null) {
                throw new IllegalStateException("Arrow adapter is required");
            }
            
            return new OPCUAArrowCollector<>(opcuaReader, arrowAdapter);
        }
    }
}
