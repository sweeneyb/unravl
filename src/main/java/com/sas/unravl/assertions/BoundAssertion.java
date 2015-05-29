package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.util.Json;

/**
 * Asserts that one or more variables are bound in the current UnRAVL
 * environment. There are two possible forms for this assertion:
 * 
 * <pre>
 * { "bound" : var-name }
 * { "bound" : [ var-name, var-name, ..., var-name ] }
 * </pre>
 * 
 * In each form, <em>var-name</em> is a string that is the name of a variable.
 * If that variable is not bound, an assertion exception is thrown. The first
 * form is to check just one name. In the second, one or more variables names
 * may be tested.
 * 
 * @author David.Biesack@sas.com
 */
@UnRAVLAssertionPlugin("bound")
public class BoundAssertion extends BaseUnRAVLAssertion {

    @Override
    public void check(UnRAVL current, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(current, assertion, when, call);
        JsonNode vars = Json.firstFieldValue(assertion);
        if (vars.isArray()) {
            for (JsonNode var : Json.array(vars))
                check(var);
        } else
            check(vars);
        return;
    }

    private void check(JsonNode j) throws UnRAVLException {
        if (j.isTextual()) {
            String name = j.textValue();
            if (!getCall().isBound(name))
                throw new UnRAVLException("Value '" + name
                        + "' is not bound in the current script environment.");
        } else
            throw new UnRAVLException(
                    "values in a bound assertion must be strings.");
    }
}
