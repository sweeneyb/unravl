package com.sas.unravl.generators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLRequestBodyGeneratorPlugin;
import com.sas.unravl.util.Json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map.Entry;

import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

/**
 * Generates a <code>application/x-www-form-urlencoded</code> request body for
 * this API call. The node can have one of several forms:
 * 
 * <pre>
 * { "form" : json-object }
 * { "form" : "@file-or-url" }
 * { "form" : "varName" }
 * { "form" : "name=encoded-&name=encoded-value&name=encoded-value" }
 * </pre>
 * <p>
 * In the first form the request body is derived from a JSON object. Each value
 * must have a scalar value (String, number, boolean). It is converted to
 * <code>application/x-www-form-urlencoded</code> by mapping each
 * <code>"name" : value</code> pair in the JSON object into a
 * "name=form-encoded-value" in the request body, separated by the
 * <code>'&amp;'</code> character.
 * </p>
 * <p>
 * In the second form, the JSON is first read from an external file or URL.
 * </p>
 * <p>
 * In the third form, <code>varName</code> must name a JSON object or a
 * java.util.Map object in the current environment; similar conversion to the
 * form body is performed.
 * </p>
 * <p>
 * The final form may be used to supply a literal pre-formatted body, using a
 * string.
 * </p>
 * <p>
 * Environment substitution is performed on string values in the input JSON.
 * </p>
 * <p>
 * The form body is bound in the current environment as a string named
 * "requestBody".
 * <p>
 * 
 * @author David.Biesack@sas.com
 * 
 */
@UnRAVLRequestBodyGeneratorPlugin("form")
public class FormBodyGenerator extends BaseUnRAVLRequestBodyGenerator {

    @Override
    public InputStream getBody(UnRAVL script, ObjectNode bodySpec, ApiCall call)
            throws IOException, UnRAVLException {
        super.getBody(script, bodySpec, call);
        JsonNode json = Json.firstFieldValue(bodySpec);
        ObjectNode inputJson = null;
        StringBuilder body = new StringBuilder();
        if (json.isTextual()) {
            String val = json.textValue(); // expand it? could be
                                           // "a={val1}&b={val2}" ?
            if (val.startsWith(UnRAVL.REDIRECT_PREFIX)) {
                Text request = new Text(script, json);
                // TODO: if can't parse, assume it is
                // application/x-www-form-urlencoded text
                json = Json.parse(request.text());
                inputJson = (ObjectNode) Json.expand(json, script);
                encode(inputJson, body);
            } else {
                Object ref = script.binding(val);
                if (ref instanceof ObjectNode) {
                    inputJson = (ObjectNode) Json.expand((ObjectNode) ref,
                            script);
                    encode(inputJson, body);
                } else if (val.contains("=")) {
                    // assume body is already application/x-www-form-urlencoded
                    // text
                    body.append(val);
                } else {
                    throw new UnRAVLException(
                            String.format(
                                    "Variable %s is not bound to a JSON value in 'body' body generator",
                                    val));
                }
            }
        } else if (json.isObject()) {
            inputJson = (ObjectNode) Json.expand((ObjectNode) json, script);
            encode(inputJson, body);
        } else {
            throw new UnRAVLException(String.format(
                    "Unrecognized value %s in 'body' body generator", json));
        }
        String bodyText = body.toString();
        script.bind("requestBody", bodyText);
        script.addRequestHeader(
                new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        return new ByteArrayInputStream(Text.utf8(bodyText));
    }

    private static final Logger logger = Logger
            .getLogger(FormBodyGenerator.class);

    private void encode(ObjectNode inputJson, StringBuilder body) {
        try {
            String delim = "";
            for (Entry<String, JsonNode> x : Json.fields(inputJson)) {
                body.append(delim).append(x.getKey()).append("=")
                        .append(URLEncoder.encode(x.getValue().asText(), "UTF-8"));
                delim = "&";
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        }
    }

}
