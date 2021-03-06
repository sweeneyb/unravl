// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLAuthPlugin;
import com.sas.unravl.assertions.UnRAVLAssertionException;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

/**
 * An auth element which provides basic authentication. This authenticates by
 * using the user's credentials stored in their ~/.netrc or %USERPROFILE%\_netrc
 * file (Windows) or in "login" and "password" elements of the script.
 * <p>
 * This precondition is specified via
 * 
 * <pre>
 * "auth" : { "basic" : true }
 * "auth" : { "basic" : true, "login" : "myuserid" }
 * "auth" : { "basic" : true, "login" : "userid", "password" : "mySecret" }
 * "auth" : { "basic" : true, "mock" : "false" }
 * </pre>
 * <p>
 * This adds an <code>Authentication:Basic <em>encoded-credentials</em></code>
 * header to the request. The <code><em>encoded-credentials</em></code> is the
 * Base64 encoding of the username:password. The encoded credentials are masked
 * when logging the request headers.
 * </p>
 * <p>
 * If mock is true, present mock authentication credentials
 * <p>
 * TODO: allow an alternate location so credentials can be shared across
 * hosts/domains instead of having to have a .netrc file for each host
 * </p>
 * 
 * <pre>
 * "auth" : { "basic" : "hostname" }
 * </pre>
 * 
 * @author DavidBiesack@sas.com
 */
@UnRAVLAuthPlugin("basic")
public class BasicAuth extends BaseUnRAVLAuth {

    private static final Logger logger = Logger.getLogger(BasicAuth.class);
    private boolean mock; // JSON spec contains "mock" : true, then mock out the
                          // responses

    @Override
    public void authenticate(UnRAVL script, ObjectNode scriptlet, ApiCall call)
            throws UnRAVLAssertionException, UnRAVLException {
        super.authenticate(script, scriptlet, call);
        authenticateAndAddAuthenticationHeader(scriptlet);
    }

    void authenticateAndAddAuthenticationHeader(ObjectNode auth)
            throws UnRAVLException {
        try {
            // TODO: generalize this check and push it into BaseUnRAVLAuth
            // It should also use a getEffectiveURI() and getEffectiveMethod()
            // which walk templates in case the script does not have them.
            if (getScript().getURI() == null || getScript().getMethod() == null)
                throw new UnRAVLException(
                        "basic auth requires an HTTP method and URI");
            // TODO: allow the value to be a string; use that host name or
            // location for credential lookup
            JsonNode authVal = Json.firstFieldValue(getScriptlet());
            if (!authVal.isBoolean()) {
                throw new UnRAVLException(
                        "Value of \"auth\" must be boolean. Found: " + authVal);
            }
            if (!authVal.booleanValue())
                return;

            if (getScriptlet().get("mock") != null)
                mock = getScriptlet().get("mock").booleanValue();
            // Note: the URI should already be expanded at this point
            String location = getCall().getURI();
            URI uri = new URI(location);

            basicAuth(uri, auth);

        } catch (URISyntaxException e) {
            new UnRAVLException(e.getMessage(), e);
        } catch (IOException e) {
            new UnRAVLException(e.getMessage(), e);
        }
    }

    private void basicAuth(URI uri, ObjectNode auth) throws UnRAVLException,
            IOException {
        String host = uri.getHost();
        CredentialsProvider cp = getScript().getRuntime().getPlugins()
                .getCredentialsProvider();
        cp.setRuntime(getScript().getRuntime());
        HostCredentials credentials = cp.getHostCredentials(host, auth, mock);

        if (credentials == null)
            throw new UnRAVLException("No Basic Auth credentials for host "
                    + host);

        String creds = new Base64().encodeToString(Text.utf8(credentials
                .getUserName() + ":" + credentials.getPassword()));
        credentials.clear();
        // TODO: use the ApiCall and add headers there instead of mutating the
        // script
        getScript().addRequestHeader(
                new BasicHeader("Authorization", "Basic " + creds));
        logger.info("\"basic\" auth added 'Authorization: Basic ********' header");
    }

}
