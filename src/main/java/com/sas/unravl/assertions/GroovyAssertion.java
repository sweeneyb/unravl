package com.sas.unravl.assertions;

import com.sas.unravl.annotations.UnRAVLAssertionPlugin;

/**
 * Run a Groovy script, passing the current environment. If the script returns
 * false, the assertion fails. Ignore non-Boolean return values.
 * <p>
 * See {@link BaseScriptAssertion} for the form and behavior; this instance uses
 * the "lang" value of "groovy".
 * 
 * @author David.Biesack@sas.com
 */
@UnRAVLAssertionPlugin({ "groovy", "Groovy" })
public class GroovyAssertion extends BaseScriptAssertion {

    public GroovyAssertion() {
        super("groovy");
    }
}
