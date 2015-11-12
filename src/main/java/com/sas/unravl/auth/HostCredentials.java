// Copyright (c) 2015, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.auth;

/**
 * Contains credentials for logging into or connecting to a host.
 * For security, one should not retain this object for a long period of time;
 * use the clear() method so the password is not retained.
 * @author David.Biesack@sas.com
 */
public final class HostCredentials {

    private String userName;
    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    private String password;
    
    public HostCredentials(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public void clear() {
        userName = null;
        password = null;
    }

}
