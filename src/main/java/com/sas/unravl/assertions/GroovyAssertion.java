package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * Run a Groovy script, passing the current environment. If the script returns
 * false, the assertion fails. Ignore non-Boolean return values.
 * <p>
 * Usage:
 * 
 * <pre>
 * "assert" : [ "<em>groovy-expression</em>", ..., "<em>groovy-expression</em>" ]
 * "assert" : [
 *     { "groovy" : "<em>groovy-expression</em>" }
 *     { "groovy" : "@file-or-URL" }
 *     { "groovy" : [ array-of-text-or-@file-or-URL" }
 * ]
 * </pre>
 * 
 * In the simplest form, all top level strings inside an "assert" or
 * "preconditions" array are interpreted as Groovy expressions.
 * <p>
 * If using a <code>{ "groovy" : <em>value</em> }</code> object, the the Groovy
 * source <code><em>value</em></code> may also be expressed as defined with
 * {@link Text}. The value may be a simple string (containing a Groovy
 * expression), or a string in the form <code>"@<em>file-or-URL</em>"</code> in
 * which case the Groovy is ready from that location. Finally, the value may be
 * an array of one or more source strings or @file-or-URL references which are
 * then concatenated and then interpreted.
 * <p>
 * The final Groovy script value is subject to environment expansion before
 * running. All variables in the environment are available as local variables
 * when the Groovy script runs.
 * 
 * @author David.Biesack@sas.com
 */
@UnRAVLAssertionPlugin({ "groovy", "Groovy" })
public class GroovyAssertion extends BaseUnRAVLAssertion {

    static final Logger logger = Logger.getLogger(GroovyAssertion.class);

    static final CompilerConfiguration configuration = new CompilerConfiguration();

    static {
        ImportCustomizer iczr = new ImportCustomizer();
        iczr.addStaticStars("org.junit.Assert"); // add static imports
                                                 // org.junit.Assert.*
        configuration.addCompilationCustomizers(iczr);
    }

    @Override
    public void check(UnRAVL script, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(script, assertion, when, call);
        JsonNode g = null;
        String groovyScript = null;
        try {
            g = Json.firstFieldValue(assertion);
            Text t = new Text(script, g);
            groovyScript = script.expand(t.text());
            Binding bindings = bindings(script.getRuntime());
            GroovyShell shell = new GroovyShell(bindings, configuration);
            Object value = shell.evaluate(groovyScript);
            logger.info("Groovy script " + groovyScript + ", returned " + value);
            if (value instanceof Boolean) {
                Boolean b = (Boolean) value;
                if (!b.booleanValue()) {
                    throw new UnRAVLAssertionException(
                            "Groovy script returned false: " + groovyScript);
                }
            }
        } catch (RuntimeException e) {
            logger.error("Groovy script '" + groovyScript
                    + "' threw a runtime exception " + e.getClass().getName()
                    + ", " + e.getMessage());
            throw new UnRAVLException(e.getMessage(), e);
        } catch (IOException e) {
            logger.error("I/O exception " + e.getMessage()
                    + " trying to load Groovy assertion '" + g);
            throw new UnRAVLException(e.getMessage(), e);
        }
    }

    public static Binding bindings(UnRAVLRuntime runtime) {
        return new Binding(runtime.getBindings());
    }
}
