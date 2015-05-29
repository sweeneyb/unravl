package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;

import org.apache.log4j.Logger;

/**
 * A No-Op assertion. Useful to "comment out" an existing assertion in an
 * UnRAVL, since JSON does not support comment syntax. For example, if you have
 * the assertion
 * 
 * <pre>
 * "assert" : [
 *     { "json" : "@benchmark.json" },
 *     { "status" : 201 }
 *     ]
 * </pre>
 * 
 * and you wish to ignore the "json" assertion, but retain it for future use,
 * change it into an ignore:
 * 
 * <pre>
 * "assert" : [
 *     { "ignore" : { "json" : "@benchmark.json" } },
 *     { "status" : 201 }
 *     ]
 * </pre>
 * 
 * This may also be used as a "doc" element, to allow arbitrary JSON
 * documentation inside an "assert" or "preconditions":
 * 
 * <pre>
 * "assert" : [
 *     { "doc" : "Verify that the POST returns 201 Created status to indicate it successfully created a new resource." },
 *     { "status" : 201 }
 *     ]
 * </pre>
 * 
 * @author David.Biesack@sas.com
 *
 */
@UnRAVLAssertionPlugin({ "ignore", "doc" })
public class IgnoreAssertion extends BaseUnRAVLAssertion {

    private static final Logger logger = Logger
            .getLogger(IgnoreAssertion.class);

    @Override
    public void check(UnRAVL current, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(current, assertion, when, call);
        logger.info("ignoring assertion: " + assertion);
    }

}
