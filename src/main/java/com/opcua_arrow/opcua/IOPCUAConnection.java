package com.opcua_arrow.opcua;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;

/**
 * Interface for OPC-UA connection management.
 * Handles establishing and maintaining connections to OPC-UA servers.
 */
public interface IOPCUAConnection extends AutoCloseable {

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

    /**
     * Gets the server URL this connection manages.
     *
     * @return The OPC-UA server URL
     */
    String getServerUrl();

    /**
     * Gets the underlying OPC-UA client for reading operations.
     * This should only be used by readers that need direct access to the client.
     *
     * @return The OPC-UA client, or null if not connected
     */
    OpcUaClient getClient();

    /**
     * Gets the client lock for thread-safe operations.
     * This should be acquired by readers before using the client.
     *
     * @return The read-write lock for client access
     */
    ReadWriteLock getClientLock();
}
