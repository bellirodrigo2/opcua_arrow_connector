package com.opcua_arrow.read;

import java.util.List;

import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.TSValue;

/**
 * Interface for OPC-UA readers focused exclusively on reading operations.
 * DataPoint management is delegated to the task layer.
 */
public interface IReader extends AutoCloseable {

    /**
     * Reads values from the specified data points.
     *
     * @param dataPoints List of data points to read
     * @return List of read values
     */
    List<TSValue> read(List<DataPoint> dataPoints);

    /**
     * Starts the reader connection.
     *
     * @return
     */
    void start();

    /**
     * Stops the reader connection.
     *
     * @return
     */
    void stop();

    /**
     * Checks if the reader is started and connected.
     *
     * @return true if the reader is started, false otherwise
     */
    boolean isStarted();
}
