package com.sas.unravl.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * A base class for script-based extractors, such as {@link GrovyExtractor}
 * and {@link JavaScriptExractor}.
 * <p>
 * Usage:
 *
 * <pre>
 * "bind" : [
 *    { "lang" : { var-value-pairs } }
 *    ]
 * </pre>
 * where "lang" is a supported script language name such as "groovy" or "javascript".
 * <p>
 * The <var>var-value-pairs</var> are JSON object notation such as
 * <code>"varName" : "<em>expression</em>"</code>. Each
 * <code><em>expression</em></code> may be a string, a string in the form
 * "@file-or-URL", or an array of such source specifications, as defined in
 * {@link Text}. The resulting text is evaluated as a script in the
 * target language.
 * </p>
 * <p>
 * Expressions may use any variable binding that is currently in effect,
 * including preceding <var>var-value-pairs</var> in the current
 * scriptlet. Example (for Groovy)
 * </p>
 * 
 * <pre>
 * "bind" : [
 *    { "groovy" : { "pi" : "Math.PI",
 * 	                 "r" : "json.a[2].getDoubleValue()",
 * 	                 "pirsquared" : "pi*r*r",
 * 	                 "itemName" : "json.name.textValue()"
 * 	                }
 * 	  }
 * 	  ]
 * </pre>
 *
 * <p>
 * The script value is subject to environment expansion before
 * running. All variables in the environment are available as local variables
 * when the script runs.
 * </p>
 *
 * @author David.Biesack@sas.com
 */
public class BaseScriptExtractor extends BaseUnRAVLExtractor {

    private static final Logger logger = Logger
            .getLogger(BaseUnRAVLExtractor.class);

    private final String language;
    public BaseScriptExtractor(String language) {
        this.language = language;
    }
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
                if (!(sourceNode.isTextual() || sourceNode.isArray())) {
                    throw new UnRAVLException(
                            "Groovy extractor requires a string or array of strings, found "
                                    + sourceNode);
                }
                source = new Text(script, sourceNode).text();
                source = call.getScript().expand(source);
                Object value = script.evalWith(source, language);
                script.bind(name, value);

            } catch (RuntimeException rte) {
                logger.error("Script '" + sourceNode + "' (expansion '"
                        + source + "') threw a runtime exception "
                        + e.getClass().getName() + ", " + rte.getMessage());
                throw new UnRAVLException(rte.getMessage(), rte);
            } catch (IOException ioe) {
                logger.error("I/O exception " + ioe.getMessage()
                        + " reading script '" + sourceNode);
                throw new UnRAVLException(ioe.getMessage(), ioe);
            }
        }
    }
}
