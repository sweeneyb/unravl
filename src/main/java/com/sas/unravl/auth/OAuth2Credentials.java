// Copyright (c) 2016, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.auth;

/**
 * A holder class for credentials for OAuth2 authentication, using a user id,
 * password, clientId, clientSecret, and accessToken in addition to user and
 * password managed by the base {@link HostCredentials} class. For security, one
 * should not retain this object for a long period of time; use the clear()
 * method so the password is not retained.
 * 
 * @author David.Biesack@sas.com
 */
public class OAuth2Credentials extends HostCredentials {

    private String clientId, clientSecret, accessToken;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public OAuth2Credentials(String userName, String password, String clientId,
            String clientSecret, String accessToken) {
        super(userName, password);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accessToken = accessToken;
    }

    public void clear() {
        super.clear();
        clientId = null;
        clientSecret = null;
        accessToken = null;
    }

}
