package com.opcua_arrow.opcua;

import java.util.List;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility for writing values to OPC-UA nodes.
 *
 * This class provides a simple interface for writing values to OPC-UA server nodes,
 * primarily used for testing subscription notifications by triggering data changes.
 */
public class OPCUAValueWriter {

    private static final Logger logger = LoggerFactory.getLogger(OPCUAValueWriter.class);

    private final OpcUaClient client;

    public OPCUAValueWriter(OpcUaClient client) {
        if (client == null) {
            throw new IllegalArgumentException("OpcUaClient cannot be null");
        }
        this.client = client;
    }

    /**
     * Write an integer value to the specified node.
     *
     * @param nodeIdString the node ID string (e.g., "ns=2;s=HelloWorld/ScalarTypes/Int32")
     * @param value the integer value to write
     * @throws Exception if the write operation fails
     */
    public void writeInt32(String nodeIdString, int value) throws Exception {
        NodeId nodeId = NodeId.parse(nodeIdString);
        DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);

        WriteValue writeValue = new WriteValue(
                nodeId,
                AttributeId.Value.uid(),
                null,
                dataValue);

        StatusCode[] results = client.write(List.of(writeValue)).getResults();

        if (results[0].isGood()) {
            logger.debug("Wrote Int32 value {} to node {}", value, nodeIdString);
        } else {
            throw new Exception("Write failed with status: " + results[0]);
        }
    }

    /**
     * Write a double value to the specified node.
     *
     * @param nodeIdString the node ID string
     * @param value the double value to write
     * @throws Exception if the write operation fails
     */
    public void writeDouble(String nodeIdString, double value) throws Exception {
        NodeId nodeId = NodeId.parse(nodeIdString);
        DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);

        WriteValue writeValue = new WriteValue(
                nodeId,
                AttributeId.Value.uid(),
                null,
                dataValue);

        StatusCode[] results = client.write(List.of(writeValue)).getResults();

        if (results[0].isGood()) {
            logger.debug("Wrote Double value {} to node {}", value, nodeIdString);
        } else {
            throw new Exception("Write failed with status: " + results[0]);
        }
    }

    /**
     * Write a boolean value to the specified node.
     *
     * @param nodeIdString the node ID string
     * @param value the boolean value to write
     * @throws Exception if the write operation fails
     */
    public void writeBoolean(String nodeIdString, boolean value) throws Exception {
        NodeId nodeId = NodeId.parse(nodeIdString);
        DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);

        WriteValue writeValue = new WriteValue(
                nodeId,
                AttributeId.Value.uid(),
                null,
                dataValue);

        StatusCode[] results = client.write(List.of(writeValue)).getResults();

        if (results[0].isGood()) {
            logger.debug("Wrote Boolean value {} to node {}", value, nodeIdString);
        } else {
            throw new Exception("Write failed with status: " + results[0]);
        }
    }

    /**
     * Write a string value to the specified node.
     *
     * @param nodeIdString the node ID string
     * @param value the string value to write
     * @throws Exception if the write operation fails
     */
    public void writeString(String nodeIdString, String value) throws Exception {
        NodeId nodeId = NodeId.parse(nodeIdString);
        DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);

        WriteValue writeValue = new WriteValue(
                nodeId,
                AttributeId.Value.uid(),
                null,
                dataValue);

        StatusCode[] results = client.write(List.of(writeValue)).getResults();

        if (results[0].isGood()) {
            logger.debug("Wrote String value '{}' to node {}", value, nodeIdString);
        } else {
            throw new Exception("Write failed with status: " + results[0]);
        }
    }

    /**
     * Write a float value to the specified node.
     *
     * @param nodeIdString the node ID string
     * @param value the float value to write
     * @throws Exception if the write operation fails
     */
    public void writeFloat(String nodeIdString, float value) throws Exception {
        NodeId nodeId = NodeId.parse(nodeIdString);
        DataValue dataValue = new DataValue(new Variant(value), StatusCode.GOOD, null);

        WriteValue writeValue = new WriteValue(
                nodeId,
                AttributeId.Value.uid(),
                null,
                dataValue);

        StatusCode[] results = client.write(List.of(writeValue)).getResults();

        if (results[0].isGood()) {
            logger.debug("Wrote Float value {} to node {}", value, nodeIdString);
        } else {
            throw new Exception("Write failed with status: " + results[0]);
        }
    }

    /**
     * Write a generic variant value to the specified node.
     *
     * @param nodeIdString the node ID string
     * @param variant the variant value to write
     * @throws Exception if the write operation fails
     */
    public void writeVariant(String nodeIdString, Variant variant) throws Exception {
        NodeId nodeId = NodeId.parse(nodeIdString);
        DataValue dataValue = new DataValue(variant, StatusCode.GOOD, null);

        WriteValue writeValue = new WriteValue(
                nodeId,
                AttributeId.Value.uid(),
                null,
                dataValue);

        StatusCode[] results = client.write(List.of(writeValue)).getResults();

        if (results[0].isGood()) {
            logger.debug("Wrote Variant value to node {}", nodeIdString);
        } else {
            throw new Exception("Write failed with status: " + results[0]);
        }
    }

    /**
     * Write a DataValue (including status and timestamps) to the specified node.
     *
     * @param nodeIdString the node ID string
     * @param dataValue the complete DataValue to write
     * @throws Exception if the write operation fails
     */
    public void writeDataValue(String nodeIdString, DataValue dataValue) throws Exception {
        NodeId nodeId = NodeId.parse(nodeIdString);

        WriteValue writeValue = new WriteValue(
                nodeId,
                AttributeId.Value.uid(),
                null,
                dataValue);

        StatusCode[] results = client.write(List.of(writeValue)).getResults();

        if (results[0].isGood()) {
            logger.debug("Wrote DataValue to node {}", nodeIdString);
        } else {
            throw new Exception("Write failed with status: " + results[0]);
        }
    }
}
