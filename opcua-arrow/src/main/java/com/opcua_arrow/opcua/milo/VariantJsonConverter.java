package com.opcua_arrow.opcua.milo;

import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.encoding.json.OpcUaJsonEncoder;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class VariantJsonConverter {

    public static String variantsToJson(EncodingContext context,
            Variant[] variants) throws Exception {

        OpcUaJsonEncoder encoder = new OpcUaJsonEncoder(context);

        encoder.encodeVariantArray("content", variants);
        String ouString = encoder.getOutputString();

        encoder.close();

        return ouString;
    }
}
