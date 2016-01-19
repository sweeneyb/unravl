// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.annotations.UnRAVLAuthPlugin;
import com.sas.unravl.assertions.UnRAVLAssertionException;
import com.sas.unravl.util.Json;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

/**
 * An auth plugin which authenticates with OAuth2. This using the user's
 * credentials provided by the {@link CredentialsProvider} which by default is
 * the {@link NetrcCredentialsProvider}.
 * <p>
 * This auth element is specified via
 *
 * <pre>
 * "auth" : { "oauth2" : <em>oauth-server-URL</em> }
 * "auth" : { "oauth2" : <em>oauth-server-URL</em>, <em>options</em> }
 * </pre>
 * 
 *
 * where <em>auth-token-URL</em> is a string containing the URL of the
 * authorization server token API, such as
 * 
 * <pre>
 * { "oauth2" : "http://www.example.com/auth/token" }
 * </pre>
 * 
 * <h2>Options</h2>
 * The "oath2" object supports several options to configure the OAUth2
 * authentication. See
 * <a href='https://github.com/sassoftware/unravl/blob/master/doc/Authentication.md#oath2'>Authentication: oauth2</a>
 * for complete details.
 * <p>
 * This auth module may be accessed with either the name <code>"oauth2"</code>, <code>"OAuth2"</code>, 
 * <code>"oauth"</code>, <code>"OAuth"</code>.
 *
 * @author DavidBiesack@sas.com
 */
@UnRAVLAuthPlugin({ "oauth", "oauth2", "OAuth", "OAuth2" })
public class OAuth2Auth extends BaseUnRAVLAuth {

    private static final String DEFAULT_ACCESS_TOKEN_JSON_PATH = "$.access_token";
    private static final String DEFAULT_OATH_SCRIPT_RESOURCE = "/com/sas/unravl/auth/get-oauth-token.unravl";
    private static final String OAUTH_SCRIPT_KEY = "OAauth2Script";
    private String tokenUrl;
    private static final Logger logger = Logger.getLogger(OAuth2Auth.class);
    
    private static String ACCESS_TOKEN_JSON_PATH_KEY = "accessTokenJsonPath";
    
    @Override
    public void authenticate(UnRAVL script, ObjectNode oauthSpec, ApiCall call)
            throws UnRAVLException {
        super.authenticate(script, oauthSpec, call);
        authenticateAndAddAuthenticationHeader(oauthSpec);
    }

    void authenticateAndAddAuthenticationHeader(ObjectNode auth)
            throws UnRAVLException {
        try {
            if (getScript().getURI() == null || getScript().getMethod() == null)
                throw new UnRAVLException(
                        "oauth2 auth requires an HTTP method and URI");
            // Note: The URI should already be expanded at this point
            long start = System.currentTimeMillis();
            JsonNode tokenNode = Json.firstFieldValue(getScriptlet());
            if (tokenNode == null || !(tokenNode instanceof TextNode))
                throw new UnRAVLException(
                        "oauth2 auth requires an oauth-server-URL.");
            tokenUrl = getScript().expand(tokenNode.textValue());
            String access_token = getAccessToken(new URI(tokenUrl), auth);
            bindAccessTokenInEnv(auth, access_token);
            addOptionalQueryParameter(auth, access_token);
            long end = System.currentTimeMillis();
            logger.trace("oauth2 authentication took " + (end - start) + "ms");

        } catch (URISyntaxException e) {
            new UnRAVLException(e.getMessage(), e);
        } catch (IOException e) {
            new UnRAVLException(e.getMessage(), e);
        }

    }

    // bind "access_token" in the environment, or if the oath2 object
    // contains a "bindAccessToken" : "varName" option, bind to that var name instead.
    // for example for 
    // "bindAccessToken" : "bitlyAccessToken"
    // the oauth2 object will bind the variable 
    // "bitlyAccessToken" in the environment instead of "access_token"
    private void bindAccessTokenInEnv(ObjectNode auth, String access_token)
            throws UnRAVLException {
        // we normally store the token via access_token but the caller
        // can override this with their own variable name to bind
        String callerKey = stringOption(auth, "bindAccessToken", "access_token");
        getScript().bind(callerKey, access_token);
        getScript().addRequestHeader(
                new BasicHeader("Authorization", "Bearer " + access_token));
        logger.info("\"oauth2\" auth added 'Authorization: Bearer "
                + access_token + "' header");
    }

    // If the oauth2 object options includes a "parameter" : "paramName",
    // add a query parameter to the URI using the parameter name
    private void addOptionalQueryParameter(ObjectNode auth, String access_token)
            throws UnRAVLException {
        // if client specifies a query parameter, we will add it to the request URI
        String queryParm = stringOption(auth, "parameter", null);
        if (queryParm != null) {
            StringBuilder uri = new StringBuilder(getCall().getURI());
            String delim = (uri.indexOf("?") == -1) ? "?" : "&";
            uri.append(delim)
                    .append(queryParm)
                    .append("=")
                    .append(access_token);
            getCall().setURI(uri.toString());
            logger.info("\"oauth2\" auth added '" + delim + queryParm + "="
                    + access_token + "' query parameter");
        }
    }

    /**
     * Cache the access_token for this URI's host in the environment, so
     * subsequent oauth objects do not have to reauthenticate. The access token
     * is stored in the environment as "{userId}.{hostname}.access_token".
     *
     * @param userId
     *            the user id
     * @param hostname
     *            the hostname of auth server URI
     * @param accessToken
     *            the access_token string
     * @retun the <var>accessToken</var>
     */
    private String cacheAccessToken(String userId, String hostname,
            String accessToken) {
        getScript().bind(accessTokenCacheKey(userId, hostname), accessToken);
        return accessToken;
    }

    private String accessTokenCacheKey(String userId, String hostname) {
        return userId + "." + hostname + "." + "access_token";
    }

    /**
     * Get the access token. If it is part of the credentials for the OAuth2
     * host, return that static access token.
     * <p>
     * If a cached token exists, return it.
     * </p>
     * <p>
     * Otherwise, use UnRAVL to run an REST API POST call to the authentication
     * server to get an OAAth access token. The default call uses Basic
     * Authentication using with the clientId and clientSecret to authenticate.
     * The default request body is a <code>application/x-www-form-urlencoded</code> form
     * </p>
     * 
     * <pre>
     *  { "grant_type" : "password",
          "username" : "{userId}",
          "password" : "{password}" }
     * </pre>
     * 
     * <p>
     * where <code>userId</code> and <code>password</code> are
     * the user id and password associated with the OAth authentication server hostname.
     * The client ID, client password, user ID and password are read from the
     * {@link CredentialsProvider}, normally the
     * {@link NetrcCredentialsProvider}. See that class for the format of the
     * <code>.netrc</code> file.
     * <p>
     * The UnRAVL script is found via the classpath at
     * <code>/com/sas/unravl/auth/get-oauth-token.unravl</code> (the default
     * resource is in the UnRAVL jar) but this resource path can be changed by
     * the text option <code>"OAauth2Script" : "/paths/to/unravl/script"</code>
     * in the <code>"oath2"</code> element. The UnRAVL script should invoke the POST to the authentication server,
     * then extract and bind the variable <code>access_token</code> from the
     * response.
     * </p>
     * 
     * @param authTokenURI
     *            URI of the authorization token server
     * @param creds
     *            credentials associated with the token server
     * @return the <code>access_token</code> for the client/user
     * @throws UnRAVLException
     *             if the script execution encounters an error
     */
    private String getAccessToken(URI authTokenURI, ObjectNode auth)
            throws UnRAVLException, URISyntaxException,
            ClientProtocolException, IOException {

        String host = authTokenURI.getHost();
        String access_token = null;
        UnRAVLRuntime runtime = getScript().getRuntime();

        CredentialsProvider cp = runtime.getPlugins().getCredentialsProvider();

        cp.setRuntime(runtime);
        HostCredentials credentials = cp.getHostCredentials(host, auth, false);
        if (credentials == null)
            throw new UnRAVLAssertionException("No auth credentials for host "
                    + host);
        if (!(credentials instanceof OAuth2Credentials))
            throw new UnRAVLAssertionException(
                    "Authentication credentials for host lack clientid and secret" + host);
        OAuth2Credentials creds = (OAuth2Credentials) credentials;
        String user = creds.getUserName();
        if (creds.getAccessToken() != null) {
            access_token = creds.getAccessToken();
            return cacheAccessToken(user, host, access_token);
        }

        String key = accessTokenCacheKey(user, host);
        if (runtime.bound(key)) {
            access_token = (String) getScript().getRuntime().binding(key);
            logger.info(String.format(
                    "Found cached OAuth2 access_token for user %s and host %s",
                    user, host));
            return cacheAccessToken(user, host, access_token);
        }

        // See if the access token is cached for this user/host
        if (runtime.bound(key)) {
            access_token = (String) getCall().getVariable(key);
            logger.info("Using cached oauth2 access token: " + access_token);
            return cacheAccessToken(user, host, access_token);
        }

        // Else, use UnRAVL to send a request to the server to generate an
        // access_token

        String oAuthScriptResourcePath = stringOption(getScriptlet(),
                OAUTH_SCRIPT_KEY, DEFAULT_OATH_SCRIPT_RESOURCE);
        String accessTokenJsonPath = stringOption(getScriptlet(),
                ACCESS_TOKEN_JSON_PATH_KEY, DEFAULT_ACCESS_TOKEN_JSON_PATH);

        // We need a new runtime because we don't want this script to
        // be recorded in the calling runtime's history of scripts, or these
        // values to affect the calling runtime.

        UnRAVLRuntime tokenRuntime = new UnRAVLRuntime(getScript().getRuntime());
        tokenRuntime.bind(ACCESS_TOKEN_JSON_PATH_KEY, accessTokenJsonPath);

        // TODO: convert  the UnRAVL to use { "body" : { "form" : { name : value, ... }}}
        // so we don't have to deal with encodedUserId etc.
        tokenRuntime // Hmmmm, is it worth defining constants for these keys?
                .bind("oath2TokenUrl", authTokenURI.toString())
                .bind("clientId", creds.getClientId())
                .bind("clientSecret", creds.getClientSecret())
                .bind("userId", creds.getUserName())
                .bind("password", creds.getPassword())
                .bind(ACCESS_TOKEN_JSON_PATH_KEY, accessTokenJsonPath  );
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream(
                oAuthScriptResourcePath)) {
            ObjectNode accessAuthJson = (ObjectNode) mapper.readTree(in);
            UnRAVL oathAccessTokenScript = new UnRAVL(tokenRuntime,
                    accessAuthJson);
            oathAccessTokenScript.run();
            access_token = (String) tokenRuntime.binding("access_token");
        } catch (IOException e) {
            throw new UnRAVLException(e.getMessage(), e);
        }
        return cacheAccessToken(user, host, access_token);
    }

}
