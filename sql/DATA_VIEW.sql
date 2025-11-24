
-- View: Complete data point configuration with all joined data
CREATE OR REPLACE VIEW metadata.v_data_point_config AS
SELECT
    ps.name as source_name,
    dpc.source_string as node_id,
    dpc.source_id as point_id,
    dp.data_type as value_type,
    sg.name as group_name,
    sg.partition_range as range_config,
    fc.range as filter_range,
    fc.interval_seconds as filter_interval_seconds,
    pg.read_mode as read_type,
    pg.interval as interval_seconds,
    dpc.created_at,
    dpc.updated_at,
    dpc.deleted_at
FROM metadata.data_point_config dpc
JOIN metadata.data_point dp ON dpc.point_uuid = dp.point_uuid
JOIN metadata.priority_group pg ON dpc.priority_group = pg.priority_group_uuid
JOIN metadata.stream_group sg ON dpc.stream_group = sg.stream_group_uuid
JOIN metadata.data_block db ON dp.parent_block_id = db.block_uuid
JOIN metadata.point_source ps ON db.point_source_id = ps.group_id
LEFT JOIN metadata.filter_config fc ON dpc.filter_uuid = fc.filter_uuid;
