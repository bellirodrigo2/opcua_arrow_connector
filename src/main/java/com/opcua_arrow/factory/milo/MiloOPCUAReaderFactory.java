package com.opcua_arrow.factory.milo;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.opcua_arrow.config.OPCUAClientConfig;
import com.opcua_arrow.opcua.IOPCUAConnection;
import com.opcua_arrow.opcua.IOPCUADataValue;
import com.opcua_arrow.opcua.IOPCUAReader;
import com.opcua_arrow.opcua.milo.MiloOPCUAReader;
import com.opcua_arrow.retry.IRetryPolicy;

public class MiloOPCUAReaderFactory<T> {

    static <T> IOPCUAReader<T> createMiloOPCUAReader(OPCUAClientConfig config, IRetryPolicy retryPolicy,
            BlockingQueue<List<IOPCUADataValue<T>>> queue) {

        IOPCUAConnection connection = MiloOPCUAConnectionFactory.createMiloOPCUAConnection(config, retryPolicy);
        return new MiloOPCUAReader<T>(connection, retryPolicy, queue);
    }
}
