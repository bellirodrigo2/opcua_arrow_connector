package com.opcua_arrow.di;

import javax.sql.DataSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opcua_arrow.config.ConfigProvider;
import com.opcua_arrow.config.PostgreSQLConfig;
import com.opcua_arrow.data_point_provider.IProvideDataPoint;
import com.opcua_arrow.data_point_provider.PostgreSQLDataPointProvider;
import org.postgresql.ds.PGSimpleDataSource;

public class DataProviderModule extends AbstractModule {

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
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(config.getJdbcUrl());
        dataSource.setUser(config.getUsername());
        dataSource.setPassword(config.getPassword());
        
        // Connection settings
        dataSource.setConnectTimeout((int) config.getConnectionTimeout().toSeconds());
        dataSource.setSocketTimeout((int) config.getQueryTimeout().toSeconds());
        dataSource.setApplicationName("opcua-arrow-connector");
        
        return dataSource;
    }

    @Provides
    @Singleton  
    public String provideSourceName(PostgreSQLConfig config) {
        return config.getSourceName();
    }
}