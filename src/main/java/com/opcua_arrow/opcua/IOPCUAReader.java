package com.opcua_arrow.opcua;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for OPC-UA reading operations.
 * Handles reading values from configured nodes using an encapsulated
 * connection.
 *
 * @param
 */
public interface IOPCUAReader extends AutoCloseable {

    /**
     * Gets the list of configured node IDs to read from.
     *
     * @return List of node IDs
     */
    List<String> getNodeIds();

    /**
     * Sets the list of node IDs to read from.
     *
     * @param nodeIds List of node IDs
     */
    void setNodeIds(List<String> nodeIds);

    /**
     * Adds a node ID to the list of nodes to read from.
     *
     * @param nodeId The node ID to add
     */
    void addNodeId(String nodeId);

    /**
     * Removes a node ID from the list of nodes to read from.
     *
     * @param nodeId The node ID to remove
     */
    void removeNodeId(String nodeId);

    /**
     * Reads values from the configured nodes using the internal connection.
     *
     * @return A future that completes when read
     * @throws IllegalStateException if the connection is not active
     */
    CompletableFuture<List<IOPCUADataValue>> read();

    /**
     * Starts the reader by connecting to the OPC-UA server and validating
     * configured nodes.
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
