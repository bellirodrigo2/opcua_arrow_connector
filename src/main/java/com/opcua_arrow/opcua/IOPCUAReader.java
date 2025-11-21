package com.opcua_arrow.opcua;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for OPC-UA reading operations.
 * Handles reading values from configured nodes using an encapsulated connection.
 *
 * @param <T> The type of values to read
 */
public interface IOPCUAReader<T> extends AutoCloseable {
    
    /**
     * Reads values from the configured nodes using the internal connection.
     * 
     * @return A future that completes when read
     * @throws IllegalStateException if the connection is not active
     */
    CompletableFuture<Void> read(List<String> nodeIds);
    
    /**
     * Starts the reader by connecting to the OPC-UA server and validating configured nodes.
     * 
     * @return A future that completes when started and nodes are validated
     */
    CompletableFuture<Void> start();
    
    /**
     * Stops the reader by disconnecting from the OPC-UA server.
     * 
     * @return A future that completes when stopped
     */
    CompletableFuture<Void> stop();
    
    /**
     * Checks if the reader is started and connected to the OPC-UA server.
     * 
     * @return true if started and connected, false otherwise
     */
    boolean isStarted();
}