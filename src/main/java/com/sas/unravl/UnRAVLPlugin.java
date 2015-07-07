// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A common interface for UnRAVL plugins: assertions, request body generators,
 * and extractors. Each plugin contains the UnRAVL script and the
 * ObjectNodescriptlet that it executes. For example, the UnRAVL assertion to
 * test if a variable is bound in the current environment is expressed as
 *
 * <pre>
 * { "bound" : "varName" }
 * </pre>
 *
 * within an UnRAVL script's "preconditions" or "assert" member.
 *
 * @author DavidBiesack@sas.com
 */
public interface UnRAVLPlugin {

    /**
     * Get the scriptlet that defines this assertion
     * 
     * @return the scriptlet node
     */
    public abstract ObjectNode getScriptlet();

    /**
     * Set the scriptlet that defines this assertion
     * 
     * @param node
     *            the UnRAVL scriptlet node
     */
    public abstract void setScriptlet(ObjectNode node);

    /**
     * Set the UnRAVL script that this instance is running in
     * 
     * @param script
     *            the current UnRAVL script
     */
    public abstract void setScript(UnRAVL script);

    /**
     * Get the UnRAVL script in which this plugin runs.
     * 
     * @return the current script this plugin is processing
     */
    public abstract UnRAVL getScript();

}
