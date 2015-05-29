package com.sas.unravl.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLPlugins;
import com.sas.unravl.assertions.UnRAVLAssertion.Stage;

/**
 * An UnRAVL script assertion. Assertion objects run as preconditions before
 * invoking an API call, or as assertions on the results of the API call,
 * checking the status code, headers, response body, or variables, etc. An
 * {@link UnRAVL} script will load UnRAVLAssertion objects while executing the
 * "preconditions" or "assert" members of the script. The first field in the
 * assertion member is used as the key, i.e.
 * 
 * <pre>
 * { "headers" : specification }
 * </pre>
 * 
 * is a headers assertion which uses the key "headers". The assertion class is
 * found in the {@link UnRAVLPlugins} list of assertions, and instantiated.
 * Then, the {@link #check(UnRAVL, ObjectNode, Stage, ApiCall)} method is run,
 * passing the currently executing {@link UnRAVL} script and the JsonNode
 * element that defines the assertion specification (in this case, the value
 * associated with "headers")
 * <p>
 * Assertions should extend {@link BaseUnRAVLAuth} and their check() method
 * should invoke super.check(script,node)
 * 
 * @author David.Biesack@sas.com
 */
public interface UnRAVLAuth {

    /**
     * execute the authentication within the given script
     * 
     * @param script
     *            the currently running script
     * @param auth
     *            the JSON object node which defines this authentication.
     * @param call
     *            the current API call
     * @throws UnRAVLException
     *             if the assertion definition is invalid
     */
    public void authenticate(UnRAVL script, ObjectNode assertion, ApiCall call)
            throws UnRAVLException;

    /** Set the scriptlet that defines this assertion */
    public void setAuth(ObjectNode node);

    /** Get the scriptlet that defines this assertion */
    public ObjectNode getAuth();

    /** Set the UnRAVL script that this instance is running in */
    public void setScript(UnRAVL script);

    /** Set the UnRAVL script that this instance is running in */
    public UnRAVL getScript();

}
