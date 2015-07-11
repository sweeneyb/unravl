package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Run a script, passing the current environment. If the script returns false,
 * the assertion fails. Ignore non-Boolean return values. This base class for
 * {@link GroovyAssertion} and {@link JavaScriptAssertion}.
 * <p>
 * Usage:
 * 
 * <pre>
 * "assert" : [
 *     { "lang" : "<em>expression</em>" }
 *     { "lang" : "@file-or-URL" }
 *     { "lang" : [ array-of-text-or-@file-or-URL" }
 * ]
 * </pre>
 * 
 * where "lang" is a supported script language name such as "groovy" or
 * "javascript".
 * <p>
 * In the simplest form, all top level strings inside an "assert" or
 * "preconditions" array are interpreted as expressions in the target script
 * language.
 * <p>
 * If using a <code>{ "lang" : <em>value</em> }</code> object, the source
 * <code><em>value</em></code> may also be expressed as defined with
 * {@link Text}. The value may be a simple string (containing a expression), or
 * a string in the form <code>"@<em>file-or-URL</em>"</code> in which case the
 * script is ready from that location. Finally, the value may be an array of one
 * or more source strings or @file-or-URL references which are then concatenated
 * and then interpreted.
 * <p>
 * The final script value is subject to environment expansion before running.
 * All variables in the environment are available as local variables when the
 * script runs.
 * 
 * @author David.Biesack@sas.com
 */
public class BaseScriptAssertion extends BaseUnRAVLAssertion {

    static final Logger logger = Logger.getLogger(BaseScriptAssertion.class);

    private final String language;

    public BaseScriptAssertion(String language) {
        this.language = language;
    }

    @Override
    public void check(UnRAVL script, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(script, assertion, when, call);
        JsonNode val = null;
        String expression = null;
        try {
            val = Json.firstFieldValue(assertion);
            // TODO: If the value is an array, such as
            // { "assert" : { "javascript" : [ "line1", "line2", "line3" ] } }
            // processing as a Text object will concatenate these into
            // one String representing a single script, "line1\nline2\nline3".
            // UnRAVL needs a new option in the assert element to treat the
            // array
            // as separate/individual assertions, for example,
            // { "assert" : { "javascript" : [ "line1", "line2", "line3" ],
            // "separate" : true } }
            // which will run each array element as an assertion. For now, one
            // must
            // use the more verbose form:
            // { "assert" : { "javascript" : "line1"} }
            // { "assert" : { "javascript" : "line2"} }
            // { "assert" : { "javascript" : "line3"} }
            // I need a better name than "separate" : true ... ??

            Text t = new Text(script, val);
            expression = script.expand(t.text());
            Object result = script.evalWith(expression, language);
            logger.info("Script " + script + ", returned " + result);
            if ((result instanceof Boolean)
                    && !(((Boolean) result).booleanValue())) {
                throw new UnRAVLAssertionException(
                        "Groovy script returned false: " + script);
            }
        } catch (RuntimeException e) {
            logger.error("Script '" + script + "' threw a runtime exception "
                    + e.getClass().getName() + ", " + e.getMessage());
            throw new UnRAVLException(e.getMessage(), e);
        } catch (IOException e) {
            logger.error("I/O exception " + e.getMessage()
                    + " trying to load assertion '" + val);
            throw new UnRAVLException(e.getMessage(), e);
        }
    }

}
