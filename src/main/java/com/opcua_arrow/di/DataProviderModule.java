package com.opcua_arrow.di;

import javax.sql.DataSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
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
    public DataSource provideDataSource() {
        // TODO: Move to configuration
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/opcua_arrow");
        dataSource.setUser("postgres");
        dataSource.setPassword("password");
        return dataSource;
    }

    @Provides
    @Singleton  
    public String provideSourceName() {
        // TODO: Get from Kafka Connect configuration
        return "default_source";
    }
}