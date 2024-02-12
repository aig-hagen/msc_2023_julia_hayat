package de.julsched.beliefchange.exceptions;

public class EncodingFailureException extends RuntimeException {

    public EncodingFailureException (Exception exception) {
        super("Failed to create encoding", exception);
    }

    public EncodingFailureException(String errorMsg) {
        super("Failed to create encoding: " + errorMsg);
    }
}
