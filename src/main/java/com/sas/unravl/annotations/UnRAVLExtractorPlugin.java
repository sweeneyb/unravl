package com.sas.unravl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for UnRAVLExtractor plugin classes, which will allow
 * UnRAVLRuntime to associate a string (the annotation value) with this class
 * within "extractor" element. For example, the class
 * com.sas.unravl.extractors.PatternExtractor uses the annotation
 * 
 * <pre>
 * &#x40;UnRAVLExtractorPlugin("pattern")
 * </pre>
 * 
 * so that UnRAVL scripts which use the extractor scriptlet
 * 
 * <pre>
 * "bind" [
 *        { "pattern" : [ "{location}", pattern, .... }
 * ]
 * </pre>
 * 
 * can execute that extractor by instantiating a PatternExtractor.
 * 
 * @author DavidBiesack@sas.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnRAVLExtractorPlugin {
    /** The tag by which this plug in is named in UnRAVL scripts */
    String[] value();
}
