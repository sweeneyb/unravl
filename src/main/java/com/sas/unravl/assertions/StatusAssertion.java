package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.util.Json;

/**
 * StatusAssertion asserts that the API call returned an HTTP status code that
 * matches the specification. There are three possible forms for this assertion:
 *
 * <pre>
 * { "status" : int }
 * { "status" : [ int, int, ..., int ] }
 * { "status" : "<em>pattern</em>" }
 * </pre>
 *
 * In the first form, the status must match the given int value exactly. <br>
 * Example: <code>{ "status" : 200 }</code>
 * <p>
 * In the second, the HTTP status code must match one of the int values. The int
 * values must be greater than or equal to 100 and less than 600. <br>
 * Example: <code>{ "status" : 200, 201, 204 }</code>
 * </p>
 * <p>
 * In the third, the string representation of the status code must regular
 * expression pattern. This is the "default" assertion. <br>
 * Example: <code>{ "status" : "2.." }</code>
 * </p>
 * <p>
 * If an UnRAVL script has no "status" assertion, an implicit assertion of <br>
 * <code>{ "status" : "2.." }</code><br>
 * is applied, which matches all 200-level status codes.
 * </p>
 *
 * @author David.Biesack@sas.com
 *
 */
@UnRAVLAssertionPlugin("status")
public class StatusAssertion extends BaseUnRAVLAssertion {

    @Override
    public void check(UnRAVL current, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(current, assertion, when, call);
        int httpStatus = call.getHttpStatus();
        boolean found = false;
        JsonNode statusCodes = Json.firstFieldValue(assertion);
        if (statusCodes.isTextual()) {
            found |= check(statusCodes.textValue(), httpStatus);
        } else if (statusCodes.isArray()) {
            for (JsonNode j : Json.array(statusCodes))
                found |= check(j, httpStatus);
        } else
            found = check(statusCodes, httpStatus);
        if (!found)
            throw new UnRAVLAssertionException("Actual HTTP status code "
                    + httpStatus + " did not match the expected status, "
                    + statusCodes);
        return;
    }

    private boolean check(String pattern, int httpStatus)
            throws UnRAVLAssertionException {
        return Integer.toString(httpStatus).matches(pattern);
    }

    private boolean check(JsonNode j, int httpStatus) throws UnRAVLException {
        if (j.isInt()) {
            int expected = j.intValue();
            if (j.intValue() >= 100 && j.intValue() < 600)
                return expected == httpStatus;
        }
        throw new UnRAVLException(
                "status assertion must use integer values between 100 and 599");
    }
}
