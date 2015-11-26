package com.sas.unravl.assertions;

import com.sas.unravl.annotations.UnRAVLAssertionPlugin;

/**
 * Run a JavaScript script, passing the current environment. If the script
 * returns false, the assertion fails. Ignore non-Boolean return values.
 * <p>
 * See {@link BaseScriptAssertion} for the form and behavior; this instance uses
 * the "lang" value of "javascript".
 * 
 * @author David.Biesack@sas.com
 */
@UnRAVLAssertionPlugin({ "javascript", "JavaScript", "js" })
public class JavaScriptAssertion extends BaseScriptAssertion {

    public JavaScriptAssertion() {
        super("javascript");
    }
}
