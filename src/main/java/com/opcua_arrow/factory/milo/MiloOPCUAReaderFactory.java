package com.opcua_arrow.factory.milo;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.opcua.milo.MiloOPCUAReader;
import com.opcua_arrow.retry.IRetryPolicy;
import com.opcua_arrow.retry.resilience4j.Resilience4jRetryPolicy;

public class MiloOPCUAReaderFactory<T> {

    static <T> IOPCUAReader<T> createMiloOPCUAReader(OPCUAClientConfig OPCUAClientConfig,
            RetryPolicyConfig retryConfig) {

        IRetryPolicy retryPolicy = new Resilience4jRetryPolicy(retryConfig);
        IOPCUAConnection connection = MiloOPCUAConnectionFactory.createMiloOPCUAConnection(
                OPCUAClientConfig, retryPolicy);
        return new MiloOPCUAReader<T>(connection, retryPolicy);
    }
}
