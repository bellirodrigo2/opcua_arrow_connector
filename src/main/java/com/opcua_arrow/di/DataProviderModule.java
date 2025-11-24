package com.opcua_arrow.di;

import javax.sql.DataSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.PostgreSQLConfig;
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

    @Provides
    @Singleton
    public String provideSourceName(PostgreSQLConfig config) {
        return config.getSourceName();
    }
}
