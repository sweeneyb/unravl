// Copyright (c) 2015, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.UnRAVLRuntime;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * An interface for an object that can provide user credentials
 * (login id, password) for connecting to a host/port
 * @author David.Biesack@sas.com
 */
public interface CredentialsPortProvider extends CredentialsProvider {

    /**
     * Get credentials for the host. Note: If reading credentials from a .netrc
     * file, the credentials are <em>not</em> cached; we reread the .netrc file
     * each time. (This allows one script to obtain credentials from a service
     * and store them in .netrc in the current directory.)
     *
     * @param host
     *            the host name
     * @param port
     *            the port for the host; this may be null (which implies the default port, 80)
     * @param auth
     *            The JsonNode containing the auth contents (usually login and
     *            password)
     * @param mock
     *            if true, return mock credentials
     * @return an object containing the user login name and password
     * @throws IOException
     *             if we could not read the .netrc file
     */
    public HostCredentials getHostCredentials(String host, String port, ObjectNode auth,
            boolean mock) throws IOException;

    /**
     * Get credentials for the host, user, and passwprd. Note: If reading credentials from a .netrc
     * file, the credentials are <em>not</em> cached; we reread the .netrc file
     * each time. (This allows one script to obtain credentials from a service
     * and store them in .netrc in the current directory.)
     *
     * @param host
     *            the host name
     * @param port
     *            the port for the host; this may be null (which implies the default port, 80)
     * @param userName the user's login name/id
     * @param password the user's password/secret
     * @param mock
     *            if true, return mock credentials
     * @return an object containing the user login name and password
     * @throws FileNotFoundException
     *             if we could not find a .netrc or _netrc file
     * @throws IOException
     *             if we could not read the .netrc or _netrc file
     */
    public abstract HostCredentials getHostCredentials(String host, String port, String userName,
            String password, boolean mock) throws FileNotFoundException,
            IOException;

}
