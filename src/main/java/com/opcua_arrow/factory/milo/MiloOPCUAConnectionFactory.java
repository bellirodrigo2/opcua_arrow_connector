package com.opcua_arrow.factory.milo;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.milo.MiloOPCUAConnection;
import com.opcua_arrow.retry.IRetryPolicy;

public class MiloOPCUAConnectionFactory {

    static IOPCUAConnection createMiloOPCUAConnection(OPCUAClientConfig config, IRetryPolicy retryPolicy) {
        return new MiloOPCUAConnection(config, retryPolicy);
    }
}
