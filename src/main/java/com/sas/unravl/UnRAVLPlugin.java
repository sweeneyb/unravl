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

    /** Get the scriptlet which defines/configures this instance */
    public abstract ObjectNode getScriptlet();

    /** Set the scriptlet which defines/configures this instance */
    public abstract void setScriptlet(ObjectNode scriptlet);

    /** Set the UnRAVL script in which this plugin runs. */
    public abstract void setScript(UnRAVL script);

    /** Get the UnRAVL script in which this plugin runs. */
    public abstract UnRAVL getScript();

}
