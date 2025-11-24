package com.opcua_arrow.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.opcua_arrow.config.OPCUAClientConfig;

public class PostgreSQLDataPointProvider implements IProvideDataPoint {

    private final DataSource dataSource;
    private final String sourceName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public PostgreSQLDataPointProvider(DataSource dataSource, String sourceName) {
        this.dataSource = dataSource;
        this.sourceName = sourceName;
    }

    @Override
    public OPCUAClientConfig getClientConfig() {
        String sql = "SELECT ps.source_config " +
                "FROM metadata.point_source ps " +
                "WHERE ps.name = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String configJson = rs.getString("source_config");
                return parseOPCUAClientConfig(configJson);
            } else {
                throw new RuntimeException("No configuration found for source: " + sourceName);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get client config", e);
        }
    }

    @Override
    public List<DataPointDTO> getDataPoints() {
        String sql = "SELECT " +
                "dpc.source_string as node_id, " +
                "dpc.source_id as point_id, " +
                "dp.data_type as value_type, " +
                "sg.name as group_name, " +
                "sg.partition_range as range_config, " +
                "dpc.filter_config, " +
                "pg.read_mode as read_type, " +
                "pg.interval as interval_seconds " +
                "FROM metadata.data_point_config dpc " +
                "JOIN metadata.data_point dp ON dpc.point_uuid = dp.point_uuid " +
                "JOIN metadata.priority_group pg ON dpc.priority_group = pg.priority_group_uuid " +
                "JOIN metadata.stream_group sg ON dpc.stream_group = sg.stream_group_uuid " +
                "JOIN metadata.data_block db ON dp.parent_block_id = db.block_uuid " +
                "JOIN metadata.point_source ps ON db.point_source_id = ps.group_id " +
                "WHERE ps.name = ? AND dpc.deleted_at IS NULL";

        return executeDataPointQuery(sql, sourceName);
    }

    @Override
    public List<DataPointDTO> getUpdatedDataPoints(Instant lastUpdate) {
        String sql = "SELECT " +
                "dpc.source_string as node_id, " +
                "dpc.source_id as point_id, " +
                "dp.data_type as value_type, " +
                "sg.name as group_name, " +
                "sg.partition_range as range_config, " +
                "dpc.filter_config, " +
                "pg.read_mode as read_type, " +
                "pg.interval as interval_seconds " +
                "FROM metadata.data_point_config dpc " +
                "JOIN metadata.data_point dp ON dpc.point_uuid = dp.point_uuid " +
                "JOIN metadata.priority_group pg ON dpc.priority_group = pg.priority_group_uuid " +
                "JOIN metadata.stream_group sg ON dpc.stream_group = sg.stream_group_uuid " +
                "JOIN metadata.data_block db ON dp.parent_block_id = db.block_uuid " +
                "JOIN metadata.point_source ps ON db.point_source_id = ps.group_id " +
                "WHERE ps.name = ? AND dpc.deleted_at IS NULL " +
                "AND dpc.updated_at > ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceName);
            stmt.setTimestamp(2, Timestamp.from(lastUpdate));

            return executeDataPointQuery(stmt);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get updated data points", e);
        }
    }

    @Override
    public List<String> getDeletedDataPoints(Instant lastUpdate) {
        String sql = "SELECT dpc.source_string as node_id " +
                "FROM metadata.data_point_config dpc " +
                "JOIN metadata.data_block db ON dpc.point_uuid = db.block_uuid " +
                "JOIN metadata.point_source ps ON db.point_source_id = ps.group_id " +
                "WHERE ps.name = ? AND dpc.deleted_at IS NOT NULL " +
                "AND dpc.deleted_at > ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceName);
            stmt.setTimestamp(2, Timestamp.from(lastUpdate));

            List<String> deletedNodeIds = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String nodeId = rs.getString("node_id");
                deletedNodeIds.add(nodeId);
            }

            return deletedNodeIds;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get deleted data points", e);
        }
    }

    private List<DataPointDTO> executeDataPointQuery(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return executeDataPointQuery(stmt);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute data point query", e);
        }
    }

    private List<DataPointDTO> executeDataPointQuery(PreparedStatement stmt) throws SQLException {
        List<DataPointDTO> dataPoints = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String nodeId = rs.getString("node_id");
            Integer pointId = rs.getInt("point_id");
            String valueType = rs.getString("value_type");
            String groupName = rs.getString("group_name");
            String rangeConfig = rs.getString("range_config");
            String filterConfigJson = rs.getString("filter_config");
            String readType = rs.getString("read_type");
            long intervalSeconds = rs.getLong("interval_seconds");

            // Parse range_config (PostgreSQL INT4RANGE format: [min,max])
            int[] range = parseRange(rangeConfig);
            int minRange = range[0];
            int maxRange = range[1];

            // Parse filter_config JSON
            Map<String, Object> filterConfig = parseFilterConfig(filterConfigJson);
            String filterType = (String) filterConfig.get("type");
            Map<String, Object> filterParameters = (Map<String, Object>) filterConfig.get("parameters");

            DataPointDTO dto = new DataPointDTO(
                    nodeId, valueType, pointId, groupName,
                    minRange, maxRange, filterType, filterParameters,
                    intervalSeconds, readType);

            dataPoints.add(dto);
        }

        return dataPoints;
    }

    private OPCUAClientConfig parseOPCUAClientConfig(String configJson) {
        try {
            Map<String, Object> config = objectMapper.readValue(configJson,
                    new TypeReference<Map<String, Object>>() {
                    });

            OPCUAClientConfig opcConfig = new OPCUAClientConfig();
            opcConfig.setServerUrl((String) config.get("serverUrl"));
            opcConfig.setUsername((String) config.get("username"));
            opcConfig.setPassword((String) config.get("password"));

            // Parse timeouts if present
            if (config.containsKey("requestTimeout")) {
                opcConfig.setRequestTimeout(java.time.Duration.ofSeconds(
                        ((Number) config.get("requestTimeout")).longValue()));
            }

            return opcConfig;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OPC UA client config", e);
        }
    }

    private Map<String, Object> parseFilterConfig(String filterConfigJson) {
        if (filterConfigJson == null || filterConfigJson.trim().isEmpty()) {
            return Map.of("type", "none", "parameters", Map.of());
        }

        try {
            return objectMapper.readValue(filterConfigJson,
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse filter config", e);
        }
    }

    private int[] parseRange(String rangeStr) {
        // Parse PostgreSQL INT4RANGE format: [min,max] or [min,max)
        if (rangeStr == null || rangeStr.trim().isEmpty()) {
            return new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
        }

        try {
            // Remove brackets and split
            String clean = rangeStr.replaceAll("[\\[\\]\\(\\)]", "");
            String[] parts = clean.split(",");

            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());

            return new int[] { min, max };

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse range: " + rangeStr, e);
        }
    }
}
