package com.sas.unravl.assertions;

import static org.junit.Assert.assertArrayEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.generators.Binary;

import java.io.IOException;

/**
 * Asserts that the HTTP response body matches some text. There are several
 * forms for specifying the expected text response, as defined by {@link Binary}
 * .
 * 
 * @author David.Biesack@sas.com
 *
 */
@UnRAVLAssertionPlugin("binary")
public class BinaryBodyAssertion extends BaseUnRAVLAssertion implements
        UnRAVLAssertion {

    @Override
    public void check(UnRAVL current, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(current, assertion, when, call);
        JsonNode value = assertion.get("binary");
        try {
            Binary binary = new Binary(current, value);
            byte[] expected = binary.bytes();
            byte[] actual = call.getResponseBody().toByteArray();
            try {
                assertArrayEquals(expected, actual);
            } catch (AssertionError a) {
                throw new UnRAVLAssertionException(a.getMessage(), a);
            }
        } catch (IOException e1) {
            throw new UnRAVLException(e1.getMessage(), e1);
        }

    }

}
