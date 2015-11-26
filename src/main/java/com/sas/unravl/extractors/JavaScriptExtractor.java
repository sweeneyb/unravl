package com.sas.unravl.extractors;

import com.sas.unravl.annotations.UnRAVLExtractorPlugin;

/**
 * This extractor runs JavaScript scripts and binds the result of each script to
 * a variable. See {@link BaseScriptExtractor} for details. This extractor uses
 * the "lang" value of "javascript" but "JavaScript" and "js" are recognized as
 * aliases:
 * 
 * <pre>
 * "bind" : [
 *    { "javascript" : { "r" : "json.a[2].getDoubleValue()" } }
 *    { "JavaScript" : { "rsquared" : "r*r" } }
 *    { "js" : { "rsquared" : "r*r" } }
 *    ]
 * </pre>
 *
 * @author David.Biesack@sas.com
 */
@UnRAVLExtractorPlugin({ "javascript", "JavaScript", "js" })
public class JavaScriptExtractor extends BaseScriptExtractor {
    public JavaScriptExtractor() {
        super("javascript");
    }
}
