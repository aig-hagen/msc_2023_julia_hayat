package de.julsched.beliefchange.exceptions;

public class MinimalSetConstraintsDeterminationException extends RuntimeException {

    public MinimalSetConstraintsDeterminationException(Exception exception) {
        super("Failed to determine minimal distance set constraints", exception);
    }

    public MinimalSetConstraintsDeterminationException(String errorMsg) {
        super("Failed to determine minimal distance set constraints: " + errorMsg);
    }
}
