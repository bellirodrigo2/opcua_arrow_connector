CREATE SCHEMA metadata;


CREATE TABLE IF NOT EXISTS metadata.point_source(
    group_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT,
    description VARCHAR,
    namespace TEXT[],
    -- JSON config for OPC-UA source connector
    source_config JSONB
);

--Data Block table (similar to an OPC node, )
CREATE TABLE IF NOT EXISTS metadata.data_block (
    block_uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT,
    description VARCHAR,
    -- to be appended to block_groups
    namespace TEXT[], 
    point_source_id UUID REFERENCES metadata.point_source(group_id),

);
CREATE TYPE DataType AS ENUM (
    'Boolean',
    'Int16',
    'UInt16',
    'Int32',
    'UInt32',
    'Int64',
    'UInt64',
    'Float',
    'Double',
    'String'
);


--Data Point table (like a opc endpoint)
CREATE TABLE IF NOT EXISTS metadata.data_point (
    point_uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_block_id UUID REFERENCES metadata.data_block(block_id),
    name TEXT,
    description VARCHAR,
    data_type DataType,
    eng_units TEXT,
    zero DOUBLE PRECISION,
    span DOUBLE PRECISION
);

CREATE TYPE READMODE AS ENUM ('Read', 'Subscribe');

CREATE TABLE IF NOT EXISTS metadata.data_point_config(
    point_uuid UUID REFERENCES metadata.data_point(point_uuid),
    -- OPC-UA NodeId string
    source_string TEXT UNIQUE,
    -- Efficient integer mapping to final hypertables
    source_id INTEGER GENERATED ALWAYS AS IDENTITY UNIQUE,
    priority_group UUID REFERENCES metadata.priority_group(priority_group_uuid),
    stream_group UUID REFERENCES metadata.stream_group(stream_group_uuid),
    filter_config JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);



CREATE TABLE IF NOT EXISTS metadata.priority_group (
    priority_group_uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT UNIQUE,
    description VARCHAR,
    read_mode READMODE,
    interval BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION metadata.stream_group(
    stream_group_uuid UUID DEFAULT gen_random_uuid(),
    name TEXT,
    description VARCHAR,
    partition_range INT4RANGE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);