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
import com.sas.unravl.generators.Binary;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

/**
 * An auth plugin which verifies that the API call is authenticated with OAuth2.
 * <p>
 * This auth element is specified via
 *
 * <pre>
 * "auth" : { "oauth2" : <em>auth-token-URL</em> }
 * "auth" : { "oauth2" : <em>auth-token-URL</em>, "login" : "myUserId" }
 * "auth" : { "oauth2" : <em>auth-token-URL</em>, "login" : "myUserId", "password" : "mySecret" }
 * 
 * "auth" : { "oauth2" : <em>auth-token-URL</em>, "login" : "myUserId", "password" : "mySecret",
 *            "clientId" : "unique client id", "secret" : "unique client secret" }
 * "auth" : { "oauth2" : <em>auth-token-URL</em>, "mock" : <em>boolean-value</em>
 * </pre>
 *
 * where <em>auth-token-URL</em> is a string containing the URL of the
 * authorization server token API, such as
 * 
 * <pre>
 * { "oauth2" : "http://www.example.com/auth/token" }
 * </pre>
 * <p>
 * This auth module may be accessed with either the name <code>"oauth2"</code>
 * or <code>"oauth"</code>.
 *
 * @author DavidBiesack@sas.com
 */
@UnRAVLAuthPlugin({ "oauth", "oauth2" })
public class OAuth2Auth extends BaseUnRAVLAuth {

    private String tokenUrl;
    private boolean mock; // JSON spec contains "mock" : true, then mock out the
                          // OAoth responses and create a fake access token
                          // and Authorization bearer header
    private static final Logger logger = Logger.getLogger(OAuth2Auth.class);

    @Override
    public void authenticate(UnRAVL script, ObjectNode casAuthSpec, ApiCall call)
            throws UnRAVLException {
        super.authenticate(script, casAuthSpec, call);
        authenticateAndAddAuthenticationHeader(casAuthSpec);
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
                        "oauth2 auth requires a auth-token-URL .");
            if (getScriptlet().get("mock") != null)
                mock = getScriptlet().get("mock").booleanValue();
            tokenUrl = getScript().expand(tokenNode.textValue());
            String access_token = logon(new URI(tokenUrl), auth);
            String atkey = stringOption(auth, "bindAccessToken", "access_token");
            getScript().bind(atkey, access_token);
            getScript().addRequestHeader(
                    new BasicHeader("Authorization", "Bearer " + access_token));
            logger.info("\"oauth2\" auth added 'Authorization: Bearer "
                    + access_token + "' header");
            getScript().bind("oauth2.access.token", access_token);
            long end = System.currentTimeMillis();
            logger.trace("oauth2 authentication took " + (end - start) + "ms");

        } catch (URISyntaxException e) {
            new UnRAVLException(e.getMessage(), e);
        } catch (IOException e) {
            new UnRAVLException(e.getMessage(), e);
        }

    }

    /**
     * Save the access_token for this URI's host in the environment, so
     * subsequent oauth objects do not have to reauthenticate. The most recently
     * acquired access token is also stored in the environment as
     * "{userId}.{hostname}.access_token", and also to "oauth2.access.token"
     *
     * @param accessToken
     *            the access_token string
     * @param host
     *            the auth server URI
     * @param userId
     *            the user id
     */
    private void bindAccessToken(String accessToken, URI host, String userId) {
        String hostname = host.getHost();
        getScript().bind(accessTokenVarName(userId, hostname), accessToken);
    }

    private String accessTokenVarName(String userId, String hostname) {
        return userId + "." + hostname + "." + "access_token";
    }

    private String logon(URI authTokenURI, ObjectNode auth)
            throws UnRAVLException, URISyntaxException,
            ClientProtocolException, IOException {

        String user = null;
        String host = authTokenURI.getHost();
        String access_token = null;
        UnRAVLRuntime runtime = getScript().getRuntime();
        String key = accessTokenVarName(user, host);
        if (runtime.bound(key)) {
            access_token = (String) getScript().getRuntime().binding(key);
            logger.info(String.format(
                    "Found cached OAuth2 access_token for user %s and host %s",
                    user, host));
        }
        if (mock) {
            access_token = "unrnavl.mock.acccess_token.677";
            user = "mockUserId";
        } else {
            CredentialsProvider cp = runtime.getPlugins()
                    .getCredentialsProvider();
            cp.setRuntime(runtime);
            HostCredentials credentials = cp.getHostCredentials(host, auth,
                    false);
            if (credentials == null)
                throw new UnRAVLAssertionException(
                        "No auth credentials for host " + host);
            if (!(credentials instanceof OAuth2Credentials))
                throw new UnRAVLAssertionException(
                        "auth credentials for host lack clientid and secret"
                                + host);
            OAuth2Credentials creds = (OAuth2Credentials) credentials;
            // See if the access token is cached for this user/host
            if (runtime.bound(key)) {
                Object at = getCall().getVariable(key);
                if (at instanceof String) {
                    String ats = (String) at;
                    logger.info("Using cached oauth2 access token: " + at);
                    return ats;
                }
            }
            access_token = getToken(authTokenURI, creds);
        }
        bindAccessToken(user, authTokenURI, access_token);
        return access_token;
    }

    /**
     * Use UnRAVL to  run an REST API POST call to the authentication server to get an OAAth access token.
     * The default call uses Basic Authentication using with the clientId and clientSecret to authenticate.
     * The default request body is 
     * <pre>
     * grant_type=password&username={encodedUserId}&password={encodedPassword}
     * </pre>
     * where <code>encodedUserId</code> and <code>encodedPassword</code>
     * are form encoded values of the user id and password for the OAth auth server.
     * The client ID, client password, user ID and password are read from the {@link CredentialsProvider},
     * normally the {@link NetrcCredentialsProvider}. See that class for the format of the .netrc file.
     * <p>
     * The UnRAVL script is found via the classpath at <code>/com/sas/unravl/auth/get-oauth-token.unravl</code>
     * (the default resource is in the UnRAVL jar)
     * but this resource path can be changed by the text option <code>"getOauthScript" : "/paths/to/unravl/script"</code>.
     * The UnRAVL script should invoke the POST to the authentication server, then extract
     * and bind the variable <code>access_token</code> from the response.
     * </p>
     * @param authTokenURI URI of the authorization token server
     * @param creds credentials associated with the token server
     * @return the <code>access_token</code> for the client/user
     * @throws UnRAVLException if the script execution encounters an error
     */
    private String getToken(URI authTokenURI, OAuth2Credentials creds) throws UnRAVLException {
        String oAuthScriptResourcePath = stringOption(getScriptlet(), "getOauthScript", "/com/sas/unravl/auth/get-oauth-token.unravl");
        String accessTokenJsonPathKey = "accessTokenJsonPath";
        String accessTokenJsonPath = stringOption(getScriptlet(), accessTokenJsonPathKey, "$.access_token" );
        String accessTokenRequestBodyKey = "accessTokenRequestBody";
        String accessTokenRequestBody = stringOption(getScriptlet(), accessTokenRequestBodyKey, 
                "grant_type=password&username={encodedUserId}&password={encodedPassword}");

        // We need a new runtime because we don't want this script to
        // be recorded in the calling runtime's history of scripts, or these values to
        // affect the calling runtime.
        
        UnRAVLRuntime tokenRuntime = new UnRAVLRuntime(getScript().getRuntime());
        tokenRuntime.bind(accessTokenJsonPathKey, accessTokenJsonPath);

        String encodedUserId = urlEncode(creds.getUserName());
        String encodedPassword = urlEncode(creds.getPassword());
        tokenRuntime.bind("oath2TokenUrl", authTokenURI.toString());
        tokenRuntime.bind("clientId", creds.getClientId());
        tokenRuntime.bind("clientSecret", creds.getClientSecret());
        tokenRuntime.bind("userId", creds.getUserName());
        tokenRuntime.bind("password", creds.getPassword());
        tokenRuntime.bind("encodedUserId", encodedUserId);
        tokenRuntime.bind("encodedPassword", encodedPassword);
        tokenRuntime.bind(accessTokenRequestBodyKey, tokenRuntime.expand(accessTokenRequestBody));
        String access_token = null;
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream(oAuthScriptResourcePath)) {
            ObjectNode accessAuthJson = (ObjectNode) mapper.readTree(in);
            UnRAVL oathAccessTokenScript = new UnRAVL(tokenRuntime, accessAuthJson);
            oathAccessTokenScript.run();
            access_token = (String) tokenRuntime.binding("access_token");
        } catch (IOException e) {
            throw new UnRAVLException(e.getMessage(), e);
        }
        return access_token;
    }

    private static String urlEncode(String string) throws UnRAVLException {
        try {
            return java.net.URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnRAVLException(e.getMessage(), e);
        }
    }
}
