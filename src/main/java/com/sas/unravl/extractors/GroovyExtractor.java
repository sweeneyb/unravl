package com.sas.unravl.extractors;

import com.sas.unravl.annotations.UnRAVLExtractorPlugin;

/**
 * This extractor runs Groovy scripts and binds the result of each script to a
 * variable. See {@link BaseScriptExtractor} for details. This extractor uses
 * the "lang" value of "groovy" but "Groovy" is recognized as an alias:
 * 
 * <pre>
 * "bind" : [
 *    { "groovy" : { "r" : "json.a[2].getDoubleValue()" } }
 *    { "Groovy" : { "rsquared" : "r*r" } }
 *    ]
 * </pre>
 *
 * @author David.Biesack@sas.com
 */
@UnRAVLExtractorPlugin({ "groovy", "Groovy" })
public class GroovyExtractor extends BaseScriptExtractor {
    public GroovyExtractor() {
        super("groovy");
    }
}
