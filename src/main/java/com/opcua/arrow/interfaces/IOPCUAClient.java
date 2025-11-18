package com.opcua.arrow.interfaces;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for OPC-UA client implementations.
 *
 * @param <T> The type of values to read
 */
public interface IOPCUAClient<T> extends AutoCloseable {
    
    /**
     * Reads values from the configured nodes.
     * 
     * @return A future containing the list of read values
     */
    CompletableFuture<List<OPCUAValue<T>>> read();
    
    /**
     * Connects to the OPC-UA server.
     * 
     * @return A future that completes when connected
     */
    CompletableFuture<Void> connect();
    
    /**
     * Disconnects from the OPC-UA server.
     * 
     * @return A future that completes when disconnected
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * Checks if the client is connected.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
}
