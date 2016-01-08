package com.sas.unravl.generators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLRequestBodyGeneratorPlugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generates a text request body for this API call. The value associated with
 * "body" can have one of several forms:
 *
 * <pre>
 * { "text" : "request body string" }
 * { "text" : "@file-or-URL" }
 * { "text" : [ array-of-text-or-@file-or-URL ]
 * </pre>
 *
 * The request body is built as described in {@link Text}.
 * <p>
 * The text is bound to the current environment as a string named
 * <code>"requestBody"</code>.
 *
 * @author David.Biesack@sas.com
 *
 */
@UnRAVLRequestBodyGeneratorPlugin("text")
public class TextRequestBodyGenerator extends BaseUnRAVLRequestBodyGenerator {

    @Override
    public InputStream getBody(UnRAVL script, ObjectNode body, ApiCall call)
            throws IOException, UnRAVLException {
        JsonNode value = body.get("text");
        Text request = new Text(script, value);
        String requestBody = request.text();
        requestBody = script.expand(requestBody);
        script.bind("requestBody", requestBody);
        return new ByteArrayInputStream(Text.utf8(requestBody));
    }

}
