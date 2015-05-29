package com.sas.unravl;

/**
 * An HTTP REST API method.
 * 
 * @author David.Biesack@sas.com
 */
// TODO: If we switch from Apache HTTP components to Spring MVC 4.x,
// we can use the Method defined there (PATCH was added in 4.0;
// it is not in Spring MVC 3.1)
public enum Method {
    GET, HEAD, PUT, POST, DELETE, PATCH, OPTIONS, TRACE;
}
