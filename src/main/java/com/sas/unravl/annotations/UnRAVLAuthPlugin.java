package com.sas.unravl.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for UnRAVLAuthentication plugin classes, which will allow
 * UnRAVLRuntime to associate one ore more string keys (the annotation value)
 * with this class within "auth" elements of an UnRAVL script. For example, the
 * class com.sas.unravl.auth.BasicAuth uses the annotation
 *
 * <pre>
 * {@literal @}UnRAVLAssertionPlugin("basic")
 * </pre>
 *
 * so that UnRAVL scripts which use the "auth" scriptlet
 *
 * <pre>
 * "auth" : { "basic" : true }
 * </pre>
 *
 * will use Basic Authentication based on the credentials for the location
 * <p>
 * A single auth class can register multiple keys, if it wishes to define an
 * alias or if it wants to support different behavior depending on the key.
 *
 * @author DavidBiesack@sas.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnRAVLAuthPlugin {
    /**
     * The tags by which this plugin is named in UnRAVL scripts
     * 
     * @return the plugin tags
     */
    String[] value();
}
