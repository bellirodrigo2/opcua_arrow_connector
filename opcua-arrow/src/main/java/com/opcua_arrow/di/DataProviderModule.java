package com.opcua_arrow.di;

import javax.sql.DataSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.IDataPointFactory;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.PostgreSQLConfig;
import com.opcua_arrow.data.DataPoint;
import com.opcua_arrow.data.DataReadGroup;
import com.opcua_arrow.data.DataWriteGroup;
import com.opcua_arrow.data.EDataType;
import com.opcua_arrow.data.EReadMode;
import com.opcua_arrow.data.IDataPointEqual;
import com.opcua_arrow.data.IntRange;
import com.opcua_arrow.data.equals.BaseEqualValue;
import com.opcua_arrow.data.equals.IsSameValue;
import com.opcua_arrow.data.equals.NoFilter;
import com.opcua_arrow.data.equals.RangeEqualValue;
import com.opcua_arrow.data.equals.StrictEqualBoolean;
import com.opcua_arrow.data.equals.StrictEqualBooleanArray;
import com.opcua_arrow.data.equals.StrictEqualDouble;
import com.opcua_arrow.data.equals.StrictEqualDoubleArray;
import com.opcua_arrow.data.equals.StrictEqualString;
import com.opcua_arrow.service.DataPointDTO;
import com.opcua_arrow.service.IProvideDataPoint;
import com.opcua_arrow.service.PostgreSQLDataPointProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataProviderModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(DataProviderModule.class);

    @Override
    protected void configure() {
        bind(IProvideDataPoint.class).to(PostgreSQLDataPointProvider.class);
    }

    @Provides
    @Singleton
    public ConfigProvider provideConfigProvider() {
        return new ConfigProvider();
    }

    @Provides
    @Singleton
    public PostgreSQLConfig providePostgreSQLConfig(ConfigProvider configProvider) {
        return configProvider.getPostgreSQLConfig();
    }

    @Provides
    public IDataPointFactory<DataPointDTO> factory() {
        return new DTOToDataPoint();
    }

    @Provides
    @Singleton
    public DataSource provideDataSource(PostgreSQLConfig config) {
        logger.info("Configuring HikariCP connection pool for PostgreSQL");

        HikariConfig hikariConfig = new HikariConfig();

        // Basic connection settings
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());

        // Connection pool settings
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinPoolSize());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout().toMillis());
        hikariConfig.setIdleTimeout(config.getIdleTimeout().toMillis());
        hikariConfig.setMaxLifetime(config.getMaxLifetime().toMillis());

        // Connection validation and reliability
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setKeepaliveTime(300000); // 5 minutes - sends test query to keep connection alive
        hikariConfig.setValidationTimeout(5000); // 5 seconds
        hikariConfig.setLeakDetectionThreshold(60000); // 1 minute - detect leaked connections

        // PostgreSQL specific settings
        hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");
        hikariConfig.addDataSourceProperty("socketTimeout", String.valueOf(config.getQueryTimeout().toSeconds()));
        hikariConfig.addDataSourceProperty("connectTimeout", String.valueOf(config.getConnectionTimeout().toSeconds()));
        hikariConfig.addDataSourceProperty("ApplicationName", "opcua-arrow-connector");

        // Transaction settings
        hikariConfig.setAutoCommit(config.isAutoCommit());
        hikariConfig.setTransactionIsolation("TRANSACTION_" + config.getTransactionIsolation());

        // Pool behavior
        hikariConfig.setPoolName("PostgreSQL-HikariPool");
        hikariConfig.setRegisterMbeans(true); // Enable JMX monitoring

        logger.info("HikariCP configured: maxPoolSize={}, minIdle={}, connectionTimeout={}ms",
                config.getMaxPoolSize(), config.getMinPoolSize(), config.getConnectionTimeout().toMillis());

        return new HikariDataSource(hikariConfig);
    }

    // @Provides
    // @Singleton
    // public String provideSourceName(PostgreSQLConfig config) {
    // return config.getSourceName();
    // }

    private class DTOToDataPoint implements IDataPointFactory<DataPointDTO> {

        @Override
        public DataPoint createDataPoint(DataPointDTO config) {

            String name = config.name;
            String description = config.description;
            String nodeId = config.nodeId;
            Integer pointId = config.pointId;
            EDataType dataType = getDataType(config.valueType);
            EReadMode readMode = EReadMode.valueOf(config.readType.toUpperCase());
            if (dataType == EDataType.EVENTS && readMode == EReadMode.EVENTS) {
                throw new IllegalArgumentException("DataPoint cannot have EVENTS data type and EVENTS read mode");
            }
            IDataPointEqual equals = createEquals(config.filterType, config.filterRange, config.filterIntervalSeconds,
                    dataType);
            DataWriteGroup group = createDataWriteGroup(dataType, config.minRange,
                    config.maxRange);
            DataReadGroup interval = createDataReadGroup(readMode, config.interval_seconds);

            return new DataPoint(name, description, nodeId, pointId, dataType, equals, group, interval);
        }

        private DataReadGroup createDataReadGroup(EReadMode readMode, long interval) {
            return new DataReadGroup(readMode, interval);
        }

        private IDataPointEqual createEquals(String filterType, double filterRange, long filterIntervalSeconds,
                EDataType dataType) {
            switch (filterType.toLowerCase()) {
                case "none":
                    return new NoFilter();
                case "equal":
                    return new BaseEqualValue(filterIntervalSeconds, createIsSameValue(dataType));
                case "range":
                    if (isNumeric(dataType)) {
                        return new BaseEqualValue(filterIntervalSeconds, new RangeEqualValue(filterRange));
                    } else {
                        logger.warn(
                                "Range filter is not applicable for non-numeric data types. Defaulting to StrictEqualValue.");
                        return new BaseEqualValue(filterIntervalSeconds, createIsSameValue(dataType));
                    }
                default:
                    throw new IllegalArgumentException("Unsupported filter type: " + filterType);
            }
        }

        private IsSameValue createIsSameValue(EDataType dataType) {
            switch (dataType) {
                case EDataType.BOOLEAN:
                    return new StrictEqualBoolean();
                case EDataType.STRING:
                    return new StrictEqualString();
                case EDataType.NUMERIC:
                    return new StrictEqualDouble();
                case EDataType.BOOLEAN_ARRAY:
                    return new StrictEqualBooleanArray();
                case EDataType.NUMERIC_ARRAY:
                    return new StrictEqualDoubleArray();
                default:
                    throw new IllegalArgumentException("IsSameValue not supported for data type: " + dataType);
            }
        }

        private boolean isNumeric(EDataType dataType) {
            return dataType == EDataType.NUMERIC;
        }

        private DataWriteGroup createDataWriteGroup(EDataType dataType, int minRange,
                int maxRange) {
            IntRange intRange = new IntRange(minRange, maxRange);
            return new DataWriteGroup(dataType, intRange);
        }

        private EDataType getDataType(String valueType) {
            return switch (valueType.toLowerCase()) {
                case "boolean" -> EDataType.BOOLEAN;
                case "string" -> EDataType.STRING;
                case "int16", "uint16", "int32", "uint32", "int64", "uint64", "float", "double" -> EDataType.NUMERIC;
                case "arrayboolean" -> EDataType.BOOLEAN_ARRAY;
                case "arraynumeric" -> EDataType.NUMERIC_ARRAY;
                default -> throw new IllegalArgumentException("Unsupported value type: " + valueType);
            };
        }
    }
}
