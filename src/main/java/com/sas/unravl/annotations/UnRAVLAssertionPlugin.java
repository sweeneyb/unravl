package com.sas.unravl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for UnRAVLAssertion plugin classes, which will allow
 * UnRAVLRuntime to associate one ore more string keys (the annotation value)
 * with a class within "assert" or "precondition" elements of an UnRAVL script.
 * For example, the class com.sas.unravl.assertions.BoundAssertion uses the
 * annotation
 *
 * <pre>
 * {@literal @}UnRAVLAssertionPlugin("bound")
 * </pre>
 *
 * so that UnRAVL scripts which use the "assert" (or "precondition") scriptlet
 *
 * <pre>
 * "assert" : [
 *            { "bound" : "location" }
 * ]
 * </pre>
 *
 * can execute that assertion by instantiating a BoundAssertion.
 * <p>
 * A single assertion class can register multiple keys, if it wishes to define
 * an alias or if it wants to support different behavior depending on the key.
 * com.sas.unravle.asssertions.GroovyAssertion uses
 *
 * <pre>
 * {@literal @}UnRAVLAssertionPlugin({"groovy","Groovy"})
 * </pre>
 *
 * @author DavidBiesack@sas.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnRAVLAssertionPlugin {
    /**
     * The tags by which this plugin is named in UnRAVL scripts
     * 
     * @return the plugin tags
     */
    String[] value();
}
