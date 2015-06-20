package com.sas.unravl.generators;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLRequestBodyGeneratorPlugin;
import com.sas.unravl.util.Json;

import java.io.IOException;
import java.io.InputStream;

/**
 * Generates a binary request body for this API call. The node bodySpec can have
 * one of several forms:
 *
 * <pre>
 * { "body" : [ array of byte values }
 * { "body" : "@file-or-URL" }
 * { "body" : [ array-of-binary-or-@file-or-URL ]
 * </pre>
 *
 * which are used to create a byte array which will be passed as the REST API
 * call's request body. The body is build as described in {@link Binary}.
 *
 * <p>
 * The resulting <code>byte[]</code> is bound to the current environment as
 * <code>"requestBody"</code>.
 *
 * @author David.Biesack@sas.com
 *
 */
@UnRAVLRequestBodyGeneratorPlugin("binary")
public class BinaryRequestBodyGenerator extends BaseUnRAVLRequestBodyGenerator {

    @Override
    public InputStream getBody(UnRAVL script, ObjectNode bodySpec, ApiCall call)
            throws IOException, UnRAVLException {
        Binary binary = new Binary(Json.object(bodySpec), "binary");
        byte requestBody[] = binary.bytes();
        script.bind("requestBody", requestBody);
        return binary.stream();
    }

}
