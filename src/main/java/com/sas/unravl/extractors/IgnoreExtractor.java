// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.extractors;

import com.sas.unravl.annotations.UnRAVLExtractorPlugin;

/**
 * Use this to comment out an extractor in a "bind" element, or to add
 * documentation to the "bind" element. For example, to cause the json extractor
 * to be ignore (not create out.json), change
 * 
 * <pre>
 * "bind" : [
 *            { "json" : "@out.json" }
 *          ]
 * </pre>
 * 
 * to
 * 
 * <pre>
 * "bind" : [
 *            { "ignore" : { "json" : "@out.json" } }
 *          ]
 * </pre>
 * 
 * or, to add a comment:
 * 
 * <pre>
 * "bind" : [
 *            { "doc" : "write the response body as JSON to out.json" },
 *            { "json" : "@out.json" }
 *          ]
 * </pre>
 * 
 * @author DavidBiesack@sas.com
 */
@UnRAVLExtractorPlugin({ "ignore", "doc" })
public class IgnoreExtractor extends BaseUnRAVLExtractor {

}
