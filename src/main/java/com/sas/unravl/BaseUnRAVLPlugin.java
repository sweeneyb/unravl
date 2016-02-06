// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A base implementation class for {@link UnRAVLPlugin}
 * 
 * @author DavidBiesack@sas.com
 */
public abstract class BaseUnRAVLPlugin implements UnRAVLPlugin {

    /**
     * Extract a boolean value from the current scriptlet. For example, the
     * "basic" auth element
     * 
     * <pre>
     * { "auth" : { "basic" : true } }
     * </pre>
     * 
     * can use this to get the boolean value named "basic"
     * 
     * @param scriptlet
     *            the element inside an UnRAVL script
     * @param optionName
     *            the name of the option
     * @return true if th eoption was found and has the value true; otherwise
     *         false
     * @throws UnRAVLException
     *             if the value is not a boolean
     */
    protected static boolean booleanOption(ObjectNode scriptlet,
            String optionName) throws UnRAVLException {
        JsonNode val = scriptlet.get(optionName);
        if (val == null)
            return false;
        if (val.isBoolean())
            return val.booleanValue();
        String msg = String.format(
                "%s option in %s must be a Boolean value; found %s",
                optionName, key(scriptlet), val);
        throw new UnRAVLException(msg);
    }

    /**
     * Extract a text value from the current scriptlet. For example, the "oath2"
     * auth element
     * 
     * <pre>
     * { "auth" : { "oath2" : "https://www.example.com/auth/token", "query" : "access_token" } }
     * </pre>
     * 
     * can use this to get the boolean value named "access_token"
     * 
     * @param scriptlet
     *            the element inside an UnRAVL script
     * @param optionName
     *            the name of the option
     * @param defaultValue
     *            value to return if the option is not present
     * @return the string associated with the options name if found, else the
     *         default value
     * @throws UnRAVLException
     *             if the value is not a text node
     */
    protected String stringOption(ObjectNode scriptlet, String optionName,
            String defaultValue) throws UnRAVLException {
        JsonNode val = scriptlet.get(optionName);
        if (val == null)
            return defaultValue;
        if (val.isTextual())
            return val.textValue();
        String msg = String.format(
                "%s option in %s must be a text value; found %s", optionName,
                key(scriptlet), val);
        throw new UnRAVLException(msg);
    }

    /**
     * Returns the name of this extractor. THis is the name (key) of the first
     * value in the object.
     * 
     * @param extractor
     *            the JSON object containing an UnRAVL extractor
     * @return the key (name) of the first value
     */
    public static String key(ObjectNode extractor) {
        return extractor.fields().next().getKey();
    }

    private UnRAVL script;
    private ObjectNode scriptlet;
    private ApiCall call;

    /**
     * @return the API call that this plugin is processing
     */
    public ApiCall getCall() {
        return call;
    }

    /**
     * Set the runtime API call instance
     * 
     * @param call
     *            the current API call instance
     */
    public void setCall(ApiCall call) {
        this.call = call;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sas.unravl.UnRAVLPlugin#getScriptlet()
     */
    @Override
    public ObjectNode getScriptlet() {
        return scriptlet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sas.unravl.UnRAVLPlugin#setScriptlet(org.codehaus.jackson.node.ObjectNode
     * )
     */
    @Override
    public void setScriptlet(ObjectNode scriptlet) {
        this.scriptlet = scriptlet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sas.unravl.UnRAVLPlugin#setScript(com.sas.unravl.UnRAVL)
     */
    @Override
    public void setScript(UnRAVL script) {
        this.script = script;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sas.unravl.UnRAVLPlugin#script()
     */
    @Override
    public UnRAVL getScript() {
        return script;
    }
}
