package com.sas.unravl.assertions;

import com.sas.unravl.UnRAVLException;

/**
 * An assertion thrown by an {@link UnRAVLAssertion} - that is, an
 * element of an <code>"assert"</code> element failed.
 * @author David.Biesack@sas.com
 */

public class UnRAVLAssertionException extends UnRAVLException {

    private static final long serialVersionUID = 1L;

    public UnRAVLAssertionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnRAVLAssertionException(String message) {
        super(message);
    }
}
