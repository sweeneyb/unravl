package com.sas.unravl.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLPlugins;
import com.sas.unravl.assertions.UnRAVLAssertion.Stage;

/**
 * An UnRAVL script authentication. Authentication objects run before
 * invoking an API call and may decorate the call. An
 * {@link UnRAVL} script will load UnRAVLAuth objects while executing the
 * "auth" members of the script. The first field in the
 * assertion member is used as the key, i.e.
 *
 * <pre>
 * { "basic" : specification }
 * </pre>
 *
 * is a headers assertion which uses the key "basic". The corresponding class is
 * found in the {@link UnRAVLPlugins} list of authentications, and instantiated.
 * Then, the {@link #authenticate(UnRAVL, ObjectNode, ApiCall)} method is run,
 * passing the currently executing {@link UnRAVL} script and the JsonNode
 * element that defines the auth specification (in this case, the value
 * associated with "auth")
 * <p>
 * Authentication plugins should extend {@link BaseUnRAVLAuth} and their autheenticate() method
 * should invoke super.check(script,node)
 *
 * @author David.Biesack@sas.com
 */
public interface UnRAVLAuth {

    /**
     * Execute the authentication within the given script
     *
     * @param script
     *            the currently running script
     * @param assertion
     *            the JSON object node which defines this authentication.
     * @param call
     *            the current API call
     * @throws UnRAVLException
     *             if the assertion definition is invalid
     */
    public void authenticate(UnRAVL script, ObjectNode assertion, ApiCall call)
            throws UnRAVLException;

    /**
     * Set the scriptlet that defines this assertion
     * @param node the UnRAVL scriptlet node
     */
    public void setAuth(ObjectNode node);

    /**
     * Get the scriptlet that defines this assertion
     * @return the scriptlet node
     */
    public ObjectNode getAuth();

    /**
     * Set the UnRAVL script that this instance is running in
     * @param script the current UnRAVL script
     */
    public void setScript(UnRAVL script);

    /**
     * Get the UnRAVL script in which this plugin runs.
     * @return the current script this plugin is processing
     */
    public UnRAVL getScript();

}
