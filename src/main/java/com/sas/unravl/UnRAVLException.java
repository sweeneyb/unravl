package com.sas.unravl;

/**
 * An exception in an UnRAVL script definition, runtime, or assertion
 * 
 * @author sasdjb
 */
public class UnRAVLException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnRAVLException(Throwable rootCause) {
        super(rootCause);
    }

    public UnRAVLException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnRAVLException(String message) {
        super(message);
    }
}
