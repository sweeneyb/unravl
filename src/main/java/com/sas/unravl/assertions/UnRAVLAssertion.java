package com.sas.unravl.assertions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLPlugins;

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
 * Assertions should extend {@link BaseUnRAVLAssertion} and their check() method
 * should invoke super.check(script,node)
 * 
 * @author David.Biesack@sas.com
 */
public interface UnRAVLAssertion {

    public static enum Stage {
        PRECONDITIONS, ASSERT;
        public String getName() {
            return toString().toLowerCase();
        }
    };

    /**
     * execute the assertion within the given script
     * 
     * @param script
     *            the currently running script
     * @param assertion
     *            the JSON object node which defines this assertion.
     * @param stage
     *            Indicates which stage is running, "preconditions" or "assert"
     * @param call
     *            The currently running API call
     * @throws UnRAVLAssertionException
     *             if the assertion fails
     * @throws UnRAVLException
     *             if the assertion definition is invalid
     */
    public void check(UnRAVL script, ObjectNode assertion, Stage stage,
            ApiCall call) throws UnRAVLAssertionException, UnRAVLException;

    /**
     * Return the UnRAVLAssertionException, if running this assertion had an
     * exception
     */
    public UnRAVLAssertionException getUnRAVLAssertionException();

    /**
     * Set the UnRAVLAssertionException, if running this assertion had an
     * exception
     */
    public void setUnRAVLAssertionException(UnRAVLAssertionException e);

    /** Set the scriptlet that defines this assertion */
    public void setAssertion(ObjectNode node);

    /** Get the scriptlet that defines this assertion */
    public ObjectNode getAssertion();

    /** Set the UnRAVL script that this instance is running in */
    public void setScript(UnRAVL script);

    /** Set the UnRAVL script that this instance is running in */
    public UnRAVL getScript();

    /**
     * Set the UnRAVL stage (Precondition, assert) that this instance is running
     * in
     */
    public void setStage(Stage stage);

    /**
     * Get the UnRAVL stage (Precondition, assert) that this instance is running
     * in
     */
    public Stage getStage();
}
