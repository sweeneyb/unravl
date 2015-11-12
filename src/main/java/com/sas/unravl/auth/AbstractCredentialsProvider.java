// Copyright (c) 2015, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.util.Json;

import java.io.IOException;

/**
 * Abstract implementation of the CredentialsProvider interfcae.
 * @author David.Biesack@sas.com
 */
abstract public class AbstractCredentialsProvider implements
        CredentialsProvider {

    protected UnRAVLRuntime runtime;

    public AbstractCredentialsProvider() {
        super();
    }

    public void setRuntime(UnRAVLRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Get credentials for the host. Note: If reading credentials from a .netrc
     * file, the credentials are <em>not</em> cached; we reread the .netrc file
     * each time. (This allows one script to obtain credentials from a service
     * and store them in .netrc in the current directory.)
     * 
     * @param host
     *            the host name
     * @param auth
     *            The JsonNode containing the auth contents (usually login and
     *            password)
     * @param mock
     *            if true, return mock credentials
     * @return an object containing the username and password
     * @throws IOException
     *             if we could not read the .netrc file
     */
    public HostCredentials getHostCredentials(String host, ObjectNode auth,
            boolean mock) throws IOException {
        if (mock)
            return mockCredentials();
        String userName = Json.stringFieldOr(auth, "login", null);
        String password = Json.stringFieldOr(auth, "password", null);
        return getHostCredentials(host, userName, password, mock);
    }

    protected HostCredentials mockCredentials() {
        return credentials("mockuser", "mockpassword");
    }

    protected HostCredentials credentials(String login, String password) {
        return new HostCredentials ( runtime.expand(login), runtime.expand(password) );
    }

}