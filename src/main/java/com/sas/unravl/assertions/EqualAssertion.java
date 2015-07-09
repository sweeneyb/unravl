package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.util.Json;

import org.junit.Assert;

import org.apache.log4j.Logger;

/**
 * Asserts that two values are equal. There are two possible forms for this
 * assertion:
 * 
 * <pre>
 * { "equal" : [ lhs, rhs ] }
 * { "equal" : [ lhs, rhs, epsilon ] }
 * </pre>
 * 
 * The lhs and rhs values are compared and if not equal, the assertion throws an
 * UnRAVLAssertionException.
 * <p>
 * By convention, the lhs expression is the <em>expected value</em> and the rhs
 * is the <em>actual</em> value.
 * </p>
 * <p>
 * The values may be JSON null, booleans, integers, strings, doubles, JSON
 * arrays, or JSON objects. The values should be the same type. Environment
 * expansion is performed on all string values (top-level or nested inside JSON
 * arrays or objects), with the exception of JSON field names which are not
 * expanded. Note that this means the <em>string value</em> of variables will be
 * compared.
 * <p>
 * If the optional-epsilon value exists, it should be a floating point value and
 * the lhs and rhs values are compared as doubles and must be within epsilon of
 * each other. If the lhs or rhs values (after environment expansion) are string
 * values, the value is converted to a double via {@link Double#valueOf(String)}
 * 
 * @author David.Biesack@sas.com
 *
 */
@UnRAVLAssertionPlugin({ "equal", "equals" })
public class EqualAssertion extends BaseUnRAVLAssertion {

    private static final Logger logger = Logger.getLogger(EqualAssertion.class);

    @Override
    public void check(UnRAVL current, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(current, assertion, when, call);
        ArrayNode forms = assertionArray(assertion);
        if (forms.size() < 2 || forms.size() > 3)
            throw new UnRAVLException(
                    "value for equals must be an array of 2 or 3 values:"
                            + forms);
        Object lhs = val(forms.get(0));
        Object rhs = val(forms.get(1));
        if (forms.size() == 3) {
            Object eps = val(forms.get(2));
            double lhsd = doubleVal(lhs);
            double rhsd = doubleVal(rhs);
            double epsilon = doubleVal(eps);
            try {

                logger.info("equals " + lhsd + ", " + rhsd + " with epsilon "
                        + epsilon);
                Assert.assertEquals(lhsd, rhsd, epsilon);
            } catch (AssertionError e) {
                throw new UnRAVLAssertionException(
                        "Unequal numeric values in equals assertion, " + lhsd
                                + " != " + rhsd + " within epsilon " + epsilon);
            }

        } else {

            try {
                logger.info("equals " + lhs + ", " + rhs);
                Assert.assertEquals(lhs, rhs);
            } catch (AssertionError e) {
                throw new UnRAVLAssertionException(
                        "Unequal values in equals assertion, " + lhs + " != "
                                + rhs);
            }

        }
    }

    private ArrayNode assertionArray(ObjectNode assertion)
            throws UnRAVLException {
        return Json.array(Json.firstFieldValue(assertion));
    }

    private double doubleVal(Object o) throws UnRAVLException {
        double d;
        if (o instanceof String)
            d = Double.valueOf((String) o);
        else if (o instanceof Double)
            d = ((Double) o).doubleValue();
        else if (o instanceof Integer)
            d = ((Integer) o).doubleValue();
        else if (o instanceof Long)
            d = ((Long) o).doubleValue();
        else
            throw new UnRAVLException(
                    "equal assertion cannot perform epsilon numeric comparison with non-number value "
                            + o);
        return d;
    }

    private Object val(JsonNode node) {
        if (node.isNull())
            return null;
        if (node.isBoolean())
            return node.asBoolean();
        if (node.isInt())
            return node.asLong(); // into to long
        if (node.isLong())
            return node.asLong();
        if (node.isTextual())
            return getScript().expand(node.textValue());
        if (node.isDouble())
            return node.asDouble();
        return Json.expand(node, getScript());
    }

}
