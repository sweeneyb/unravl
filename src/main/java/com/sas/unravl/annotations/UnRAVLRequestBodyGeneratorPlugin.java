package com.sas.unravl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for UnRAVLBodygenerator plugin classes, which will allow
 * UnRAVLRuntime to associate a string (the annotation value) with this class
 * within "body" element. For example, the class
 * com.sas.unravl.generators.JsonRequestBodyGenerator uses the annotation
 *
 * <pre>
 * {@literal @}UnRAVLRequestBodyGeneratorPlugin("json")
 * </pre>
 *
 * so that UnRAVL scripts which use the "body" scriptlet
 *
 * <pre>
 * "body" : { "json" : { ... } }
 * </pre>
 *
 * can execute that body generator by instantiating a JsonRequestBodyGenerator.
 *
 * @author DavidBiesack@sas.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnRAVLRequestBodyGeneratorPlugin {
    /**
     * The tags by which this plugin is named in UnRAVL scripts
     * 
     * @return the plugin tags
     */
    String[] value();
}
