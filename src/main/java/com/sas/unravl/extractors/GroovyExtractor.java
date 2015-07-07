package com.sas.unravl.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLExtractorPlugin;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import groovy.lang.GroovyShell;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This extractor runs Groovy scripts and binds the result of each script to a
 * variable. The scripts can use all defined variable bindings.
 * <p>
 * Usage:
 *
 * <pre>
 * "bind" : [
 *    { "groovy" : { var-value-pairs } }
 *    ]
 * </pre>
 * <p>
 * The <var>var-value-pairs</var> are JSON object notation such as
 * <code>"varName" : "<em>groovy-expression</em>"</code>. Each
 * <code><em>groovy-expression</em></code> may be a string, a string in the form
 * "@file-or-URL", or an array of such source specifications, as defined in
 * {@link Text}.
 * </p>
 * <p>
 * Expressions may use any variable binding that is currently in effect,
 * including preceding <var>var-value-pairs</var> in the current "groovy"
 * scriptlet. Example:
 * </p>
 * <pre>
 * "bind" : [
 *    { "groovy" : { "pi" : "Math.PI",
 * 	                 "r" : "json.a[2].getDoubleValue()",
 * 	                 "pirsquared" : "pi*r*r",
 * 	                 "itemName" : "json.name.textValue()"
 * 	                }
 * 	  }
 * 	  ],
 * </pre>
 *
 * <p>
 * The final Groovy script value is subject to environment expansion before
 * running. All variables in the environment are available as local variables
 * when the Groovy script runs.
 * </p>
 *
 * @author David.Biesack@sas.com
 */
@UnRAVLExtractorPlugin({ "groovy", "Groovy" })
public class GroovyExtractor extends BaseUnRAVLExtractor {

    private static final Logger logger = Logger
            .getLogger(BaseUnRAVLExtractor.class);

    @Override
    public void extract(UnRAVL script, ObjectNode scriptlet, ApiCall call)
            throws UnRAVLException {
        super.extract(script, scriptlet, call);
        ObjectNode bindings = Json.object(Json.firstFieldValue(scriptlet));
        for (Map.Entry<String, JsonNode> e : Json.fields(bindings)) {
            String source = null;
            JsonNode sourceNode = null;
            try {
                String name = e.getKey();
                sourceNode = e.getValue();
                if (! (sourceNode.isTextual() || sourceNode.isArray()) ) {
                    throw new UnRAVLException(
                            "Groovy extractor requires a string or array of strings, found "
                                    + sourceNode);
                }
                source = new Text(script, sourceNode).text();
                Object value = eval(call, source);
                script.bind(name, value);

            } catch (RuntimeException rte) {
                logger.error("Groovy script '" + sourceNode + "' (expansion '"
                        + source + "') threw a runtime exception "
                        + e.getClass().getName() + ", " + rte.getMessage());
                throw new UnRAVLException(rte.getMessage(), rte);
            } catch (IOException ioe) {
                logger.error("I/O exception " + ioe.getMessage()
                        + " reading Groovy script '" + sourceNode);
                throw new UnRAVLException(ioe.getMessage(), ioe);
            }
        }
    }

    /**
     * Evaluate script as a Groovy expression and return the result. Environment
     * variable expansion is performed on the input before evaluation.
     *
     * @param call
     *            the API call context
     * @param source
     *            Groovy script source
     * @return the value of the expression
     */
    public static Object eval(ApiCall call, String source) {
        String groovy = call.getScript().expand(source);
        // Create a new shell for each expression, so bindings can build
        // on previous bindings
        GroovyShell shell = new GroovyShell(call.getScript().getRuntime()
                .getBindings());
        Object value = shell.evaluate(groovy);
        return value;
    }
}
