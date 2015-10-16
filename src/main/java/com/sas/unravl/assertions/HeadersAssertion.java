package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.util.Json;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.Header;
import org.apache.log4j.Logger;

/**
 * Asserts that HTTP response headers exists and that it matches a regular
 * expression pattern.
 * 
 * <pre>
 * { "headers" : { header : pattern } }
 * </pre>
 * 
 * HTTP header case is ignored (Content-Type and content-type are the same
 * header), but by convention headers are coded in Initial-Caps-With-Hyphens
 * format. The pattern is a Java regular expression pattern as in
 * {@link Pattern}.
 * 
 * @author David.Biesack@sas.com
 */
@UnRAVLAssertionPlugin({ "headers", "header" })
public class HeadersAssertion extends BaseUnRAVLAssertion {

    static final Logger logger = Logger.getLogger(HeadersAssertion.class);

    @Override
    public void check(UnRAVL current, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(current, assertion, when, call);
        Header headers[] = call.getResponseHeaders();
        JsonNode spec = assertion.get("headers");
        check(spec, headers, current);
        return;
    }

    private void check(JsonNode headerNode, Header[] headers, UnRAVL current)
            throws UnRAVLException {
        for (Map.Entry<String, JsonNode> next : Json.fields(headerNode)) {
            String header = next.getKey();
            JsonNode valNode = next.getValue();
            if (!valNode.isTextual())
                throw new UnRAVLException("header value " + valNode
                        + " is not a string (regular expression expected)");
            String pattern = current.expand(valNode.textValue());
            Header h = findHeader(header, headers);
            try {
                Matcher matcher = Pattern.compile(pattern)
                        .matcher(h.getValue());
                if (!matcher.matches())
                    throw new UnRAVLAssertionException("header " + header
                            + " does not match required pattern " + pattern);
                else {
                    logger.trace("header " + header
                            + " matches required pattern " + pattern);
                }
            } catch (PatternSyntaxException e) {
                throw new UnRAVLException(
                        "Invalid header pattern regular expression, " + pattern);
            }

        }

    }

    private Header findHeader(String header, Header[] headers)
            throws UnRAVLAssertionException {
        for (Header h : headers)
            if (h.getName().equalsIgnoreCase(header))
                return h;
        throw new UnRAVLAssertionException("Required header " + header
                + " not found. Existing headers:" + Arrays.asList(headers));
    }
}
