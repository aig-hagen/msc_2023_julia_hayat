package de.julsched.beliefchange.exceptions;

public class ValidationException extends RuntimeException {

    public ValidationException(String errorMsg) {
        super(errorMsg);
    }

    public ValidationException(String errorMsg, Exception e) {
        super(errorMsg, e);
    }
}
