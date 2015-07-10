package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import java.io.IOException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.log4j.Logger;

/**
 * Run a JavaScript script, passing the current environment. If the script returns
 * false, the assertion fails. Ignore non-Boolean return values.
 * <p>
 * Usage:
 * This works just like {@link GroovyAssertion}
 * except it uses the keyword "javascript" and interprets with
 * the JavaScript script interpreter instead of the Groovy script interpreter.
 * 
 * @author David.Biesack@sas.com
 */
@UnRAVLAssertionPlugin({ "javascript", "JavaScript", "js" })
public class JavsScriptAssertion extends BaseUnRAVLAssertion {

    static final Logger logger = Logger.getLogger(JavsScriptAssertion.class);


    static {
        // TODO: import org.junit.Assert
    }

    @Override
    public void check(UnRAVL script, ObjectNode assertion, Stage when,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException {
        super.check(script, assertion, when, call);
        JsonNode g = null;
        String jsScript = null;
        try {
            g = Json.firstFieldValue(assertion);
            Text t = new Text(script, g);
            jsScript = script.expand(t.text());  
            ScriptEngine engine = script.getRuntime().getPlugins().interpreter(null);
            SimpleBindings bindings = new SimpleBindings(script.getRuntime().getBindings());
            engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            Object value = engine.eval(jsScript);
            logger.info("Script " + jsScript + ", returned " + value);
            if (value instanceof Boolean) {
                Boolean b = (Boolean) value;
                if (!b.booleanValue()) {
                    throw new UnRAVLAssertionException(
                            "cript returned false: " + jsScript);
                }
            }
        } catch (RuntimeException e) {
            logger.error("Script '" + jsScript
                    + "' threw a runtime exception " + e.getClass().getName()
                    + ", " + e.getMessage());
            throw new UnRAVLException(e.getMessage(), e);
        } catch (IOException e) {
            logger.error("I/O exception " + e.getMessage()
                    + " trying to load assertion '" + g);
            throw new UnRAVLException(e.getMessage(), e);
        } catch (ScriptException e) {
            logger.error("Script '" + jsScript
                    + "' threw a runtime exception " + e.getClass().getName()
                    + ", " + e.getMessage());
            throw new UnRAVLException(e.getMessage(), e);
        }
    }

}
